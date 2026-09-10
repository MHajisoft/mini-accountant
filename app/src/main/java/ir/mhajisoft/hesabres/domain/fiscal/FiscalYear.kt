package ir.mhajisoft.hesabres.domain.fiscal

import ir.mhajisoft.hesabres.domain.jalali.BirashkAlgorithm
import ir.mhajisoft.hesabres.domain.jalali.JalaliConverter
import ir.mhajisoft.hesabres.domain.jalali.JalaliYmd
import ir.mhajisoft.hesabres.domain.ledger.BalanceMath
import ir.mhajisoft.hesabres.domain.ledger.SystemCategories
import ir.mhajisoft.hesabres.domain.model.FiscalYear
import ir.mhajisoft.hesabres.domain.model.LedgerTransaction

data class FiscalWindow(
    val start: JalaliYmd,
    val end: JalaliYmd,
) {
    val startEpoch: Long get() = JalaliConverter.toEpochMillisStartOfDay(start)
    val endEpoch: Long get() = JalaliConverter.toEpochMillisEndOfDay(end)

    fun contains(epochMillis: Long): Boolean =
        epochMillis in startEpoch..endEpoch

    fun label(): String {
        return if (start.month == 1 && start.day == 1 && end.month == 12) {
            start.year.toString()
        } else {
            "${start.year}/${start.month.toString().padStart(2, '0')}–${end.year}/${end.month.toString().padStart(2, '0')}"
        }
    }
}

/**
 * Default fiscal year: 1 Farvardin of the Jalali year containing [nowEpoch]
 * through last day of Esfand (29, or 30 if Birashk leap).
 *
 * User may change the start Y/M/D in settings. End is the day before the next
 * start, wrapping into the following Jalali year.
 */
object FiscalYearCalculator {
    fun defaultWindowFor(nowEpochMillis: Long): FiscalWindow {
        val today = JalaliConverter.fromEpochMillis(nowEpochMillis)
        return windowStarting(JalaliYmd(today.year, 1, 1))
    }

    fun windowStarting(start: JalaliYmd): FiscalWindow {
        val nextStart = nextStart(start)
        val endEpochDay = JalaliConverter.toEpochDay(nextStart) - 1
        val end = JalaliConverter.fromEpochDay(endEpochDay)
        return FiscalWindow(start, end)
    }

    fun nextStart(start: JalaliYmd): JalaliYmd {
        val nextYear = start.year + 1
        val maxDay = BirashkAlgorithm.monthLength(nextYear, start.month)
        val day = minOf(start.day, maxDay)
        return JalaliYmd(nextYear, start.month, day)
    }

    fun windowContaining(nowEpochMillis: Long, startMonth: Int, startDay: Int): FiscalWindow {
        val today = JalaliConverter.fromEpochMillis(nowEpochMillis)
        val candidateThis = clampStart(today.year, startMonth, startDay)
        val windowThis = windowStarting(candidateThis)
        return if (nowEpochMillis >= windowThis.startEpoch) {
            windowThis
        } else {
            windowStarting(clampStart(today.year - 1, startMonth, startDay))
        }
    }

    private fun clampStart(year: Int, month: Int, day: Int): JalaliYmd {
        val m = month.coerceIn(1, 12)
        val d = day.coerceIn(1, BirashkAlgorithm.monthLength(year, m))
        return JalaliYmd(year, m, d)
    }

    fun toModel(id: String, window: FiscalWindow, isCurrent: Boolean, closedAt: Long? = null): FiscalYear =
        FiscalYear(
            id = id,
            label = window.label(),
            startJalaliYear = window.start.year,
            startJalaliMonth = window.start.month,
            startJalaliDay = window.start.day,
            endJalaliYear = window.end.year,
            endJalaliMonth = window.end.month,
            endJalaliDay = window.end.day,
            startEpoch = window.startEpoch,
            endEpoch = window.endEpoch,
            isCurrent = isCurrent,
            closedAt = closedAt,
        )
}

/**
 * Rules for first-year bootstrap and "close year / start next":
 * - Default start is 1 Farvardin unless the user chose another month/day.
 * - Opening balances are prior closings (or onboarding input), never sample rows.
 * - Opening-balance *transactions* are display-only and must not be added again
 *   on top of the openings table (that doubled every new year).
 */
object NewYearDefaults {
    fun clampStart(year: Int, month: Int, day: Int): JalaliYmd {
        val m = month.coerceIn(1, 12)
        val d = day.coerceIn(1, BirashkAlgorithm.monthLength(year, m))
        return JalaliYmd(year, m, d)
    }

    fun firstWindow(nowEpochMillis: Long, startMonth: Int, startDay: Int): FiscalWindow =
        FiscalYearCalculator.windowContaining(nowEpochMillis, startMonth, startDay)

    fun nextWindowAfter(
        current: FiscalYear,
        nowEpochMillis: Long,
        startMonth: Int,
        startDay: Int,
    ): FiscalWindow {
        var start = clampStart(current.startJalaliYear, startMonth, startDay)
        var window = FiscalYearCalculator.windowStarting(start)
        var guard = 0
        while (window.startEpoch <= current.endEpoch && guard++ < 8) {
            start = FiscalYearCalculator.nextStart(start)
            window = FiscalYearCalculator.windowStarting(start)
        }
        val containing = FiscalYearCalculator.windowContaining(nowEpochMillis, startMonth, startDay)
        return if (containing.startEpoch > current.startEpoch) containing else window
    }

    fun signedBalance(openingSigned: Long, liveTxns: List<LedgerTransaction>): Long {
        val live = liveTxns.filter { it.categoryId != SystemCategories.OPENING_BALANCE_ID }
        return BalanceMath.accountBalance(openingSigned, live, 0L)
    }
}

data class RebucketResult(
    val updated: List<LedgerTransaction>,
    val unchangedCount: Int,
)

/**
 * After the user changes FY start and confirms, re-assign [LedgerTransaction.fiscalYearId]
 * from [occurredAt]. Rows are never deleted.
 */
object FiscalRebucketer {
    fun rebucket(
        transactions: List<LedgerTransaction>,
        years: List<FiscalYear>,
    ): RebucketResult {
        val sorted = years.sortedBy { it.startEpoch }
        require(sorted.isNotEmpty())
        var unchanged = 0
        val updated = transactions.map { txn ->
            val match = sorted.firstOrNull { fy ->
                txn.occurredAt in fy.startEpoch..fy.endEpoch
            } ?: sorted.last()
            if (match.id == txn.fiscalYearId) {
                unchanged += 1
                txn
            } else {
                txn.copy(fiscalYearId = match.id)
            }
        }
        return RebucketResult(updated = updated, unchangedCount = unchanged)
    }
}
