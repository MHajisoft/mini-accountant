package ir.mhajisoft.hesabres.domain.people

import ir.mhajisoft.hesabres.domain.money.PersianDigits

enum class PersonFieldError {
    NAME,
    PHONE,
    EMAIL,
}

object PersonProfileRules {
    fun validate(first: String, last: String, phone: String, email: String): PersonFieldError? {
        if (first.isBlank() && last.isBlank()) return PersonFieldError.NAME
        if (phone.isNotBlank()) {
            val digits = PersianDigits.toAscii(phone).filter { it.isDigit() }
            if (digits.length !in 8..15) return PersonFieldError.PHONE
        }
        if (email.isNotBlank() && !ContactLinks.isEmail(email)) return PersonFieldError.EMAIL
        return null
    }
}
