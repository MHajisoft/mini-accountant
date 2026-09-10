package ir.mhajisoft.miniaccountant.data.repository

import ir.mhajisoft.miniaccountant.data.local.dao.AccountDao
import ir.mhajisoft.miniaccountant.data.local.dao.CategoryDao
import ir.mhajisoft.miniaccountant.data.local.dao.FiscalYearDao
import ir.mhajisoft.miniaccountant.data.local.dao.LedgerWriteDao
import ir.mhajisoft.miniaccountant.data.local.dao.OpeningBalanceDao
import ir.mhajisoft.miniaccountant.data.local.dao.PersonDao
import ir.mhajisoft.miniaccountant.data.local.dao.SnapshotDao
import ir.mhajisoft.miniaccountant.data.local.dao.TransactionDao
import ir.mhajisoft.miniaccountant.data.local.dao.TransferDao
import ir.mhajisoft.miniaccountant.data.local.db.MiniAccountantDatabase
import ir.mhajisoft.miniaccountant.data.local.toDomain
import ir.mhajisoft.miniaccountant.data.local.toEntity
import ir.mhajisoft.miniaccountant.domain.fiscal.FiscalRebucketer
import ir.mhajisoft.miniaccountant.domain.fiscal.FiscalYearCalculator
import ir.mhajisoft.miniaccountant.domain.jalali.JalaliConverter
import ir.mhajisoft.miniaccountant.domain.jalali.JalaliYmd
import ir.mhajisoft.miniaccountant.domain.ledger.BalanceMath
import ir.mhajisoft.miniaccountant.domain.ledger.CategoryCatalog
import ir.mhajisoft.miniaccountant.domain.ledger.CategoryRules
import ir.mhajisoft.miniaccountant.domain.ledger.ComposerRules
import ir.mhajisoft.miniaccountant.domain.ledger.OpeningBalancePoster
import ir.mhajisoft.miniaccountant.domain.ledger.SystemCategories
import ir.mhajisoft.miniaccountant.domain.ledger.TransferPoster
import ir.mhajisoft.miniaccountant.domain.model.Account
import ir.mhajisoft.miniaccountant.domain.model.AccountOpeningBalance
import ir.mhajisoft.miniaccountant.domain.model.AccountType
import ir.mhajisoft.miniaccountant.domain.model.Category
import ir.mhajisoft.miniaccountant.domain.model.CategoryKind
import ir.mhajisoft.miniaccountant.domain.model.Direction
import ir.mhajisoft.miniaccountant.domain.model.FiscalYear
import ir.mhajisoft.miniaccountant.domain.model.LedgerTransaction
import ir.mhajisoft.miniaccountant.domain.model.Person
import ir.mhajisoft.miniaccountant.domain.model.Transfer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class AccountBalance(
    val account: Account,
    val balanceSigned: Long,
)

