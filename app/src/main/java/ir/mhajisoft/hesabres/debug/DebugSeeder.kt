package ir.mhajisoft.hesabres.debug

import ir.mhajisoft.hesabres.BuildConfig
import ir.mhajisoft.hesabres.data.local.datastore.SettingsDataStore
import ir.mhajisoft.hesabres.data.repository.LedgerRepository
import ir.mhajisoft.hesabres.domain.model.Direction
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebugSeeder @Inject constructor(
    private val ledger: LedgerRepository,
    private val settings: SettingsDataStore,
) {
    suspend fun seedIfNeeded(cashAccountId: String) {
        if (!BuildConfig.DEBUG || !BuildConfig.SEED_SAMPLE_DATA) return
        val current = settings.settings.first()
        if (current.sampleDataSeeded) return
        if (ledger.txnCount() > 0) {
            settings.markSampleSeeded()
            return
        }
        val now = System.currentTimeMillis()
        ledger.newExpenseOrIncome(
            accountId = cashAccountId,
            categoryId = "sys-food",
            amount = 250_0000L,
            direction = Direction.OUT,
            note = "نمونه: ناهار",
            occurredAt = now - 86_400_000L,
        )
        ledger.newExpenseOrIncome(
            accountId = cashAccountId,
            categoryId = "sys-salary",
            amount = 200_000_000L,
            direction = Direction.IN,
            note = "نمونه: حقوق",
            occurredAt = now - 3 * 86_400_000L,
        )
        settings.markSampleSeeded()
    }
}
