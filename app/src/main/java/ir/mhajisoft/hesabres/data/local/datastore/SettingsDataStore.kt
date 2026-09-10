package ir.mhajisoft.hesabres.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.mhajisoft.hesabres.domain.model.AppLockSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsStore by preferencesDataStore(name = "mini_accountant_settings")

data class UserSettings(
    val displayToman: Boolean = false,
    val defaultAccountId: String? = null,
    val onboarded: Boolean = false,
    val fyStartMonth: Int = 1,
    val fyStartDay: Int = 1,
    val lastAccountId: String? = null,
    val lastExpenseCategoryId: String? = null,
    val lastIncomeCategoryId: String? = null,
    val sampleDataSeeded: Boolean = false,
    val lock: AppLockSettings = AppLockSettings(),
)

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.settingsStore

    val settings: Flow<UserSettings> = store.data.map { p ->
        UserSettings(
            displayToman = p[Keys.toman] ?: false,
            defaultAccountId = p[Keys.defaultAccount],
            onboarded = p[Keys.onboarded] ?: false,
            fyStartMonth = p[Keys.fyMonth] ?: 1,
            fyStartDay = p[Keys.fyDay] ?: 1,
            lastAccountId = p[Keys.lastAccount],
            lastExpenseCategoryId = p[Keys.lastExpenseCat],
            lastIncomeCategoryId = p[Keys.lastIncomeCat],
            sampleDataSeeded = p[Keys.sampleSeeded] ?: false,
            lock = AppLockSettings(
                enabled = p[Keys.lockEnabled] ?: false,
                timeoutSec = p[Keys.lockTimeout] ?: 60,
                lockOnLeave = p[Keys.lockOnLeave] ?: true,
            ),
        )
    }

    suspend fun update(block: MutablePreferences.() -> Unit) {
        store.edit(block)
    }

    suspend fun setOnboarded() {
        store.edit { it[Keys.onboarded] = true }
    }

    suspend fun setLock(settings: AppLockSettings) {
        store.edit {
            it[Keys.lockEnabled] = settings.enabled
            it[Keys.lockTimeout] = settings.timeoutSec
            it[Keys.lockOnLeave] = settings.lockOnLeave
        }
    }

    suspend fun setDisplayToman(value: Boolean) {
        store.edit { it[Keys.toman] = value }
    }

    suspend fun setDefaultAccount(id: String?) {
        store.edit {
            if (id == null) it.remove(Keys.defaultAccount) else it[Keys.defaultAccount] = id
        }
    }

    suspend fun setLastUsed(accountId: String, categoryId: String, expense: Boolean) {
        store.edit {
            it[Keys.lastAccount] = accountId
            if (expense) it[Keys.lastExpenseCat] = categoryId else it[Keys.lastIncomeCat] = categoryId
        }
    }

    suspend fun setFyStart(month: Int, day: Int) {
        store.edit {
            it[Keys.fyMonth] = month
            it[Keys.fyDay] = day
        }
    }

    suspend fun markSampleSeeded() {
        store.edit { it[Keys.sampleSeeded] = true }
    }

    object Keys {
        val toman = booleanPreferencesKey("display_toman")
        val defaultAccount = stringPreferencesKey("default_account")
        val onboarded = booleanPreferencesKey("onboarded")
        val fyMonth = intPreferencesKey("fy_start_month")
        val fyDay = intPreferencesKey("fy_start_day")
        val lastAccount = stringPreferencesKey("last_account")
        val lastExpenseCat = stringPreferencesKey("last_expense_cat")
        val lastIncomeCat = stringPreferencesKey("last_income_cat")
        val sampleSeeded = booleanPreferencesKey("sample_seeded")
        val lockEnabled = booleanPreferencesKey("lock_enabled")
        val lockTimeout = intPreferencesKey("lock_timeout_sec")
        val lockOnLeave = booleanPreferencesKey("lock_on_leave")
        val lastBackground = longPreferencesKey("last_background_at")
    }
}
