package ir.mhajisoft.miniaccountant.domain.ledger

import ir.mhajisoft.miniaccountant.domain.jalali.JalaliConverter
import ir.mhajisoft.miniaccountant.domain.model.Category
import ir.mhajisoft.miniaccountant.domain.model.CategoryKind
import ir.mhajisoft.miniaccountant.domain.model.Direction
import ir.mhajisoft.miniaccountant.domain.model.LedgerTransaction
import ir.mhajisoft.miniaccountant.domain.model.Transfer
import java.util.UUID

object SystemCategories {
    const val OPENING_BALANCE_ID = "sys-cat-opening"
    const val TRANSFER_ID = "sys-cat-transfer"
    const val FEE_ID = "sys-cat-fee"

    fun isExcludedFromCategoryCharts(categoryId: String?): Boolean =
        categoryId == OPENING_BALANCE_ID || categoryId == TRANSFER_ID
}

object CategoryRules {
    fun canDelete(category: Category): Boolean = !category.isSystem

    fun custom(
        name: String,
        iconKey: String,
        color: Long,
        kind: CategoryKind,
        sortOrder: Int = 100,
        id: String = UUID.randomUUID().toString(),
    ): Category {
        require(name.isNotBlank()) { "Category name is required" }
        require(kind != CategoryKind.TRANSFER) { "Custom transfer categories are not allowed" }
        return Category(
            id = id,
            name = name.trim(),
            iconKey = iconKey,
            color = color,
            kind = kind,
            parentId = null,
            isSystem = false,
            sortOrder = sortOrder,
        )
    }
}

data class TransferPosting(
    val transfer: Transfer,
    val sourceLeg: LedgerTransaction,
    val destLeg: LedgerTransaction,
    val feeLeg: LedgerTransaction?,
) {
    fun allTransactions(): List<LedgerTransaction> = buildList {
        add(sourceLeg)
        add(destLeg)
        feeLeg?.let { add(it) }
    }
}

/**
 * Writes two (optionally three) transaction legs that share [Transfer.id].
 * P&L queries MUST filter `transferId IS NULL`. Opening-balance rows use a
 * system category and are excluded from category charts.
 */
object TransferPoster {
    fun post(
        fromAccountId: String,
        toAccountId: String,
        amountRials: Long,
        feeRials: Long?,
        occurredAt: Long,
        fiscalYearId: String,
        note: String,
        feeCategoryId: String = SystemCategories.FEE_ID,
        transferCategoryId: String = SystemCategories.TRANSFER_ID,
        now: Long = occurredAt,
        transferId: String = UUID.randomUUID().toString(),
    ): TransferPosting {
        require(amountRials > 0L)
        require(fromAccountId != toAccountId)
        require(feeRials == null || feeRials >= 0L)

        val ymd = JalaliConverter.fromEpochMillis(occurredAt)
        val transfer = Transfer(
            id = transferId,
            fromAccountId = fromAccountId,
            toAccountId = toAccountId,
            amount = amountRials,
            fee = feeRials?.takeIf { it > 0L },
            occurredAt = occurredAt,
            fiscalYearId = fiscalYearId,
            note = note,
        )
        val source = LedgerTransaction(
            id = UUID.randomUUID().toString(),
            accountId = fromAccountId,
            categoryId = transferCategoryId,
            personId = null,
            amount = amountRials,
            direction = Direction.OUT,
            note = note,
            occurredAt = occurredAt,
            jalaliYear = ymd.year,
            jalaliMonth = ymd.month,
            jalaliDay = ymd.day,
            fiscalYearId = fiscalYearId,
            transferId = transferId,
            createdAt = now,
        )
        val dest = LedgerTransaction(
            id = UUID.randomUUID().toString(),
            accountId = toAccountId,
            categoryId = transferCategoryId,
            personId = null,
            amount = amountRials,
            direction = Direction.IN,
            note = note,
            occurredAt = occurredAt,
            jalaliYear = ymd.year,
            jalaliMonth = ymd.month,
            jalaliDay = ymd.day,
            fiscalYearId = fiscalYearId,
            transferId = transferId,
            createdAt = now,
        )
        val fee = if (feeRials != null && feeRials > 0L) {
            LedgerTransaction(
                id = UUID.randomUUID().toString(),
                accountId = fromAccountId,
                categoryId = feeCategoryId,
                personId = null,
                amount = feeRials,
                direction = Direction.OUT,
                note = "کارمزد",
                occurredAt = occurredAt,
                jalaliYear = ymd.year,
                jalaliMonth = ymd.month,
                jalaliDay = ymd.day,
                fiscalYearId = fiscalYearId,
                transferId = transferId,
                createdAt = now,
            )
        } else {
            null
        }
        return TransferPosting(transfer, source, dest, fee)
    }
}

object OpeningBalancePoster {
    fun post(
        accountId: String,
        amountSigned: Long,
        occurredAt: Long,
        fiscalYearId: String,
        now: Long = occurredAt,
    ): LedgerTransaction? {
        if (amountSigned == 0L) return null
        val ymd = JalaliConverter.fromEpochMillis(occurredAt)
        return LedgerTransaction(
            id = UUID.randomUUID().toString(),
            accountId = accountId,
            categoryId = SystemCategories.OPENING_BALANCE_ID,
            personId = null,
            amount = kotlin.math.abs(amountSigned),
            direction = if (amountSigned >= 0L) Direction.IN else Direction.OUT,
            note = "موجودی اول دوره",
            occurredAt = occurredAt,
            jalaliYear = ymd.year,
            jalaliMonth = ymd.month,
            jalaliDay = ymd.day,
            fiscalYearId = fiscalYearId,
            transferId = null,
            createdAt = now,
        )
    }
}

object ProfitAndLoss {
    fun includeInPnL(txn: LedgerTransaction): Boolean = txn.transferId == null

    fun includeInCategoryChart(txn: LedgerTransaction, categories: Map<String, Category>): Boolean {
        if (txn.transferId != null) return false
        if (SystemCategories.isExcludedFromCategoryCharts(txn.categoryId)) return false
        val kind = txn.categoryId?.let { categories[it]?.kind } ?: return true
        return kind != ir.mhajisoft.miniaccountant.domain.model.CategoryKind.TRANSFER
    }
}

object BalanceMath {
    fun accountBalance(
        openingSigned: Long,
        liveTxns: List<LedgerTransaction>,
        archivedSnapshotsSigned: Long = 0L,
    ): Long {
        val live = liveTxns.sumOf { it.signedRials }
        return archivedSnapshotsSigned + openingSigned + live
    }
}
