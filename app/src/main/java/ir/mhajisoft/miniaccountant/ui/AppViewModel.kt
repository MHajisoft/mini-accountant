package ir.mhajisoft.miniaccountant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.mhajisoft.miniaccountant.BuildConfig
import ir.mhajisoft.miniaccountant.data.archive.ArchiveRepository
import ir.mhajisoft.miniaccountant.data.backup.BackupRepository
import ir.mhajisoft.miniaccountant.data.local.datastore.SettingsDataStore
import ir.mhajisoft.miniaccountant.data.local.datastore.UserSettings
import ir.mhajisoft.miniaccountant.data.repository.AccountBalance
import ir.mhajisoft.miniaccountant.data.repository.LedgerRepository
import ir.mhajisoft.miniaccountant.data.repository.VaultRepository
import ir.mhajisoft.miniaccountant.debug.DebugSeeder
import ir.mhajisoft.miniaccountant.domain.jalali.JalaliConverter
import ir.mhajisoft.miniaccountant.domain.model.Account
import ir.mhajisoft.miniaccountant.domain.model.AccountType
import ir.mhajisoft.miniaccountant.domain.model.AppLockSettings
import ir.mhajisoft.miniaccountant.domain.model.Category
import ir.mhajisoft.miniaccountant.domain.model.Direction
import ir.mhajisoft.miniaccountant.domain.model.FiscalYear
import ir.mhajisoft.miniaccountant.domain.model.LedgerTransaction
import ir.mhajisoft.miniaccountant.domain.model.Person
import ir.mhajisoft.miniaccountant.domain.money.Money
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AppUiState(
    val settings: UserSettings = UserSettings(),
    val balances: List<AccountBalance> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val recent: List<LedgerTransaction> = emptyList(),
    val txns: List<LedgerTransaction> = emptyList(),
    val people: List<Person> = emptyList(),
    val fy: FiscalYear? = null,
    val ready: Boolean = false,
) {
    val total: Long
        get() = balances.filter { it.account.includeInTotal }.sumOf { it.balanceSigned }
}

