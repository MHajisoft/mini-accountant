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
import ir.mhajisoft.miniaccountant.domain.model.CategoryKind
import ir.mhajisoft.miniaccountant.domain.model.Direction
import ir.mhajisoft.miniaccountant.domain.model.FiscalYear
import ir.mhajisoft.miniaccountant.domain.model.LedgerTransaction
import ir.mhajisoft.miniaccountant.domain.ledger.ComposerRules
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
    val fiscalYears: List<FiscalYear> = emptyList(),
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
            ledger.fiscalYearsFlow,
        ) { txns, people, fy, years -> Triple(txns, people, fy to years) },
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
            fy = right.third.first,
            fiscalYears = right.third.second,
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
        onError: (String) -> Unit = {},
    ) {
        viewModelScope.launch {
            val ui = state.value
            val resolved = ComposerRules.resolveLedgerAccountId(accountId, ui.accounts)
            val err = ComposerRules.validate(
                ComposerRules.Draft(
                    accountId = resolved.orEmpty(),
                    categoryId = categoryId,
                    amountDisplay = amountDisplay,
                    toman = ui.settings.displayToman,
                    accounts = ui.accounts,
                    categories = ui.categories,
                    fiscalYear = ui.fy,
                ),
            )
            if (err != null) {
                onError(err)
                return@launch
            }
            val ledgerAccountId = resolved ?: run {
                onError(ComposerRules.ERR_PICK_ACCOUNT)
                return@launch
            }
            val amount = Money.parseDisplayAmount(amountDisplay, ui.settings.displayToman) ?: return@launch
            runCatching {
                ledger.newExpenseOrIncome(
                    accountId = ledgerAccountId,
                    categoryId = categoryId,
                    amount = amount,
                    direction = if (income) Direction.IN else Direction.OUT,
                    note = note,
                    occurredAt = occurredAt,
                )
            }.onSuccess {
                settingsStore.setLastUsed(ledgerAccountId, categoryId, expense = !income)
            }.onFailure { onError(mapWriteError(it)) }
        }
    }

    fun saveTransfer(
        from: String,
        to: String,
        amountDisplay: String,
        feeDisplay: String,
        note: String,
        at: Long,
        onError: (String) -> Unit = {},
    ) {
        viewModelScope.launch {
            val ui = state.value
            val err = ComposerRules.validate(
                ComposerRules.Draft(
                    accountId = from,
                    categoryId = "",
                    amountDisplay = amountDisplay,
                    toman = ui.settings.displayToman,
                    accounts = ui.accounts,
                    categories = ui.categories,
                    fiscalYear = ui.fy,
                    toAccountId = to,
                    transfer = true,
                ),
            )
            if (err != null) {
                onError(err)
                return@launch
            }
            val amount = Money.parseDisplayAmount(amountDisplay, ui.settings.displayToman) ?: return@launch
            val fee = Money.parseDisplayAmount(feeDisplay, ui.settings.displayToman)
            runCatching { ledger.postTransfer(from, to, amount, fee, at, note) }
                .onFailure { onError(mapWriteError(it)) }
        }
    }

    private fun mapWriteError(t: Throwable): String {
        val msg = t.message.orEmpty()
        return when {
            msg.contains("FOREIGN", ignoreCase = true) -> ComposerRules.ERR_ACCOUNT_GONE
            msg.contains("fiscal", ignoreCase = true) -> ComposerRules.ERR_NO_FY
            msg.any { it in '\u0600'..'\u06FF' } -> msg
            else -> ComposerRules.ERR_NO_FY
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

    fun addCustomCategory(name: String, iconKey: String, color: Long, kind: CategoryKind) {
        viewModelScope.launch { runCatching { ledger.addCustomCategory(name, iconKey, color, kind) } }
    }

    fun deleteCustomCategory(id: String, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            runCatching { ledger.deleteCustomCategory(id) }
                .onFailure { onError(it.message ?: "error") }
        }
    }

    fun archiveAccount(id: String, archived: Boolean) {
        viewModelScope.launch { ledger.archiveAccount(id, archived) }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch { ledger.upsertAccount(account.copy(updatedAt = System.currentTimeMillis())) }
    }

    fun deleteTxnOrTransfer(txn: LedgerTransaction) {
        viewModelScope.launch {
            val transferId = txn.transferId
            if (transferId != null) {
                runCatching { ledger.deleteTransfer(transferId) }
            } else {
                runCatching { ledger.deleteTransaction(txn.id) }
            }
        }
    }

    fun addPerson(name: String, phone: String?, note: String?) {
        viewModelScope.launch { ledger.createPerson(name, phone, note, 0xFF6A1B9A) }
    }

    fun updatePerson(person: Person) {
        viewModelScope.launch { ledger.updatePersonProfile(person) }
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

    fun closeCurrentYear() { viewModelScope.launch { ledger.closeCurrentAndStartNext(System.currentTimeMillis()) } }

    fun payPerson(personAccountId: String, amountDisplay: String, theyPay: Boolean, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val ui = state.value
            val amount = Money.parseDisplayAmount(amountDisplay, ui.settings.displayToman)
            if (amount == null) {
                onError(ComposerRules.ERR_AMOUNT)
                return@launch
            }
            if (amount <= 0L) {
                onError(ComposerRules.ERR_AMOUNT_ZERO)
                return@launch
            }
            val other = ComposerRules.resolveLedgerAccountId(
                ui.settings.defaultAccountId.orEmpty(),
                ui.accounts.filter { it.type != AccountType.PERSON },
            )
            if (other == null) {
                onError(ComposerRules.ERR_NO_ACCOUNT)
                return@launch
            }
            if (other == personAccountId) {
                onError(ComposerRules.ERR_SAME_ACCOUNTS)
                return@launch
            }
            runCatching {
                if (theyPay) {
                    ledger.postTransfer(personAccountId, other, amount, null, System.currentTimeMillis(), "")
                } else {
                    ledger.postTransfer(other, personAccountId, amount, null, System.currentTimeMillis(), "")
                }
            }.onFailure { onError(mapWriteError(it)) }
        }
    }

    fun updateTxn(txn: LedgerTransaction) {
        viewModelScope.launch { runCatching { ledger.updateTransaction(txn) } }
    }

    fun setOpening(accountId: String, amountDisplay: String) {
        viewModelScope.launch {
            val s = state.value.settings
            val amount = Money.parseDisplayAmount(amountDisplay, s.displayToman) ?: 0L
            ledger.setOpeningBalance(accountId, amount, replaceTxn = true)
        }
    }

    val jalaliToday get() = JalaliConverter.fromEpochMillis(System.currentTimeMillis())

    val openings = ledger.openingsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyMap(),
    )
}
