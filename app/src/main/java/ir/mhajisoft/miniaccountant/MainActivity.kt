package ir.mhajisoft.miniaccountant

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import ir.mhajisoft.miniaccountant.ui.AppRoot
import ir.mhajisoft.miniaccountant.ui.lock.LockScreen
import ir.mhajisoft.miniaccountant.ui.theme.MiniAccountantTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject lateinit var lockController: AppLockController

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(newBase.forceFaLocale())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiniAccountantTheme {
                val locked by lockController.locked.collectAsStateWithLifecycle()
                if (locked) {
                    BackHandler { finish() }
                    LockScreen(
                        onUnlock = { promptUnlock(onSuccess = { lockController.unlock() }, onCancel = { finish() }) },
                    )
                    LaunchedEffect(Unit) {
                        promptUnlock(onSuccess = { lockController.unlock() }, onCancel = { finish() })
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

    fun promptUnlock(
        crypto: BiometricPrompt.CryptoObject? = null,
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onCancel: () -> Unit,
        onError: ((Int) -> Unit)? = null,
    ) {
        val executor = ContextCompat.getMainExecutor(this)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess(result)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
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
        val prompt = BiometricPrompt(this, executor, callback)
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.unlock_title))
            .setSubtitle(getString(R.string.unlock_subtitle))
        if (crypto != null && Build.VERSION.SDK_INT < 30) {
            builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            builder.setNegativeButtonText(getString(R.string.cancel))
        } else if (Build.VERSION.SDK_INT >= 30) {
            builder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            )
        } else {
            @Suppress("DEPRECATION")
            builder.setDeviceCredentialAllowed(true)
        }
        val info = builder.build()
        if (crypto != null) prompt.authenticate(info, crypto) else prompt.authenticate(info)
    }

    fun lockBeforePan(onUnlocked: () -> Unit) {
        promptUnlock(onSuccess = { onUnlocked() }, onCancel = {})
    }
}

@Composable
fun activity(): MainActivity = LocalContext.current as MainActivity
