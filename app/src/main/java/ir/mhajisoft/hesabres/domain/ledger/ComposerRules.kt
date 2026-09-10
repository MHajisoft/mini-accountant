package ir.mhajisoft.hesabres.domain.ledger

import ir.mhajisoft.hesabres.domain.model.Account
import ir.mhajisoft.hesabres.domain.model.AccountType
import ir.mhajisoft.hesabres.domain.model.Category
import ir.mhajisoft.hesabres.domain.model.FiscalYear
import ir.mhajisoft.hesabres.domain.money.Money

/**
 * Validates the add-transaction composer before any Room write.
 * Missing FY / account / category must never crash the UI.
 */
object ComposerRules {
    const val ERR_NO_ACCOUNT = "حسابی برای ثبت تراکنش نیست. از «بیشتر» یک حساب بسازید."
    const val ERR_PICK_ACCOUNT = "حساب را انتخاب کنید."
    const val ERR_ACCOUNT_GONE = "حساب انتخاب‌شده دیگر وجود ندارد."
    const val ERR_NO_CATEGORY = "دسته‌بندی را انتخاب کنید."
    const val ERR_NO_FY = "سال مالی آماده نیست. برنامه را ببندید و دوباره باز کنید."
    const val ERR_AMOUNT = "مبلغ را به ریال یا تومان وارد کنید."
    const val ERR_AMOUNT_ZERO = "مبلغ باید بزرگ‌تر از صفر باشد."
    const val ERR_SAME_ACCOUNTS = "حساب مبدأ و مقصد باید متفاوت باشند."
    const val ERR_NEED_TWO_ACCOUNTS = "برای جابه‌جایی حداقل دو حساب لازم است."

    data class Draft(
        val accountId: String,
        val categoryId: String,
        val amountDisplay: String,
        val toman: Boolean,
        val accounts: List<Account>,
        val categories: List<Category>,
        val fiscalYear: FiscalYear?,
        val toAccountId: String = "",
        val transfer: Boolean = false,
    )

    fun resolveLedgerAccountId(preferred: String, accounts: List<Account>): String? {
        val live = accounts.filter { !it.archived }
        if (preferred.isNotBlank() && live.any { it.id == preferred }) return preferred
        return live.firstOrNull { it.type != AccountType.PERSON }?.id
            ?: live.firstOrNull()?.id
    }

    fun validate(draft: Draft): String? {
        if (draft.fiscalYear == null) return ERR_NO_FY
        val live = draft.accounts.filter { !it.archived }
        if (live.isEmpty()) return ERR_NO_ACCOUNT
        val amount = Money.parseDisplayAmount(draft.amountDisplay, draft.toman) ?: return ERR_AMOUNT
        if (amount <= 0L) return ERR_AMOUNT_ZERO
        if (draft.transfer) {
            if (live.size < 2) return ERR_NEED_TWO_ACCOUNTS
            val from = resolveLedgerAccountId(draft.accountId, draft.accounts) ?: return ERR_PICK_ACCOUNT
            val to = draft.toAccountId.takeIf { id -> live.any { it.id == id } }
                ?: live.firstOrNull { it.id != from }?.id
                ?: return ERR_NEED_TWO_ACCOUNTS
            if (from == to) return ERR_SAME_ACCOUNTS
            return null
        }
        val accountId = resolveLedgerAccountId(draft.accountId, draft.accounts) ?: return ERR_PICK_ACCOUNT
        if (live.none { it.id == accountId }) return ERR_ACCOUNT_GONE
        if (draft.categoryId.isBlank() || draft.categories.none { it.id == draft.categoryId }) {
            return ERR_NO_CATEGORY
        }
        return null
    }
}