@Singleton
class LedgerRepository @Inject constructor(
    private val db: MiniAccountantDatabase,
    private val accounts: AccountDao,
    private val categories: CategoryDao,
    private val fiscalYears: FiscalYearDao,
    private val txns: TransactionDao,
    private val transfers: TransferDao,
    private val people: PersonDao,
    private val openings: OpeningBalanceDao,
    private val snapshots: SnapshotDao,
    private val writes: LedgerWriteDao,
) {
    val accountsFlow: Flow<List<Account>> = accounts.observeAll().map { list -> list.map { it.toDomain() } }
    val activeAccountsFlow: Flow<List<Account>> = accounts.observeActive().map { list -> list.map { it.toDomain() } }
    val categoriesFlow: Flow<List<Category>> = categories.observeAll().map { list -> list.map { it.toDomain() } }
    val currentFyFlow: Flow<FiscalYear?> = fiscalYears.observeCurrent().map { it?.toDomain() }
    val fiscalYearsFlow: Flow<List<FiscalYear>> = fiscalYears.observeAll().map { list -> list.map { it.toDomain() } }
    val recentTxnsFlow: Flow<List<LedgerTransaction>> = combine(
        txns.observeRecent(20),
        fiscalYears.observeCurrent(),
    ) { list, fy ->
        list.filter { fy == null || it.fiscalYearId == fy.id }.take(5).map { it.toDomain() }
    }
    val allTxnsFlow: Flow<List<LedgerTransaction>> =
        txns.observeAll().map { list -> list.map { it.toDomain() } }
    val peopleFlow: Flow<List<Person>> = people.observeAll().map { list -> list.map { it.toDomain() } }
    val openingsFlow: Flow<Map<String, Long>> = combine(
        openings.observeAll(),
        fiscalYears.observeCurrent(),
    ) { rows, fy ->
        rows.filter { fy != null && it.fiscalYearId == fy.id }.associate { it.accountId to it.amountSigned }
    }

    val homeBalancesFlow: Flow<List<AccountBalance>> = combine(
        accounts.observeActive(),
        txns.observeAll(),
        snapshots.observeAll(),
        openings.observeAll(),
        fiscalYears.observeCurrent(),
    ) { accs, txnRows, snaps, openingRows, fy ->
        val fyId = fy?.id
        val openingMap = openingRows.filter { it.fiscalYearId == fyId }
            .associate { it.accountId to it.amountSigned }
        val snapMap = snaps.groupBy { it.accountId }
            .mapValues { e -> e.value.maxBy { it.capturedAt }.amountSigned }
        val liveByAccount = txnRows.filter { fyId == null || it.fiscalYearId == fyId }.groupBy { it.accountId }
        accs.map { entity ->
            val account = entity.toDomain()
            val live = liveByAccount[account.id].orEmpty().map { it.toDomain() }
            val opening = openingMap[account.id]
            val prior = if (opening != null) 0L else (snapMap[account.id] ?: 0L)
            val bal = BalanceMath.accountBalance(
                openingSigned = opening ?: 0L,
                liveTxns = live,
                archivedSnapshotsSigned = prior,
            )
            AccountBalance(account, bal)
        }
    }

    suspend fun homeBalancesNow(): List<AccountBalance> {
        val fy = fiscalYears.getCurrent() ?: return emptyList()
        return balancesForYear(fy.id)
    }

    suspend fun balancesForYear(fyId: String): List<AccountBalance> {
        val accs = accounts.getAll().filter { !it.archived }
        val openingMap = openings.forYear(fyId).associate { it.accountId to it.amountSigned }
        val liveByAccount = txns.forYear(fyId).groupBy { it.accountId }
        return accs.map { entity ->
            val live = liveByAccount[entity.id].orEmpty().map { it.toDomain() }
            AccountBalance(
                entity.toDomain(),
                BalanceMath.accountBalance(openingMap[entity.id] ?: 0L, live, 0L),
            )
        }
    }

    suspend fun closeCurrentAndStartNext(now: Long): FiscalYear {
        val current = fiscalYears.getCurrent() ?: error("no fiscal year")
        val oldStart = JalaliYmd(current.startJalaliYear, current.startJalaliMonth, current.startJalaliDay)
        val nextFromCycle = FiscalYearCalculator.windowStarting(FiscalYearCalculator.nextStart(oldStart))
        val containing = FiscalYearCalculator.windowContaining(
            now,
            current.startJalaliMonth,
            current.startJalaliDay,
        )
        val nextWindow = if (containing.startEpoch > current.startEpoch) containing else nextFromCycle
        val closings = balancesForYear(current.id)
        snapshots.upsertAll(
            closings.map { ab ->
                ir.mhajisoft.miniaccountant.data.local.entity.BalanceSnapshotEntity(
                    id = UUID.randomUUID().toString(),
                    fiscalYearId = current.id,
                    accountId = ab.account.id,
                    amountSigned = ab.balanceSigned,
                    capturedAt = now,
                )
            },
        )
        fiscalYears.closeAndDemote(current.id, now)
        val nextFy = FiscalYearCalculator.toModel(UUID.randomUUID().toString(), nextWindow, isCurrent = true)
        fiscalYears.upsert(nextFy.toEntity())
        closings.forEach { ab ->
            openings.upsert(
                ir.mhajisoft.miniaccountant.domain.model.AccountOpeningBalance(
                    fiscalYearId = nextFy.id,
                    accountId = ab.account.id,
                    amountSigned = ab.balanceSigned,
                ).toEntity(),
            )
        }
        return nextFy
    }

    suspend fun ensureSeeded(nowEpoch: Long, fyStartMonth: Int, fyStartDay: Int) {
        if (categories.count() == 0) {
            categories.insertAll(CategoryCatalog.systemCategories().map { it.toEntity() })
        }
        if (fiscalYears.getCurrent() == null && fiscalYears.getAll().isEmpty()) {
            val window = FiscalYearCalculator.windowContaining(nowEpoch, fyStartMonth, fyStartDay)
            val fy = FiscalYearCalculator.toModel(UUID.randomUUID().toString(), window, isCurrent = true)
            fiscalYears.upsert(fy.toEntity())
        }
    }

    suspend fun currentFiscalYear(): FiscalYear? = fiscalYears.getCurrent()?.toDomain()

    suspend fun upsertAccount(account: Account) = accounts.upsert(account.toEntity())

    suspend fun upsertCategory(category: Category) = categories.upsert(category.toEntity())

    suspend fun addCustomCategory(name: String, iconKey: String, color: Long, kind: CategoryKind): Category {
        val cat = CategoryRules.custom(name, iconKey, color, kind)
        categories.upsert(cat.toEntity())
        return cat
    }

    suspend fun deleteCustomCategory(id: String) {
        val entity = categories.get(id) ?: return
        val cat = entity.toDomain()
        require(CategoryRules.canDelete(cat)) { "دسته‌های سیستمی حذف نمی‌شوند" }
        val used = txns.getAll().any { it.categoryId == id }
        require(!used) { "این دسته در تراکنش‌ها استفاده شده است" }
        categories.delete(entity)
    }

    suspend fun archiveAccount(id: String, archived: Boolean) {
        val entity = accounts.get(id) ?: return
        accounts.update(entity.copy(archived = archived, updatedAt = System.currentTimeMillis()))
    }

    suspend fun addTransaction(txn: LedgerTransaction) {
        require(txn.amount >= 0L)
        require(txn.transferId == null) { "Transfer legs cannot be edited as a single transaction" }
        txns.upsert(txn.toEntity())
    }

    suspend fun updateTransaction(txn: LedgerTransaction) {
        val existing = txns.get(txn.id) ?: error("missing")
        require(existing.transferId == null) { "Transfer legs cannot be edited as a single transaction" }
        require(txn.transferId == null)
        txns.update(txn.toEntity())
    }

    suspend fun deleteTransaction(id: String) {
        val existing = txns.get(id) ?: return
        require(existing.transferId == null) { "Delete the transfer instead of a single leg" }
        txns.delete(existing)
    }

    suspend fun postTransfer(
        fromAccountId: String,
        toAccountId: String,
        amountRials: Long,
        feeRials: Long?,
        occurredAt: Long,
        note: String,
    ): Transfer {
        val fy = currentFiscalYear() ?: error(ComposerRules.ERR_NO_FY)
        val posting = TransferPoster.post(
            fromAccountId = fromAccountId,
            toAccountId = toAccountId,
            amountRials = amountRials,
            feeRials = feeRials,
            occurredAt = occurredAt,
            fiscalYearId = fy.id,
            note = note,
        )
        writes.postTransferAtomic(
            posting.transfer.toEntity(),
            posting.allTransactions().map { it.toEntity() },
        )
        return posting.transfer
    }

    suspend fun deleteTransfer(transferId: String) {
        writes.deleteTransferAtomic(transferId)
    }

    suspend fun getTransfer(id: String): Transfer? = transfers.get(id)?.toDomain()

    suspend fun createPerson(
        firstName: String,
        lastName: String,
        phone: String?,
        email: String?,
        instagram: String?,
        telegram: String?,
        whatsapp: String?,
        note: String?,
        avatarColor: Long,
    ): Person {
        val now = System.currentTimeMillis()
        val accountId = UUID.randomUUID().toString()
        val personId = UUID.randomUUID().toString()
        val display = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "شخص" }
        val account = Account(
            id = accountId,
            name = display,
            type = AccountType.PERSON,
            includeInTotal = false,
            archived = false,
            color = avatarColor,
            sortOrder = 100,
            createdAt = now,
            updatedAt = now,
        )
        val person = Person(
            id = personId,
            accountId = accountId,
            name = display,
            phone = phone,
            note = note,
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            email = email,
            instagram = instagram,
            telegram = telegram,
            whatsapp = whatsapp,
            avatarColor = avatarColor,
        )
        writes.createPersonAtomic(account.toEntity(), person.toEntity())
        return person
    }

    suspend fun upsertPerson(person: Person) = people.upsert(person.toEntity())

    suspend fun setOpeningBalance(accountId: String, amountSigned: Long, replaceTxn: Boolean) {
        val fy = currentFiscalYear() ?: error("no fiscal year")
        openings.upsert(AccountOpeningBalance(fy.id, accountId, amountSigned).toEntity())
        if (replaceTxn) {
            val existing = txns.forAccount(accountId).filter {
                it.categoryId == SystemCategories.OPENING_BALANCE_ID && it.fiscalYearId == fy.id
            }
            existing.forEach { txns.delete(it) }
            OpeningBalancePoster.post(
                accountId = accountId,
                amountSigned = amountSigned,
                occurredAt = fy.startEpoch,
                fiscalYearId = fy.id,
            )?.let { txns.upsert(it.toEntity()) }
        }
    }

    suspend fun rebuildFiscalYear(startMonth: Int, startDay: Int, nowEpoch: Long, hasTxns: Boolean) {
        val window = FiscalYearCalculator.windowContaining(nowEpoch, startMonth, startDay)
        val current = fiscalYears.getCurrent()
        if (current == null) {
            val fy = FiscalYearCalculator.toModel(UUID.randomUUID().toString(), window, true)
            fiscalYears.upsert(fy.toEntity())
            return
        }
        val existingTxns = txns.count()
        if (existingTxns == 0 || !hasTxns) {
            val updated = current.copy(
                label = window.label(),
                startJalaliYear = window.start.year,
                startJalaliMonth = window.start.month,
                startJalaliDay = window.start.day,
                endJalaliYear = window.end.year,
                endJalaliMonth = window.end.month,
                endJalaliDay = window.end.day,
                startEpoch = window.startEpoch,
                endEpoch = window.endEpoch,
            )
            fiscalYears.upsert(updated)
            return
        }
        val years = fiscalYears.getAll().map { it.toDomain() }.toMutableList()
        val rebuilt = FiscalYearCalculator.toModel(current.id, window, isCurrent = true, closedAt = current.closedAt)
        val idx = years.indexOfFirst { it.id == current.id }
        if (idx >= 0) years[idx] = rebuilt else years += rebuilt
        fiscalYears.upsert(rebuilt.toEntity())
        val allTx = txns.getAll().map { it.toDomain() }
        val result = FiscalRebucketer.rebucket(allTx, years)
        txns.upsertAll(result.updated.map { it.toEntity() })
    }

    suspend fun updatePersonProfile(person: Person) {
        val named = person.copy(name = person.displayName)
        people.upsert(named.toEntity())
        val acc = accounts.get(named.accountId) ?: return
        accounts.update(acc.copy(name = named.displayName, color = named.avatarColor, updatedAt = System.currentTimeMillis()))
    }

    suspend fun newExpenseOrIncome(
        accountId: String,
        categoryId: String,
        amount: Long,
        direction: Direction,
        note: String,
        occurredAt: Long,
        personId: String? = null,
    ): LedgerTransaction {
        require(accountId.isNotBlank()) { ComposerRules.ERR_PICK_ACCOUNT }
        accounts.get(accountId) ?: error(ComposerRules.ERR_ACCOUNT_GONE)
        val fy = currentFiscalYear() ?: error(ComposerRules.ERR_NO_FY)
        val ymd = JalaliConverter.fromEpochMillis(occurredAt)
        val txn = LedgerTransaction(
            id = UUID.randomUUID().toString(),
            accountId = accountId,
            categoryId = categoryId,
            personId = personId,
            amount = amount,
            direction = direction,
            note = note,
            occurredAt = occurredAt,
            jalaliYear = ymd.year,
            jalaliMonth = ymd.month,
            jalaliDay = ymd.day,
            fiscalYearId = fy.id,
            transferId = null,
            createdAt = System.currentTimeMillis(),
        )
        txns.upsert(txn.toEntity())
        return txn
    }

    suspend fun monthPnL(fyId: String, month: Int): List<LedgerTransaction> =
        txns.forYear(fyId).map { it.toDomain() }.filter {
            it.jalaliMonth == month && it.transferId == null &&
                !SystemCategories.isExcludedFromCategoryCharts(it.categoryId)
        }

    suspend fun search(query: String): Flow<List<LedgerTransaction>> =
        txns.search(query).map { list -> list.map { it.toDomain() } }

    suspend fun accountById(id: String): Account? = accounts.get(id)?.toDomain()

    suspend fun txnCount(): Int = txns.count()
}
