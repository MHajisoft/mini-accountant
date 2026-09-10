package ir.mhajisoft.hesabres.domain.people

import ir.mhajisoft.hesabres.domain.model.SocialLink
import java.util.UUID

/**
 * Fixed Instagram/Telegram/WhatsApp columns were too rigid.
 * Users can add any number of labeled contact/social rows, including custom.
 */
object SocialLinkCatalog {
    val suggestions = listOf(
        "اینستاگرام",
        "تلگرام",
        "واتساپ",
        "ایتا",
        "بله",
        "روبیکا",
        "سروش",
        "لینکدین",
        "ایکس",
        "یوتیوب",
        "وب‌سایت",
        "سایر",
    )

    fun fromLegacyColumns(
        personId: String,
        instagram: String?,
        telegram: String?,
        whatsapp: String?,
    ): List<SocialLink> {
        val out = mutableListOf<SocialLink>()
        fun add(label: String, raw: String?, order: Int) {
            val value = raw?.trim().orEmpty()
            if (value.isNotEmpty()) {
                out += SocialLink(
                    id = UUID.randomUUID().toString(),
                    personId = personId,
                    label = label,
                    value = value,
                    sortOrder = order,
                )
            }
        }
        add("اینستاگرام", instagram, 0)
        add("تلگرام", telegram, 1)
        add("واتساپ", whatsapp, 2)
        return out
    }

    fun mergeVisible(personId: String, stored: List<SocialLink>, instagram: String?, telegram: String?, whatsapp: String?): List<SocialLink> {
        if (stored.isNotEmpty()) return stored.sortedBy { it.sortOrder }
        return fromLegacyColumns(personId, instagram, telegram, whatsapp)
    }

    fun sanitized(links: List<SocialLink>, personId: String): List<SocialLink> =
        links
            .mapIndexed { index, link ->
                link.copy(
                    personId = personId,
                    label = link.label.trim().ifBlank { "سایر" },
                    value = link.value.trim(),
                    sortOrder = index,
                    id = link.id.ifBlank { UUID.randomUUID().toString() },
                )
            }
            .filter { it.value.isNotBlank() }
}
