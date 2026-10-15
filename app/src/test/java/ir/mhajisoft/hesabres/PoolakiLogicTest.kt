package ir.mhajisoft.hesabres

import com.google.common.truth.Truth.assertThat
import ir.mhajisoft.hesabres.domain.backup.BackupReminder
import ir.mhajisoft.hesabres.domain.bank.BankLogoResources
import ir.mhajisoft.hesabres.domain.jalali.JalaliCalendar
import ir.mhajisoft.hesabres.domain.jalali.JalaliConverter
import ir.mhajisoft.hesabres.domain.jalali.JalaliLabels
import ir.mhajisoft.hesabres.domain.jalali.JalaliYmd
import ir.mhajisoft.hesabres.domain.model.Direction
import ir.mhajisoft.hesabres.domain.model.LedgerTransaction
import ir.mhajisoft.hesabres.domain.money.AmountKeypad
import ir.mhajisoft.hesabres.domain.planner.BudgetMath
import ir.mhajisoft.hesabres.domain.planner.Cheque
import ir.mhajisoft.hesabres.domain.planner.ChequeStatus
import ir.mhajisoft.hesabres.domain.planner.DueAgenda
import ir.mhajisoft.hesabres.domain.planner.FlowSide
import ir.mhajisoft.hesabres.domain.planner.InstallmentPlan
import ir.mhajisoft.hesabres.domain.planner.ListOrder
import ir.mhajisoft.hesabres.domain.planner.Note
import ir.mhajisoft.hesabres.domain.planner.PlannerRules
import ir.mhajisoft.hesabres.domain.report.ReportMath
import org.junit.Test

class PoolakiLogicTest {
    private val today = JalaliYmd(1405, 7, 23)

    @Test
    fun thursdayEpochIsPanjshanbehAndKnownCivilDayMatches() {
        val epoch = JalaliConverter.fromGregorian(1970, 1, 1)
        assertThat(JalaliCalendar.weekdayIndex(epoch)).isEqualTo(5)
        assertThat(JalaliLabels.weekdayNamesFa[5]).isEqualTo("پنجشنبه")
        val oct15 = JalaliConverter.fromGregorian(2026, 10, 15)
        assertThat(JalaliCalendar.weekdayName(oct15)).isEqualTo("پنجشنبه")
        assertThat(oct15).isEqualTo(JalaliYmd(1405, 7, 23))
    }

    @Test
    fun monthGridStartsOnSaturdayAndSkipsFutureSelectionMath() {
        val grid = JalaliCalendar.monthGrid(1405, 7)
        assertThat(grid.first().day).isNull()
        assertThat(grid.count { it.day != null }).isEqualTo(30)
        assertThat(grid.size % 7).isEqualTo(0)
        val shifted = JalaliCalendar.shiftMonth(1405, 12, 1)
        assertThat(shifted).isEqualTo(1406 to 1)
        assertThat(JalaliCalendar.shiftDay(JalaliYmd(1405, 7, 30), 1)).isEqualTo(JalaliYmd(1405, 8, 1))
    }

    @Test
    fun reportTotalsDeltaSharesAndDayGroups() {
        val food = txn("a", 150_000, Direction.OUT, 13, "sys-food")
        val wallet = txn("b", 11_600_000, Direction.OUT, 13, "sys-other")
        val income = txn("c", 5_000_000, Direction.IN, 10, "sys-salary")
        val prev = txn("d", 1_000_000, Direction.OUT, 4, "sys-food").copy(jalaliMonth = 6)
        val rows = listOf(food, wallet, income, prev)
        val mehr = ReportMath.monthTotals(rows, 1405, 7)
        assertThat(mehr.expense).isEqualTo(11_750_000)
        assertThat(mehr.income).isEqualTo(5_000_000)
        assertThat(mehr.net).isEqualTo(5_000_000 - 11_750_000)
        assertThat(ReportMath.monthDelta(mehr.expense, 1_000_000)).isEqualTo(10_750_000)
        val shares = ReportMath.categoryShares(rows, 1405, 7, expense = true)
        assertThat(shares.sumOf { it.percent }).isEqualTo(100)
        assertThat(shares.first().categoryId).isEqualTo("sys-other")
        val days = ReportMath.dayGroups(rows, 1405, 7)
        assertThat(days.first().day).isEqualTo(13)
        assertThat(days.first().transactions).hasSize(2)
        assertThat(days.first().totalSigned).isEqualTo(-(150_000 + 11_600_000))
        val bars = ReportMath.comparisonBars(rows, 1405, 7, expense = true)
        assertThat(bars).hasSize(3)
        assertThat(bars.last().current).isTrue()
        assertThat(bars.last().amount).isEqualTo(mehr.expense)
        assertThat(ReportMath.integerPercents(listOf(1, 1, 1)).sum()).isEqualTo(100)
    }

