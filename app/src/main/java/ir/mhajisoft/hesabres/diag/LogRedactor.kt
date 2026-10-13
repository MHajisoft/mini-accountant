package ir.mhajisoft.hesabres.diag

/**
 * Strips card numbers, IBANs, CVVs, emails, phones, and labeled names
 * before anything is written. Stack traces and our own labels stay.
 */
object LogRedactor {
    private const val DIGIT = "[0-9۰-۹٠-٩]"

    private val jsonName = Regex(
        """"(?i)(holderName|firstName|lastName|fullName|accountName|personName|displayName|holder|name|note)"\s*:\s*"(?:\\.|[^"\\]){0,120}"""",
    )
    private val labeledName = Regex(
        """(?i)\b(holderName|firstName|lastName|fullName|accountName|personName|displayName|holder|name|note)\b\s*[:=]\s*[^\n,;]{1,80}""",
    )
    private val persianName = Regex("""(نام خانوادگی|نام)\s*[:=]\s*[^\n,;]{1,80}""")
    private val email = Regex("""[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}""")
    private val iban = Regex("""(?i)(?<![A-Za-z0-9])IR(?:[\s-]*$DIGIT){24}(?!$DIGIT)""")
    private val sheba24 = Regex("""(?<!$DIGIT)(?:$DIGIT[\s-]?){23}$DIGIT(?!$DIGIT)""")
    private val card = Regex("""(?<!$DIGIT)(?:$DIGIT[\s-]?){12,18}$DIGIT(?!$DIGIT)""")
    private val cvv = Regex("""(?i)(cvv2?|cvc2?|رمز\s*دوم)\s*[:=]?\s*$DIGIT{3,4}""")
    private val phone = Regex(
        """(?<!$DIGIT)(?:\+98|0098|0)?[9۹٩]$DIGIT{9}(?!$DIGIT)""",
    )

    fun redact(text: String): String {
        if (text.isEmpty()) return text
        val trailingNewline = text.endsWith('\n')
        val redacted = text.lineSequence().joinToString("\n") { line ->
            val clipped = if (line.length > 4_000) line.take(4_000) + "…[truncated]" else line
            redactLine(clipped)
        }
        return if (trailingNewline && !redacted.endsWith('\n')) "$redacted\n" else redacted
    }

    private fun redactLine(line: String): String {
        var s = line
        s = jsonName.replace(s) { "\"${it.groupValues[1]}\":\"[name]\"" }
        s = labeledName.replace(s) { "${it.groupValues[1]}=[name]" }
        s = persianName.replace(s) { "${it.groupValues[1]}=[name]" }
        s = email.replace(s, "[email]")
        s = iban.replace(s, "IR[redacted]")
        s = sheba24.replace(s, "[iban]")
        s = card.replace(s, "[card]")
        s = cvv.replace(s) { "${it.groupValues[1]} [redacted]" }
        s = phone.replace(s, "[phone]")
        return s
    }
}
