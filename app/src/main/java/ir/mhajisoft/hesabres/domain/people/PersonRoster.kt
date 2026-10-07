package ir.mhajisoft.hesabres.domain.people

import ir.mhajisoft.hesabres.domain.model.BankAccount
import ir.mhajisoft.hesabres.domain.model.BankCard
import ir.mhajisoft.hesabres.domain.model.Person
import ir.mhajisoft.hesabres.domain.money.PersianDigits

enum class PersonListFilter {
    ALL,
    DEBTOR,
    CREDITOR,
    SETTLED,
}

/**
 * People-list search and the debtor/creditor split.
 * Positive person-wallet balance means they owe you.
 */
object PersonRoster {
    fun roleOf(balanceSigned: Long): PersonListFilter = when {
        balanceSigned > 0L -> PersonListFilter.DEBTOR
        balanceSigned < 0L -> PersonListFilter.CREDITOR
        else -> PersonListFilter.SETTLED
    }

    /** Phone first, then email. Null when the person has neither. */
    fun contactLine(person: Person): String? {
        val phone = person.phone?.trim().orEmpty()
        if (phone.isNotEmpty()) return phone
        val email = person.email?.trim().orEmpty()
        return email.ifEmpty { null }
    }

    fun matches(person: Person, rawQuery: String): Boolean {
        val query = rawQuery.trim()
        if (query.isEmpty()) return true
        val asciiQuery = PersianDigits.toAscii(query)
        val fields = buildList {
            add(person.displayName)
            add(person.name)
            add(person.firstName)
            add(person.lastName)
            person.phone?.let { add(it) }
            person.email?.let { add(it) }
            person.note?.let { add(it) }
            person.socialLinks.forEach { link ->
                add(link.label)
                add(link.value)
            }
        }
        return fields.any { field ->
            field.contains(query, ignoreCase = true) ||
                PersianDigits.toAscii(field).contains(asciiQuery, ignoreCase = true)
        }
    }

    fun visible(
        people: List<Person>,
        balanceOf: (Person) -> Long,
        query: String,
        filter: PersonListFilter,
    ): List<Person> = people.filter { person ->
        val roleOk = filter == PersonListFilter.ALL || roleOf(balanceOf(person)) == filter
        roleOk && matches(person, query)
    }
}

/** Cards and IBANs that belong on this person's profile. */
object PersonVaultLinks {
    fun cardLinked(personId: String, accountId: String, card: BankCard): Boolean =
        card.personId == personId || (card.personId.isNullOrBlank() && card.accountId == accountId)

    fun accountLinked(personId: String, accountId: String, account: BankAccount): Boolean =
        account.personId == personId || (account.personId.isNullOrBlank() && account.accountId == accountId)
}
