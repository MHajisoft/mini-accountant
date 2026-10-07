package ir.mhajisoft.hesabres.domain.people

import ir.mhajisoft.hesabres.domain.ledger.ComposerRules
import ir.mhajisoft.hesabres.domain.model.Account
import ir.mhajisoft.hesabres.domain.money.Money

/** Settle-up amounts before a person-wallet transfer is posted. */
object SettleRules {
    fun validate(
        amountDisplay: String,
        toman: Boolean,
        personAccountId: String,
        ledgerAccounts: List<Account>,
        preferredAccountId: String,
    ): String? {
        val amount = Money.parseDisplayAmount(amountDisplay, toman) ?: return ComposerRules.ERR_AMOUNT
        if (amount <= 0L) return ComposerRules.ERR_AMOUNT_ZERO
        val other = ComposerRules.resolveLedgerAccountId(preferredAccountId, ledgerAccounts)
            ?: return ComposerRules.ERR_NO_ACCOUNT
        if (other == personAccountId) return ComposerRules.ERR_SAME_ACCOUNTS
        return null
    }
}
