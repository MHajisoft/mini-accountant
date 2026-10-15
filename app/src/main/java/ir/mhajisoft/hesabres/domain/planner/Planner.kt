package ir.mhajisoft.hesabres.domain.planner

import ir.mhajisoft.hesabres.domain.jalali.JalaliCalendar
import ir.mhajisoft.hesabres.domain.jalali.JalaliYmd

enum class FlowSide { PAID, RECEIVED }

enum class ChequeStatus { OPEN, SETTLED }

enum class DueKind { NOTE, CHEQUE, INSTALLMENT }

data class Note(
    val id: String,
    val body: String,
    val year: Int,
    val month: Int,
    val day: Int,
    val occurredAt: Long,
    val createdAt: Long,
) {
    val ymd: JalaliYmd get() = JalaliYmd(year, month, day)
}

data class Cheque(
    val id: String,
    val side: FlowSide,
    val amount: Long,
    val counterparty: String,
    val personId: String?,
    val year: Int,
    val month: Int,
    val day: Int,
    val dueAt: Long,
    val status: ChequeStatus,
    val note: String,
    val createdAt: Long,
) {
    val ymd: JalaliYmd get() = JalaliYmd(year, month, day)
}

data class InstallmentPlan(
    val id: String,
    val side: FlowSide,
    val title: String,
    val totalAmount: Long,
    val installmentAmount: Long,
    val count: Int,
    val paidCount: Int,
    val counterparty: String,
    val personId: String?,
    val year: Int,
    val month: Int,
    val day: Int,
    val nextDueAt: Long,
    val note: String,
    val createdAt: Long,
) {
    val ymd: JalaliYmd get() = JalaliYmd(year, month, day)
    val open: Boolean get() = paidCount < count
    val remaining: Long get() = (totalAmount - installmentAmount * paidCount).coerceAtLeast(0L)
}

data class MonthBudget(
    val year: Int,
    val month: Int,
    val limitRials: Long,
)

data class DueItem(
    val id: String,
    val kind: DueKind,
    val title: String,
    val subtitle: String,
    val amount: Long?,
    val ymd: JalaliYmd,
    val overdue: Boolean,
)

object PlannerRules {
    const val ERR_NOTE = "متن یادداشت را بنویسید."
    const val ERR_AMOUNT = "مبلغ باید بزرگ‌تر از صفر باشد."
    const val ERR_WHO = "طرف حساب را بنویسید."
    const val ERR_TITLE = "عنوان قسط را بنویسید."
    const val ERR_COUNT = "تعداد قسط باید حداقل یک باشد."
    const val ERR_LIMIT = "سقف بودجه باید بزرگ‌تر از صفر باشد."

    fun validateNote(body: String): String? = if (body.isBlank()) ERR_NOTE else null

    fun validateCheque(amount: Long, counterparty: String): String? = when {
        amount <= 0L -> ERR_AMOUNT
        counterparty.isBlank() -> ERR_WHO
        else -> null
    }

    fun validateInstallment(
        title: String,
        totalAmount: Long,
        installmentAmount: Long,
        count: Int,
        counterparty: String,
    ): String? = when {
        title.isBlank() -> ERR_TITLE
        totalAmount <= 0L || installmentAmount <= 0L -> ERR_AMOUNT
        count < 1 -> ERR_COUNT
        counterparty.isBlank() -> ERR_WHO
        else -> null
    }

    fun validateBudget(limitRials: Long): String? = if (limitRials <= 0L) ERR_LIMIT else null

    fun recordPayment(plan: InstallmentPlan): InstallmentPlan {
        if (!plan.open) return plan
        val nextPaid = plan.paidCount + 1
        val shifted = JalaliCalendar.shiftMonth(plan.year, plan.month, 1)
        val due = JalaliCalendar.clampDay(shifted.first, shifted.second, plan.day)
        return plan.copy(
            paidCount = nextPaid,
            year = due.year,
            month = due.month,
            day = due.day,
        )
    }
}

object DueAgenda {
    fun forDay(
        day: JalaliYmd,
        today: JalaliYmd,
        notes: List<Note>,
        cheques: List<Cheque>,
        installments: List<InstallmentPlan>,
    ): List<DueItem> {
        val includeOverdue = day == today
        val items = ArrayList<DueItem>()
        notes.forEach { note ->
            if (sameDay(note.ymd, day)) {
                items += DueItem(note.id, DueKind.NOTE, note.body, "یادداشت", null, note.ymd, overdue = false)
            }
        }
        cheques.forEach { cheque ->
            if (cheque.status != ChequeStatus.OPEN) return@forEach
            val overdue = cheque.ymd < today
            if (sameDay(cheque.ymd, day) || (includeOverdue && overdue)) {
                items += DueItem(
                    id = cheque.id,
                    kind = DueKind.CHEQUE,
                    title = cheque.counterparty,
                    subtitle = if (cheque.side == FlowSide.PAID) "چک پرداختی" else "چک دریافتی",
                    amount = cheque.amount,
                    ymd = cheque.ymd,
                    overdue = overdue && !sameDay(cheque.ymd, day),
                )
            }
        }
        installments.forEach { plan ->
            if (!plan.open) return@forEach
            val overdue = plan.ymd < today
            if (sameDay(plan.ymd, day) || (includeOverdue && overdue)) {
                items += DueItem(
                    id = plan.id,
                    kind = DueKind.INSTALLMENT,
                    title = plan.title,
                    subtitle = if (plan.side == FlowSide.PAID) "قسط پرداختی" else "قسط دریافتی",
                    amount = plan.installmentAmount,
                    ymd = plan.ymd,
                    overdue = overdue && !sameDay(plan.ymd, day),
                )
            }
        }
        return items.sortedWith(compareBy<DueItem> { !it.overdue }.thenBy { it.kind.ordinal })
    }

    private fun sameDay(a: JalaliYmd, b: JalaliYmd) = a.year == b.year && a.month == b.month && a.day == b.day
}

object BudgetMath {
    fun remaining(limitRials: Long, spentRials: Long): Long = limitRials - spentRials

    fun progress(limitRials: Long, spentRials: Long): Float {
        if (limitRials <= 0L) return 0f
        return (spentRials.toFloat() / limitRials.toFloat()).coerceIn(0f, 1f)
    }
}

object ListOrder {
    fun <T> move(items: List<T>, fromIndex: Int, toIndex: Int): List<T> {
        if (fromIndex !in items.indices || toIndex !in items.indices || fromIndex == toIndex) return items
        val next = items.toMutableList()
        val item = next.removeAt(fromIndex)
        next.add(toIndex, item)
        return next
    }
}
