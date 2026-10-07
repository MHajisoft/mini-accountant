package ir.mhajisoft.hesabres

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.first
import dagger.hilt.android.AndroidEntryPoint
import ir.mhajisoft.hesabres.domain.crash.BiometricPromptPolicy
import ir.mhajisoft.hesabres.domain.crash.WriteFailures
import ir.mhajisoft.hesabres.ui.AppRoot
import ir.mhajisoft.hesabres.ui.BrandSplash
import ir.mhajisoft.hesabres.ui.lock.LockScreen
import ir.mhajisoft.hesabres.ui.theme.HesabresTheme
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject lateinit var lockController: AppLockController
    private val promptGate = AtomicBoolean(false)

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(newBase.forceFaLocale())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        val keepSystemSplash = AtomicBoolean(savedInstanceState == null)
        splash.setKeepOnScreenCondition { keepSystemSplash.get() }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val coldStart = savedInstanceState == null
        setContent {
            var showBrandSplash by remember { mutableStateOf(coldStart) }
            HesabresTheme {
                if (showBrandSplash) {
                    BrandSplash()
                    LaunchedEffect(Unit) {
                        keepSystemSplash.set(false)
                        delay(1_250)
                        showBrandSplash = false
                    }
                } else {
                    val locked by lockController.locked.collectAsStateWithLifecycle()
                    if (locked) {
                        BackHandler { finish() }
                        var lockError by remember { mutableStateOf<String?>(null) }
                        LockScreen(
                            message = lockError,
                            onUnlock = {
                                promptUnlock(
                                    onSuccess = { lockController.unlock() },
                                    onCancel = { finish() },
                                    onError = { lockError = WriteFailures.ERR_BIOMETRIC },
                                )
                            },
                        )
                        LaunchedEffect(Unit) {
                            // Prompting before RESUMED throws from FragmentManager.
                            snapshotFlow { lifecycle.currentState }
                                .first { it.isAtLeast(Lifecycle.State.RESUMED) }
                            promptUnlock(
                                onSuccess = { lockController.unlock() },
                                onCancel = { finish() },
                                onError = { lockError = WriteFailures.ERR_BIOMETRIC },
                            )
                        }
                    } else {
                        AppRoot(onSecureWindow = { secure ->
                            if (secure) {
                                window.setFlags(
                                    WindowManager.LayoutParams.FLAG_SECURE,
                                    WindowManager.LayoutParams.FLAG_SECURE,
                                )
                            } else {
                                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                            }
                        })
                    }
                }
            }
        }
    }

    fun promptUnlock(
        crypto: BiometricPrompt.CryptoObject? = null,
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onCancel: () -> Unit,
        onError: ((Int) -> Unit)? = null,
    ) {
        if (!promptGate.compareAndSet(false, true)) return
        val needsCrypto = crypto != null
        val authenticators = if (BiometricPromptPolicy.allowsDeviceCredential(Build.VERSION.SDK_INT, needsCrypto)) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        }
        val can = runCatching { BiometricManager.from(this).canAuthenticate(authenticators) }
            .getOrDefault(BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE)
        if (can != BiometricManager.BIOMETRIC_SUCCESS) {
            promptGate.set(false)
            onError?.invoke(can) ?: onCancel()
            return
        }
        val release = { promptGate.set(false) }
        val executor = ContextCompat.getMainExecutor(this)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                release()
                onSuccess(result)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                release()
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                ) {
                    onCancel()
                } else {
                    onError?.invoke(errorCode) ?: onCancel()
                }
            }
        }
        try {
            val prompt = BiometricPrompt(this, executor, callback)
            val builder = BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.unlock_title))
                .setSubtitle(getString(R.string.unlock_subtitle))
            if (BiometricPromptPolicy.allowsDeviceCredential(Build.VERSION.SDK_INT, needsCrypto)) {
                builder.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                )
            } else {
                builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                builder.setNegativeButtonText(getString(R.string.cancel))
            }
            val info = builder.build()
            if (crypto != null) prompt.authenticate(info, crypto) else prompt.authenticate(info)
        } catch (t: IllegalArgumentException) {
            release()
            onError?.invoke(BiometricPrompt.ERROR_HW_NOT_PRESENT) ?: onCancel()
        } catch (t: IllegalStateException) {
            release()
            onError?.invoke(BiometricPrompt.ERROR_HW_NOT_PRESENT) ?: onCancel()
        }
    }

    fun lockBeforePan(onUnlocked: () -> Unit) {
        promptUnlock(onSuccess = { onUnlocked() }, onCancel = {})
    }
}

