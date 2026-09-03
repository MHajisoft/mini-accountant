package ir.mhajisoft.miniaccountant.domain.money

/**
 * All money is stored as Long rials. Toman is a display-only division by 10.
 * Never use Double/Float for money.
 */
object Money {
    const val RIALS_PER_TOMAN = 10L

    fun toDisplayUnit(rials: Long, toman: Boolean): Long =
        if (toman) rials / RIALS_PER_TOMAN else rials

    fun fromDisplayUnit(display: Long, toman: Boolean): Long =
        if (toman) display * RIALS_PER_TOMAN else display

    /** Parses a user-typed amount that may contain Persian digits, separators, or spaces. */
    fun parseDisplayAmount(raw: String, toman: Boolean): Long? {
        val ascii = PersianDigits.toAscii(raw)
            .replace(",", "")
            .replace("٬", "")
            .replace(" ", "")
            .replace("\u200c", "")
            .trim()
        if (ascii.isEmpty()) return null
        val value = ascii.toLongOrNull() ?: return null
        if (value < 0L) return null
        return fromDisplayUnit(value, toman)
    }

    fun signed(amount: Long, inbound: Boolean): Long {
        require(amount >= 0L)
        return if (inbound) amount else -amount
    }

    fun runningBalance(openingSigned: Long, liveSignedDeltas: Iterable<Long>): Long {
        var total = openingSigned
        for (delta in liveSignedDeltas) {
            total += delta
        }
        return total
    }

    fun sum(values: Iterable<Long>): Long {
        var total = 0L
        for (v in values) total += v
        return total
    }
}

object PersianDigits {
    private val persian = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    private val arabicIndic = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    fun toPersian(input: String): String {
        val out = StringBuilder(input.length)
        for (ch in input) {
            out.append(if (ch in '0'..'9') persian[ch - '0'] else ch)
        }
        return out.toString()
    }

    fun toAscii(input: String): String {
        val out = StringBuilder(input.length)
        for (ch in input) {
            val p = persian.indexOf(ch)
            val a = arabicIndic.indexOf(ch)
            when {
                p >= 0 -> out.append('0' + p)
                a >= 0 -> out.append('0' + a)
                else -> out.append(ch)
            }
        }
        return out.toString()
    }

    fun formatGrouped(rialsOrToman: Long): String {
        val negative = rialsOrToman < 0
        val abs = kotlin.math.abs(rialsOrToman).toString()
        val grouped = abs.reversed().chunked(3).joinToString("٬").reversed()
        val withSign = if (negative) "−$grouped" else grouped
        return toPersian(withSign)
    }
}
