package ir.mhajisoft.hesabres.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.mhajisoft.hesabres.BuildConfig
import ir.mhajisoft.hesabres.data.archive.ArchiveRepository
import ir.mhajisoft.hesabres.data.backup.BackupRepository
import ir.mhajisoft.hesabres.data.local.datastore.SettingsDataStore
import ir.mhajisoft.hesabres.data.local.datastore.UserSettings
import ir.mhajisoft.hesabres.data.repository.AccountBalance
import ir.mhajisoft.hesabres.data.repository.LedgerRepository
import ir.mhajisoft.hesabres.data.repository.PlannerRepository
import ir.mhajisoft.hesabres.data.repository.VaultRepository
import ir.mhajisoft.hesabres.debug.DebugSeeder
import ir.mhajisoft.hesabres.domain.jalali.JalaliConverter
import ir.mhajisoft.hesabres.domain.jalali.JalaliYmd
import ir.mhajisoft.hesabres.domain.planner.Cheque
import ir.mhajisoft.hesabres.domain.planner.ChequeStatus
import ir.mhajisoft.hesabres.domain.planner.InstallmentPlan
import ir.mhajisoft.hesabres.domain.planner.MonthBudget
import ir.mhajisoft.hesabres.domain.planner.PlannerRules
import ir.mhajisoft.hesabres.domain.model.Account
import ir.mhajisoft.hesabres.domain.model.AccountType
import ir.mhajisoft.hesabres.domain.model.AppLockSettings
import ir.mhajisoft.hesabres.domain.model.Category
import ir.mhajisoft.hesabres.domain.model.CategoryKind
import ir.mhajisoft.hesabres.domain.model.Direction
import ir.mhajisoft.hesabres.domain.model.FiscalYear
import ir.mhajisoft.hesabres.domain.model.LedgerTransaction
import ir.mhajisoft.hesabres.diag.DiagnosticLog
import ir.mhajisoft.hesabres.domain.crash.WriteFailures
import ir.mhajisoft.hesabres.domain.ledger.ComposerRules
import ir.mhajisoft.hesabres.domain.model.Person
import ir.mhajisoft.hesabres.domain.money.Money
import ir.mhajisoft.hesabres.domain.people.SettleRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
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
    val startupError: Boolean = false,
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
    private val plannerRepo: PlannerRepository,
) : ViewModel() {
    val vaultRepo get() = vault
    val backupRepo get() = backup
    val archiveRepo get() = archive
    val settingsStorePublic get() = settingsStore
    val planner get() = plannerRepo

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
    }.catch { error ->
        DiagnosticLog.recordCaught("ui.state", error)
        emit(AppUiState(ready = true, startupError = true))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages
    private val onboardingOnce = AtomicBoolean(false)

    private fun loggedLaunch(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(DiagnosticLog.coroutineHandler("viewmodel"), block = block)
    }

    private fun mappedFailure(where: String, t: Throwable): String {
        DiagnosticLog.recordCaught(where, t)
        return WriteFailures.map(t)
    }

    private fun report(t: Throwable) {
        _messages.tryEmit(mappedFailure("viewmodel", t))
    }

    init {
        loggedLaunch {
            DiagnosticLog.breadcrumb("db", "startup seed")
            runCatching {
                val s = settingsStore.settings.first()
                val now = System.currentTimeMillis()
                ledger.ensureSystemCategories()
                if (s.onboarded) {
                    ledger.ensureSeeded(now, s.fyStartMonth, s.fyStartDay)
                }
            }.onSuccess {
                DiagnosticLog.breadcrumb("db", "startup seed ok")
            }.onFailure { report(it) }
        }
    }

    fun completeOnboarding(
        startMonth: Int,
        startDay: Int,
        cashName: String,
        opening: Long,
        enableLock: Boolean,
        onError: (String) -> Unit = {},
    ) {
        if (!onboardingOnce.compareAndSet(false, true)) return
        loggedLaunch {
            runCatching {
                val now = System.currentTimeMillis()
                settingsStore.setFyStart(startMonth, startDay)
                ledger.applyFiscalStart(now, startMonth, startDay)
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
            }.onFailure {
                onboardingOnce.set(false)
                onError(mappedFailure("viewmodel", it))
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
        onOk: () -> Unit = {},
    ) {
        loggedLaunch {
            DiagnosticLog.breadcrumb("txn", if (income) "save income" else "save expense")
            val ui = state.value
            val resolved = ComposerRules.resolveSpendAccountId(accountId, ui.accounts)
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
                return@loggedLaunch
            }
            val ledgerAccountId = resolved ?: run {
                onError(ComposerRules.ERR_PICK_ACCOUNT)
                return@loggedLaunch
            }
            val amount = Money.parseDisplayAmount(amountDisplay, ui.settings.displayToman) ?: return@loggedLaunch
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
                DiagnosticLog.breadcrumb("txn", if (income) "save income ok" else "save expense ok")
                settingsStore.setLastUsed(ledgerAccountId, categoryId, expense = !income)
                onOk()
            }.onFailure { onError(mappedFailure("txn.save", it)) }
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
        onOk: () -> Unit = {},
    ) {
        loggedLaunch {
            DiagnosticLog.breadcrumb("txn", "save transfer")
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
                return@loggedLaunch
            }
            val amount = Money.parseDisplayAmount(amountDisplay, ui.settings.displayToman) ?: return@loggedLaunch
            val fee = Money.parseDisplayAmount(feeDisplay, ui.settings.displayToman)
            runCatching { ledger.postTransfer(from, to, amount, fee, at, note) }
                .onSuccess {
                    DiagnosticLog.breadcrumb("txn", "save transfer ok")
                    onOk()
                }
                .onFailure { onError(mappedFailure("txn.transfer", it)) }
        }
    }

    fun deleteTxn(id: String) {
        loggedLaunch {
            runCatching { ledger.deleteTransaction(id) }.onFailure { report(it) }
        }
    }

    fun addAccount(name: String, type: AccountType, include: Boolean, opening: Long) {
        loggedLaunch {
            runCatching {
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
            }.onFailure { report(it) }
        }
    }

    fun addCustomCategory(name: String, iconKey: String, color: Long, kind: CategoryKind) {
        loggedLaunch {
            runCatching { ledger.addCustomCategory(name, iconKey, color, kind) }.onFailure { report(it) }
        }
    }

    fun deleteCustomCategory(id: String, onError: (String) -> Unit = {}) {
        loggedLaunch {
            runCatching { ledger.deleteCustomCategory(id) }
                .onFailure { onError(mappedFailure("viewmodel", it)) }
        }
    }

    fun archiveAccount(id: String, archived: Boolean) {
        loggedLaunch { runCatching { ledger.archiveAccount(id, archived) }.onFailure { report(it) } }
    }

    fun updateAccount(account: Account) {
        loggedLaunch {
            runCatching { ledger.upsertAccount(account.copy(updatedAt = System.currentTimeMillis())) }
                .onFailure { report(it) }
        }
    }

    fun deleteTxnOrTransfer(txn: LedgerTransaction) {
        loggedLaunch {
            runCatching {
                val transferId = txn.transferId
                if (transferId != null) ledger.deleteTransfer(transferId) else ledger.deleteTransaction(txn.id)
            }.onFailure { report(it) }
        }
    }

    fun addPerson(
        firstName: String,
        lastName: String,
        phone: String?,
        email: String?,
        note: String?,
        avatarColor: Long,
        socials: List<ir.mhajisoft.hesabres.domain.model.SocialLink>,
    ) {
        loggedLaunch {
            runCatching { ledger.createPerson(firstName, lastName, phone, email, note, avatarColor, socials) }
                .onFailure { report(it) }
        }
    }

    fun updatePerson(person: Person) {
        loggedLaunch { runCatching { ledger.updatePersonProfile(person) }.onFailure { report(it) } }
    }

    fun setToman(v: Boolean) { loggedLaunch { settingsStore.setDisplayToman(v) } }
    fun setLock(s: AppLockSettings) { loggedLaunch { settingsStore.setLock(s) } }
    fun setDefaultAccount(id: String) { loggedLaunch { settingsStore.setDefaultAccount(id) } }
    fun setFyStart(month: Int, day: Int, confirm: Boolean) {
        loggedLaunch {
            runCatching {
                settingsStore.setFyStart(month, day)
                ledger.rebuildFiscalYear(month, day, System.currentTimeMillis(), hasTxns = confirm)
            }.onFailure { report(it) }
        }
    }

    fun monthChart(fyId: String, month: Int) = loggedLaunch {
        runCatching { ledger.monthPnL(fyId, month) }.onFailure { report(it) }
    }

    suspend fun pnl(fyId: String, month: Int) = ledger.monthPnL(fyId, month)

    fun archiveYear(id: String) {
        loggedLaunch { runCatching { archive.archiveClosedYear(id) }.onFailure { report(it) } }
    }

    fun closeCurrentYear() {
        loggedLaunch {
            runCatching {
                val s = settingsStore.settings.first()
                ledger.closeCurrentAndStartNext(System.currentTimeMillis(), s.fyStartMonth, s.fyStartDay)
            }.onFailure { report(it) }
        }
    }

    fun payPerson(
        personAccountId: String,
        amountDisplay: String,
        theyPay: Boolean,
        onError: (String) -> Unit = {},
        onDone: () -> Unit = {},
    ) {
        loggedLaunch {
            val ui = state.value
            val ledgerAccounts = ui.accounts.filter { it.type != AccountType.PERSON }
            val problem = SettleRules.validate(
                amountDisplay = amountDisplay,
                toman = ui.settings.displayToman,
                personAccountId = personAccountId,
                ledgerAccounts = ledgerAccounts,
                preferredAccountId = ui.settings.defaultAccountId.orEmpty(),
            )
            if (problem != null) {
                onError(problem)
                return@loggedLaunch
            }
            val amount = Money.parseDisplayAmount(amountDisplay, ui.settings.displayToman) ?: return@loggedLaunch
            val other = ComposerRules.resolveLedgerAccountId(
                ui.settings.defaultAccountId.orEmpty(),
                ledgerAccounts,
            ) ?: return@loggedLaunch
            runCatching {
                if (theyPay) {
                    ledger.postTransfer(personAccountId, other, amount, null, System.currentTimeMillis(), "")
                } else {
                    ledger.postTransfer(other, personAccountId, amount, null, System.currentTimeMillis(), "")
                }
            }.onSuccess { onDone() }.onFailure { onError(mappedFailure("viewmodel", it)) }
        }
    }

    fun updateTxn(txn: LedgerTransaction) {
        loggedLaunch { runCatching { ledger.updateTransaction(txn) }.onFailure { report(it) } }
    }

    fun setOpening(accountId: String, amountDisplay: String) {
        loggedLaunch {
            runCatching {
                val s = state.value.settings
                val amount = Money.parseDisplayAmount(amountDisplay, s.displayToman) ?: 0L
                ledger.setOpeningBalance(accountId, amount, replaceTxn = true)
            }.onFailure { report(it) }
        }
    }

    val jalaliToday get() = JalaliConverter.fromEpochMillis(System.currentTimeMillis())

    fun saveNote(body: String, ymd: JalaliYmd, onError: (String) -> Unit = {}, onOk: () -> Unit = {}) {
        val problem = PlannerRules.validateNote(body)
        if (problem != null) {
            onError(problem)
            return
        }
        loggedLaunch {
            runCatching { plannerRepo.saveNote(body, ymd) }
                .onSuccess { onOk() }
                .onFailure { onError(mappedFailure("note.save", it)) }
        }
    }

    fun deleteNote(id: String) {
        loggedLaunch { runCatching { plannerRepo.deleteNote(id) }.onFailure { report(it) } }
    }

    fun saveCheque(cheque: Cheque, onError: (String) -> Unit = {}, onOk: () -> Unit = {}) {
        val problem = PlannerRules.validateCheque(cheque.amount, cheque.counterparty)
        if (problem != null) {
            onError(problem)
            return
        }
        loggedLaunch {
            runCatching { plannerRepo.saveCheque(cheque) }
                .onSuccess { onOk() }
                .onFailure { onError(mappedFailure("cheque.save", it)) }
        }
    }

    fun settleCheque(cheque: Cheque) {
        loggedLaunch {
            runCatching { plannerRepo.saveCheque(cheque.copy(status = ChequeStatus.SETTLED)) }.onFailure { report(it) }
        }
    }

    fun deleteCheque(id: String) {
        loggedLaunch { runCatching { plannerRepo.deleteCheque(id) }.onFailure { report(it) } }
    }

    fun saveInstallment(plan: InstallmentPlan, onError: (String) -> Unit = {}, onOk: () -> Unit = {}) {
        val problem = PlannerRules.validateInstallment(
            plan.title, plan.totalAmount, plan.installmentAmount, plan.count, plan.counterparty,
        )
        if (problem != null) {
            onError(problem)
            return
        }
        loggedLaunch {
            runCatching { plannerRepo.saveInstallment(plan) }
                .onSuccess { onOk() }
                .onFailure { onError(mappedFailure("installment.save", it)) }
        }
    }

    fun payInstallment(plan: InstallmentPlan) {
        loggedLaunch {
            runCatching { plannerRepo.saveInstallment(PlannerRules.recordPayment(plan)) }.onFailure { report(it) }
        }
    }

    fun deleteInstallment(id: String) {
        loggedLaunch { runCatching { plannerRepo.deleteInstallment(id) }.onFailure { report(it) } }
    }

    fun saveBudget(year: Int, month: Int, limitRials: Long, onError: (String) -> Unit = {}) {
        val problem = PlannerRules.validateBudget(limitRials)
        if (problem != null) {
            onError(problem)
            return
        }
        loggedLaunch {
            runCatching { plannerRepo.saveBudget(MonthBudget(year, month, limitRials)) }.onFailure { report(it) }
        }
    }

    fun reorderAccounts(ids: List<String>) {
        loggedLaunch { runCatching { ledger.reorderAccounts(ids) }.onFailure { report(it) } }
    }

    fun reorderPeople(ids: List<String>) {
        loggedLaunch { runCatching { ledger.reorderPeople(ids) }.onFailure { report(it) } }
    }

    val openings = ledger.openingsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyMap(),
    )
}