@HiltViewModel
class AppViewModel @Inject constructor(
    private val ledger: LedgerRepository,
    private val vault: VaultRepository,
    private val settingsStore: SettingsDataStore,
    private val backup: BackupRepository,
    private val archive: ArchiveRepository,
    private val seeder: DebugSeeder,
) : ViewModel() {
    val vaultRepo get() = vault
    val backupRepo get() = backup
    val archiveRepo get() = archive
    val settingsStorePublic get() = settingsStore

    val state = combine(
        combine(
            settingsStore.settings,
            ledger.homeBalancesFlow,
            ledger.accountsFlow,
            ledger.categoriesFlow,
            ledger.recentTxnsFlow,
        ) { settings, balances, accounts, categories, recent ->
            Triple(settings, balances to accounts, categories to recent)
        },
        combine(
            ledger.allTxnsFlow,
            ledger.peopleFlow,
            ledger.currentFyFlow,
        ) { txns, people, fy -> Triple(txns, people, fy) },
    ) { left, right ->
        val settings = left.first
        val balances = left.second.first
        val accounts = left.second.second
        val categories = left.third.first
        val recent = left.third.second
        AppUiState(
            settings = settings,
            balances = balances,
            accounts = accounts,
            categories = categories,
            recent = recent,
            txns = right.first,
            people = right.second,
            fy = right.third,
            ready = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    init {
        viewModelScope.launch {
            val s = state.value.settings
            val now = System.currentTimeMillis()
            ledger.ensureSeeded(now, 1, 1)
        }
    }

    fun completeOnboarding(
        startMonth: Int,
        startDay: Int,
        cashName: String,
        opening: Long,
        enableLock: Boolean,
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            settingsStore.setFyStart(startMonth, startDay)
            ledger.ensureSeeded(now, startMonth, startDay)
            val account = Account(
                id = UUID.randomUUID().toString(),
                name = cashName.ifBlank { "نقد" },
                type = AccountType.CASH,
                includeInTotal = true,
                archived = false,
                color = 0xFF0F766E,
                sortOrder = 0,
                createdAt = now,
                updatedAt = now,
            )
            ledger.upsertAccount(account)
            settingsStore.setDefaultAccount(account.id)
            if (opening != 0L) ledger.setOpeningBalance(account.id, opening, replaceTxn = true)
            if (enableLock) settingsStore.setLock(AppLockSettings(enabled = true, timeoutSec = 60, lockOnLeave = true))
            settingsStore.setOnboarded()
            if (BuildConfig.DEBUG && BuildConfig.SEED_SAMPLE_DATA) {
                seeder.seedIfNeeded(account.id)
            }
        }
    }

    fun saveExpense(
        accountId: String,
        categoryId: String,
        amountDisplay: String,
        note: String,
        occurredAt: Long,
        income: Boolean,
    ) {
        viewModelScope.launch {
            val s = state.value.settings
            val amount = Money.parseDisplayAmount(amountDisplay, s.displayToman) ?: return@launch
            ledger.newExpenseOrIncome(
                accountId = accountId,
                categoryId = categoryId,
                amount = amount,
                direction = if (income) Direction.IN else Direction.OUT,
                note = note,
                occurredAt = occurredAt,
            )
            settingsStore.setLastUsed(accountId, categoryId, expense = !income)
        }
    }

    fun saveTransfer(from: String, to: String, amountDisplay: String, feeDisplay: String, note: String, at: Long) {
        viewModelScope.launch {
            val s = state.value.settings
            val amount = Money.parseDisplayAmount(amountDisplay, s.displayToman) ?: return@launch
            val fee = Money.parseDisplayAmount(feeDisplay, s.displayToman)
            ledger.postTransfer(from, to, amount, fee, at, note)
        }
    }

    fun deleteTxn(id: String) {
        viewModelScope.launch {
            runCatching { ledger.deleteTransaction(id) }
        }
    }

    fun addAccount(name: String, type: AccountType, include: Boolean, opening: Long) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val acc = Account(
                id = UUID.randomUUID().toString(),
                name = name,
                type = type,
                includeInTotal = include,
                archived = false,
                color = 0xFF1565C0,
                sortOrder = 10,
                createdAt = now,
                updatedAt = now,
            )
            ledger.upsertAccount(acc)
            if (opening != 0L) ledger.setOpeningBalance(acc.id, opening, true)
        }
    }

    fun addPerson(name: String, phone: String?, note: String?) {
        viewModelScope.launch { ledger.createPerson(name, phone, note, 0xFF6A1B9A) }
    }

    fun setToman(v: Boolean) { viewModelScope.launch { settingsStore.setDisplayToman(v) } }
    fun setLock(s: AppLockSettings) { viewModelScope.launch { settingsStore.setLock(s) } }
    fun setDefaultAccount(id: String) { viewModelScope.launch { settingsStore.setDefaultAccount(id) } }
    fun setFyStart(month: Int, day: Int, confirm: Boolean) {
        viewModelScope.launch {
            settingsStore.setFyStart(month, day)
            ledger.rebuildFiscalYear(month, day, System.currentTimeMillis(), hasTxns = confirm)
        }
    }

    fun monthChart(fyId: String, month: Int) = viewModelScope.launch { ledger.monthPnL(fyId, month) }

    suspend fun pnl(fyId: String, month: Int) = ledger.monthPnL(fyId, month)

    fun archiveYear(id: String) { viewModelScope.launch { archive.archiveClosedYear(id) } }

    fun updateTxn(txn: LedgerTransaction) {
        viewModelScope.launch { runCatching { ledger.updateTransaction(txn) } }
    }

    val jalaliToday get() = JalaliConverter.fromEpochMillis(System.currentTimeMillis())
}
