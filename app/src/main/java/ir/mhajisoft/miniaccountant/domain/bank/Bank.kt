package ir.mhajisoft.miniaccountant.domain.bank

import ir.mhajisoft.miniaccountant.domain.money.PersianDigits

data class BankInfo(
    val id: String,
    val nameFa: String,
    val nameEn: String,
    val shebaCode: String,
    val bins: List<String>,
    val logoDrawable: String,
    val mergedInto: String? = null,
    val formerNameFa: String? = null,
)

data class BankDirectory(
    val version: Int,
    val sources: List<String>,
    val banks: List<BankInfo>,
) {
    private val binIndex: Map<String, BankInfo> = buildMap {
        for (bank in banks) {
            for (bin in bank.bins) put(bin, bank)
        }
    }
    private val shebaIndex: Map<String, BankInfo> = banks
        .filter { it.mergedInto == null }
        .associateBy { it.shebaCode }

    fun findByBin(bin6: String): BankInfo? = binIndex[bin6]

    fun findById(id: String): BankInfo? = banks.firstOrNull { it.id == id || it.logoDrawable == id }

    fun logoOf(bin6: String, bankCode: String): String =
        findByBin(bin6)?.logoDrawable ?: findById(bankCode)?.logoDrawable ?: "bank_unknown"

    fun findBySheba(code3: String): BankInfo? = shebaIndex[code3]

    fun resolvePan(panDigits: String): BankMatch {
        val digits = CardMath.normalizeDigits(panDigits)
        if (digits.length >= 6) {
            val hit = findByBin(digits.take(6))
            if (hit != null) return BankMatch.Known(hit)
        }
        return BankMatch.Unknown
    }

    fun resolveIban(ibanRaw: String): BankMatch {
        val compact = IbanMath.normalize(ibanRaw)
        if (compact.length < 7) return BankMatch.Unknown
        val sheba = compact.substring(4, 7)
        val hit = findBySheba(sheba)
        return if (hit != null) BankMatch.Known(hit) else BankMatch.Unknown
    }
}

sealed class BankMatch {
    data class Known(val bank: BankInfo) : BankMatch()
    data object Unknown : BankMatch()
}

object CardMath {
    fun normalizeDigits(raw: String): String =
        PersianDigits.toAscii(raw).filter { it.isDigit() }

    fun luhnValid(panRaw: String): Boolean {
        val digits = normalizeDigits(panRaw)
        if (digits.length !in 16..19) return false
        var sum = 0
        var alternate = false
        for (i in digits.lastIndex downTo 0) {
            var n = digits[i] - '0'
            if (alternate) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alternate = !alternate
        }
        return sum % 10 == 0
    }

    fun last4(panRaw: String): String = normalizeDigits(panRaw).takeLast(4)

    fun bin6(panRaw: String): String = normalizeDigits(panRaw).take(6)

    fun maskPan(panRaw: String): String {
        val digits = normalizeDigits(panRaw)
        if (digits.length < 4) return "••••"
        val last = digits.takeLast(4)
        return "•••• •••• •••• $last"
    }
}

object IbanMath {
    fun normalize(raw: String): String {
        val ascii = PersianDigits.toAscii(raw)
            .replace(" ", "")
            .replace("-", "")
            .replace("\u200c", "")
            .uppercase()
        return ascii
    }

    /**
     * Iranian IBAN: IR + 24 digits, ISO 13616 mod-97 == 1.
     * Bank code is digits 5–7 of the compact form (after IR + check digits).
     */
    fun isValidIranIban(raw: String): Boolean {
        val compact = normalize(raw)
        if (!compact.startsWith("IR")) return false
        if (compact.length != 26) return false
        if (!compact.substring(2).all { it.isDigit() }) return false
        return iso13616Mod97(compact) == 1
    }

    fun shebaBankCode(raw: String): String? {
        val compact = normalize(raw)
        if (compact.length < 7) return null
        return compact.substring(4, 7)
    }

    fun formatGrouped(raw: String): String {
        val compact = normalize(raw)
        if (compact.length < 2) return compact
        val body = compact.drop(2).chunked(4).joinToString(" ")
        return compact.take(2) + body.let { if (it.isEmpty()) it else " $it" }
    }

    fun iso13616Mod97(iban: String): Int {
        val rearranged = iban.substring(4) + iban.substring(0, 4)
        val numeric = buildString {
            for (ch in rearranged) {
                if (ch.isDigit()) append(ch)
                else append(ch - 'A' + 10)
            }
        }
        var remainder = 0
        for (ch in numeric) {
            remainder = (remainder * 10 + (ch - '0')) % 97
        }
        return remainder
    }
}
