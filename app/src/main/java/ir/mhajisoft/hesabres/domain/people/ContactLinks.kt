package ir.mhajisoft.hesabres.domain.people

import ir.mhajisoft.hesabres.domain.money.PersianDigits

/**
 * Turns a free-form contact value into a dial, mail, or https target.
 * Unknown labels stay copy-only so custom rows are never forced into the wrong app.
 */
object ContactLinks {
    fun dialUri(raw: String): String? {
        val ascii = PersianDigits.toAscii(raw).filter { it.isDigit() || it == '+' }
        val digits = ascii.filter { it.isDigit() }
        if (digits.length !in 3..15) return null
        return "tel:$ascii"
    }

    fun mailtoUri(raw: String): String? {
        val email = raw.trim()
        if (!isEmail(email)) return null
        return "mailto:$email"
    }

    fun isEmail(raw: String): Boolean {
        val email = raw.trim()
        if (email.isEmpty() || email.any { it.isWhitespace() }) return false
        val at = email.indexOf('@')
        if (at <= 0 || at != email.lastIndexOf('@') || at == email.lastIndex - 1) return false
        val domain = email.substring(at + 1)
        return domain.contains('.') && !domain.startsWith('.') && !domain.endsWith('.')
    }

    fun openUri(label: String, value: String): String? {
        val raw = value.trim()
        if (raw.isEmpty()) return null
        val lower = raw.lowercase()
        if (lower.startsWith("https://") || lower.startsWith("http://")) return raw
        val key = label.trim()
        val handle = handleOf(raw) ?: return whatsappIfPhone(key, raw)
        return when {
            key.contains("اینستا") || key.equals("instagram", ignoreCase = true) ->
                "https://instagram.com/$handle"
            key.contains("تلگرام") || key.equals("telegram", ignoreCase = true) ->
                "https://t.me/$handle"
            key.contains("واتس") || key.equals("whatsapp", ignoreCase = true) ->
                whatsappTarget(raw)
            key.contains("ایتا") || key.equals("eitaa", ignoreCase = true) ->
                "https://eitaa.com/$handle"
            key.contains("بله") || key.equals("bale", ignoreCase = true) ->
                "https://ble.ir/$handle"
            key.contains("روبیکا") || key.equals("rubika", ignoreCase = true) ->
                "https://rubika.ir/$handle"
            key.contains("سروش") ->
                "https://splus.ir/$handle"
            key.contains("لینکدین") || key.equals("linkedin", ignoreCase = true) ->
                "https://www.linkedin.com/in/$handle"
            key.equals("ایکس", ignoreCase = true) ||
                key.equals("x", ignoreCase = true) ||
                key.equals("twitter", ignoreCase = true) ->
                "https://x.com/$handle"
            key.contains("یوتیوب") || key.equals("youtube", ignoreCase = true) ->
                "https://www.youtube.com/@$handle"
            key.contains("وب") || key.contains("سایت") || key.equals("website", ignoreCase = true) ->
                if (handle.contains('.')) "https://$handle" else null
            else -> null
        }
    }

    private fun whatsappIfPhone(label: String, raw: String): String? {
        val whats = label.contains("واتس") || label.equals("whatsapp", ignoreCase = true)
        if (!whats) return null
        return whatsappTarget(raw)
    }

    private fun whatsappTarget(raw: String): String? {
        val digits = PersianDigits.toAscii(raw).filter { it.isDigit() }
        if (digits.length >= 10) {
            val intl = when {
                digits.startsWith("98") -> digits
                digits.startsWith("0") -> "98" + digits.drop(1)
                else -> digits
            }
            return "https://wa.me/$intl"
        }
        val handle = handleOf(raw) ?: return null
        return "https://wa.me/$handle"
    }

    private fun handleOf(raw: String): String? {
        val handle = raw.trim().removePrefix("@").trim().trim('/')
        if (handle.isEmpty() || handle.any { it.isWhitespace() } || handle.contains("://")) return null
        return handle
    }
}
