package ir.mhajisoft.miniaccountant

import android.app.Application
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.HiltAndroidApp
import ir.mhajisoft.miniaccountant.data.local.datastore.SettingsDataStore
import ir.mhajisoft.miniaccountant.domain.jalali.JalaliConverter
import ir.amirroid.jalalidate.configuration.JalaliDateGlobalConfiguration
import ir.amirroid.jalalidate.algorithm.defaults.BirashkAlgorithm as LibBirashk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@HiltAndroidApp
class MiniAccountantApp : Application() {
    @Inject lateinit var lockController: AppLockController

    override fun onCreate() {
        super.onCreate()
        JalaliDateGlobalConfiguration.convertAlgorithm = LibBirashk
        val fa = Locale.forLanguageTag("fa")
        Locale.setDefault(fa)
        lockController.attach()
    }
}

@Singleton
class AppLockController @Inject constructor(
    private val settings: SettingsDataStore,
) : DefaultLifecycleObserver {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _locked = MutableStateFlow(false)
    val locked: StateFlow<Boolean> = _locked
    @Volatile private var lastBackground: Long = 0L
    @Volatile private var started = false

    fun attach() {
        if (started) return
        started = true
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        scope.launch {
            val s = settings.settings.first()
            if (s.lock.enabled) _locked.value = true
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        lastBackground = System.currentTimeMillis()
        scope.launch {
            val s = settings.settings.first()
            if (s.lock.enabled && s.lock.lockOnLeave) {
                // lock after timeout when returning; also lock immediately if timeout is 0
                if (s.lock.timeoutSec <= 0) _locked.value = true
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        scope.launch {
            val s = settings.settings.first()
            if (!s.lock.enabled) {
                _locked.value = false
                return@launch
            }
            val elapsed = System.currentTimeMillis() - lastBackground
            if (lastBackground == 0L || elapsed >= s.lock.timeoutSec * 1000L) {
                _locked.value = true
            }
        }
    }

    fun lockNow() {
        _locked.value = true
    }

    fun unlock() {
        _locked.value = false
        lastBackground = System.currentTimeMillis()
    }
}

fun Context.forceFaLocale(): Context {
    val fa = Locale.forLanguageTag("fa")
    Locale.setDefault(fa)
    val config = resources.configuration
    config.setLocale(fa)
    config.setLayoutDirection(fa)
    return createConfigurationContext(config)
}

@Suppress("unused")
private val jalaliReady = JalaliConverter
