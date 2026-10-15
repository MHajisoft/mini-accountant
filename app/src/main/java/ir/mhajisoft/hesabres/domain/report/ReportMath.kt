package ir.mhajisoft.hesabres.domain.report

import ir.mhajisoft.hesabres.domain.jalali.JalaliCalendar
import ir.mhajisoft.hesabres.domain.jalali.JalaliLabels
import ir.mhajisoft.hesabres.domain.jalali.JalaliYmd
import ir.mhajisoft.hesabres.domain.ledger.SystemCategories
import ir.mhajisoft.hesabres.domain.model.Direction
import ir.mhajisoft.hesabres.domain.model.LedgerTransaction
import ir.mhajisoft.hesabres.domain.money.PersianDigits

data class FlowTotals(val income: Long, val expense: Long) {
    val net: Long get() = income - expense
    val count: Int get() = incomeCount + expenseCount
    var incomeCount: Int = 0
    var expenseCount: Int = 0
}

data class MonthBar(
    val year: Int,
    val month: Int,
    val amount: Long,
    val current: Boolean,
) {
    val label: String get() = JalaliLabels.monthName(month)
}

data class CategoryShare(
    val categoryId: String,
    val amount: Long,
    val percent: Int,
)

data class DayGroup(
    val year: Int,
    val month: Int,
    val day: Int,
    val totalSigned: Long,
    val transactions: List<LedgerTransaction>,
) {
    val ymd: JalaliYmd get() = JalaliYmd(year, month, day)

    fun header(): String {
        val name = JalaliCalendar.weekdayName(ymd)
        val date = PersianDigits.toPersian("$day $monthName $year")
        return "$name، $date"
    }

    private val monthName: String get() = JalaliLabels.monthName(month)
}

object ReportMath {
    fun countsAsFlow(txn: LedgerTransaction): Boolean =
        txn.transferId == null && txn.categoryId != SystemCategories.OPENING_BALANCE_ID

    fun inMonth(
        txns: List<LedgerTransaction>,
        year: Int,
        month: Int,
        accountId: String? = null,
    ): List<LedgerTransaction> = txns.filter { txn ->
        countsAsFlow(txn) &&
            txn.jalaliYear == year &&
            txn.jalaliMonth == month &&
            (accountId == null || txn.accountId == accountId)
    }

    fun totals(txns: List<LedgerTransaction>): FlowTotals {
        var income = 0L
        var expense = 0L
        var incomeCount = 0
        var expenseCount = 0
        for (txn in txns) {
            if (txn.direction == Direction.IN) {
                income += txn.amount
                incomeCount++
            } else {
                expense += txn.amount
                expenseCount++
            }
        }
        return FlowTotals(income, expense).also {
            it.incomeCount = incomeCount
            it.expenseCount = expenseCount
        }
    }

    fun monthTotals(
        txns: List<LedgerTransaction>,
        year: Int,
        month: Int,
        accountId: String? = null,
    ): FlowTotals = totals(inMonth(txns, year, month, accountId))

    /** Positive means the selected side grew versus the previous month. */
    fun monthDelta(current: Long, previous: Long): Long = current - previous

    fun comparisonBars(
        txns: List<LedgerTransaction>,
        year: Int,
        month: Int,
        expense: Boolean,
        accountId: String? = null,
        count: Int = 3,
    ): List<MonthBar> {
        val start = count - 1
        return (start downTo 0).map { back ->
            val (y, m) = JalaliCalendar.shiftMonth(year, month, -back)
            val flow = monthTotals(txns, y, m, accountId)
            MonthBar(
                year = y,
                month = m,
                amount = if (expense) flow.expense else flow.income,
                current = back == 0,
            )
        }
    }

    fun dailyAmounts(
        txns: List<LedgerTransaction>,
        year: Int,
        month: Int,
        expense: Boolean,
        accountId: String? = null,
    ): List<Pair<Int, Long>> {
        val rows = inMonth(txns, year, month, accountId)
        return rows.groupBy { it.jalaliDay }
            .map { (day, list) ->
                val amount = list.sumOf { txn ->
                    val matches = if (expense) txn.direction == Direction.OUT else txn.direction == Direction.IN
                    if (matches) txn.amount else 0L
                }
                day to amount
            }
            .filter { it.second > 0L }
            .sortedBy { it.first }
    }

