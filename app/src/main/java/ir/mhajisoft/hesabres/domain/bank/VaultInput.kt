package ir.mhajisoft.hesabres.domain.bank

enum class VaultFieldError {
    PAN_REQUIRED,
    PAN,
    EXPIRY,
    CVV,
    CVV_REQUIRED,
    IBAN,
    ACCOUNT,
}

object VaultInput {
    fun isValidAccountNumber(raw: String): Boolean {
        val digits = CardMath.normalizeDigits(raw)
        return digits.length in 4..24
    }

    fun validateCard(
        panRaw: String,
        expiryMonth: Int,
        expiryYear: Int,
        rememberCvv: Boolean,
        cvvRaw: String,
        editing: Boolean,
        hasStoredCvv: Boolean,
    ): VaultFieldError? {
        if (expiryMonth !in 1..12 || expiryYear !in 1300..1600) return VaultFieldError.EXPIRY
        val digits = CardMath.normalizeDigits(panRaw)
        if (digits.isEmpty()) {
            if (!editing) return VaultFieldError.PAN_REQUIRED
        } else if (!CardMath.luhnValid(digits)) {
            return VaultFieldError.PAN
        }
        if (!rememberCvv) return null
        val cvv = CardMath.normalizeDigits(cvvRaw)
        if (cvv.isEmpty()) {
            if (!hasStoredCvv) return VaultFieldError.CVV_REQUIRED
        } else if (cvv.length !in 3..4) {
            return VaultFieldError.CVV
        }
        return null
    }

    fun validateBankAccount(accountNumber: String, ibanRaw: String): VaultFieldError? {
        if (!isValidAccountNumber(accountNumber)) return VaultFieldError.ACCOUNT
        if (!IbanMath.isValidIranIban(ibanRaw)) return VaultFieldError.IBAN
        return null
    }
}