    @Test
    fun keypadEvaluatesPrecedenceAndPersianDigits() {
        var expr = ""
        "12+3×2".forEach { expr = AmountKeypad.press(expr, it.toString()) }
        assertThat(AmountKeypad.evaluate(expr)).isEqualTo(18)
        expr = AmountKeypad.press("۱۰", "۰")
        assertThat(AmountKeypad.evaluate(expr)).isEqualTo(100)
        assertThat(AmountKeypad.evaluate("8÷0")).isNull()
        assertThat(AmountKeypad.evaluate("3-9")).isNull()
        assertThat(AmountKeypad.press("15", AmountKeypad.BACKSPACE)).isEqualTo("1")
        assertThat(AmountKeypad.amountText("20+5")).isEqualTo("25")
    }

    @Test
    fun backupReminderAndLogoPackage() {
        val now = 10 * 86_400_000L
        assertThat(BackupReminder.isOverdue(null, now)).isTrue()
        assertThat(BackupReminder.isOverdue(now - 2 * 86_400_000L, now)).isFalse()
        assertThat(BackupReminder.isOverdue(now - 3 * 86_400_000L, now)).isTrue()
        val debugId = "ir.mhajisoft.hesabres.debug"
        assertThat(BankLogoResources.lookupPackage(debugId)).isEqualTo(BankLogoResources.NAMESPACE)
        assertThat(BankLogoResources.lookupPackage(debugId)).isNotEqualTo(debugId)
        assertThat(BankLogoResources.canonical("bank_mellat")).isEqualTo("bank_mellat")
        assertThat(BankLogoResources.canonical("missing")).isEqualTo(BankLogoResources.UNKNOWN)
        assertThat(BankLogoResources.known).contains("bank_melli")
    }

    @Test
    fun plannerDueItemsShiftAndReorder() {
        val note = Note("n", "یادداشت نان", 1405, 7, 23, 1, 1)
        val cheque = Cheque(
            "c", FlowSide.PAID, 2_000_000, "آبسرد", null, 1405, 7, 20, 1, ChequeStatus.OPEN, "", 1,
        )
        val plan = InstallmentPlan(
            "i", FlowSide.RECEIVED, "وام", 6_000_000, 2_000_000, 3, 0, "بابا", null,
            1405, 7, 23, 1, "", 1,
        )
        val todayItems = DueAgenda.forDay(today, today, listOf(note), listOf(cheque), listOf(plan))
        assertThat(todayItems.map { it.id }).containsAtLeast("c", "n", "i")
        assertThat(todayItems.first { it.id == "c" }.overdue).isTrue()
        val otherDay = DueAgenda.forDay(JalaliYmd(1405, 7, 22), today, listOf(note), listOf(cheque), listOf(plan))
        assertThat(otherDay).isEmpty()
        val paid = PlannerRules.recordPayment(plan)
        assertThat(paid.paidCount).isEqualTo(1)
        assertThat(paid.month).isEqualTo(8)
        assertThat(paid.day).isEqualTo(23)
        assertThat(PlannerRules.validateNote("  ")).isEqualTo(PlannerRules.ERR_NOTE)
        assertThat(PlannerRules.validateCheque(0, "الف")).isEqualTo(PlannerRules.ERR_AMOUNT)
        assertThat(BudgetMath.remaining(100, 40)).isEqualTo(60)
        assertThat(BudgetMath.progress(100, 250)).isEqualTo(1f)
        assertThat(ListOrder.move(listOf("a", "b", "c"), 0, 2)).containsExactly("b", "c", "a").inOrder()
    }

    private fun txn(id: String, amount: Long, direction: Direction, day: Int, category: String) = LedgerTransaction(
        id = id,
        accountId = "cash",
        categoryId = category,
        personId = null,
        amount = amount,
        direction = direction,
        note = "",
        occurredAt = day.toLong(),
        jalaliYear = 1405,
        jalaliMonth = 7,
        jalaliDay = day,
        fiscalYearId = "fy",
        transferId = null,
        createdAt = day.toLong(),
    )
}