    fun categoryShares(
        txns: List<LedgerTransaction>,
        year: Int,
        month: Int,
        expense: Boolean,
        accountId: String? = null,
    ): List<CategoryShare> {
        val rows = inMonth(txns, year, month, accountId).filter {
            if (expense) it.direction == Direction.OUT else it.direction == Direction.IN
        }
        val byCat = linkedMapOf<String, Long>()
        for (txn in rows) {
            val id = txn.categoryId ?: ""
            byCat[id] = (byCat[id] ?: 0L) + txn.amount
        }
        val ordered = byCat.entries.sortedByDescending { it.value }
        val percents = integerPercents(ordered.map { it.value })
        return ordered.mapIndexed { index, entry ->
            CategoryShare(entry.key, entry.value, percents[index])
        }
    }

    fun dayGroups(
        txns: List<LedgerTransaction>,
        year: Int,
        month: Int,
        accountId: String? = null,
        categoryId: String? = null,
        query: String = "",
    ): List<DayGroup> {
        val needle = PersianDigits.toAscii(query).trim().lowercase()
        val rows = txns.filter { txn ->
            txn.jalaliYear == year &&
                txn.jalaliMonth == month &&
                (accountId == null || txn.accountId == accountId) &&
                (categoryId == null || txn.categoryId == categoryId) &&
                (needle.isEmpty() || matchesQuery(txn, needle, query))
        }
        return rows.groupBy { it.jalaliDay }
            .map { (day, list) ->
                val sorted = list.sortedByDescending { it.occurredAt }
                DayGroup(
                    year = year,
                    month = month,
                    day = day,
                    totalSigned = sorted.sumOf { it.signedRials },
                    transactions = sorted,
                )
            }
            .sortedByDescending { it.day }
    }

    fun recent(txns: List<LedgerTransaction>, limit: Int = 6): List<LedgerTransaction> =
        txns.filter { countsAsFlow(it) }
            .sortedByDescending { it.occurredAt }
            .take(limit)

    fun exportText(
        year: Int,
        month: Int,
        expense: Boolean,
        totals: FlowTotals,
        shares: List<CategoryShare>,
        categoryName: (String) -> String,
    ): String {
        val side = if (expense) "خرج" else "دخل"
        val monthLabel = "${JalaliLabels.monthName(month)} ${PersianDigits.toPersian(year.toString())}"
        val focus = if (expense) totals.expense else totals.income
        return buildString {
            appendLine("گزارش $side $monthLabel")
            appendLine("جمع: ${PersianDigits.formatGrouped(focus)}")
            appendLine("دخل: ${PersianDigits.formatGrouped(totals.income)}")
            appendLine("خرج: ${PersianDigits.formatGrouped(totals.expense)}")
            appendLine("اختلاف: ${PersianDigits.formatGrouped(totals.net)}")
            if (shares.isNotEmpty()) {
                appendLine("دسته‌ها:")
                shares.forEach { share ->
                    val name = categoryName(share.categoryId).ifBlank { "بدون دسته" }
                    appendLine("$name  ${PersianDigits.toPersian(share.percent.toString())}٪  ${PersianDigits.formatGrouped(share.amount)}")
                }
            }
        }.trimEnd()
    }

    /**
     * Largest-remainder percentages. Parts of a non-empty positive total sum to 100.
     */
    fun integerPercents(amounts: List<Long>): List<Int> {
        val total = amounts.sum()
        if (total <= 0L || amounts.isEmpty()) return List(amounts.size) { 0 }
        val floors = amounts.map { (it * 100L / total).toInt() }.toMutableList()
        var remain = 100 - floors.sum()
        val order = amounts.indices.sortedByDescending { amounts[it] * 100L % total }
        var cursor = 0
        while (remain > 0 && order.isNotEmpty()) {
            floors[order[cursor % order.size]] += 1
            remain--
            cursor++
        }
        return floors
    }

    private fun matchesQuery(txn: LedgerTransaction, needleAscii: String, raw: String): Boolean {
        val noteAscii = PersianDigits.toAscii(txn.note).lowercase()
        return txn.note.contains(raw) || noteAscii.contains(needleAscii) || txn.id.contains(needleAscii)
    }
}
