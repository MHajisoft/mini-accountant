package ir.mhajisoft.miniaccountant.domain.backup

import kotlinx.serialization.Serializable
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

const val BACKUP_SCHEMA_VERSION = 1
const val BACKUP_FILE_EXTENSION = "pfbak"

@Serializable
data class BackupManifest(
    val schemaVersion: Int = BACKUP_SCHEMA_VERSION,
    val includesSecrets: Boolean = false,
    val createdAtEpoch: Long,
    val appId: String = "ir.mhajisoft.miniaccountant",
    val rowCount: Int = 0,
)

@Serializable
data class BackupCard(
    val id: String,
    val accountId: String,
    val last4: String,
    val bin6: String,
    val bankCode: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val holderName: String? = null,
    val panCipherId: String? = null,
    val cvvCipherId: String? = null,
    val rememberCvv: Boolean = false,
)

@Serializable
data class BackupPayload(
    val manifest: BackupManifest,
    val cards: List<BackupCard> = emptyList(),
    val jsonLedger: String? = null,
)

object BackupSanitizer {
    /**
     * CVV is never persisted into a backup. PAN ciphertext is also stripped
     * (`includesSecrets = false`). last4/bin6 remain so cards can be restored
     * as masked records.
     */
    fun sanitizeCards(cards: List<BackupCard>): List<BackupCard> =
        cards.map { card ->
            card.copy(
                panCipherId = null,
                cvvCipherId = null,
                rememberCvv = false,
            )
        }

    fun buildManifest(createdAtEpoch: Long, rowCount: Int): BackupManifest =
        BackupManifest(
            schemaVersion = BACKUP_SCHEMA_VERSION,
            includesSecrets = false,
            createdAtEpoch = createdAtEpoch,
            rowCount = rowCount,
        )

    fun unzippedText(zipBytes: ByteArray): String {
        val parts = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                parts += zip.readBytes().decodeToString(throwOnInvalidSequence = false)
            }
        }
        return parts.joinToString("\n")
    }

    fun containsCvvMaterial(bytes: ByteArray): Boolean {
        val haystack = runCatching { unzippedText(bytes) }.getOrElse {
            bytes.decodeToString(throwOnInvalidSequence = false)
        }
        val lowered = haystack.lowercase().replace("_", "")
        if ("cvv-super-secret" in lowered || "cvvcipherid\":\"" in lowered) return true
        val assigned = Regex("\"cvvCipherId\"\\s*:\\s*\"[^\"]+\"")
        return assigned.containsMatchIn(haystack)
    }

    fun assertSafe(manifest: BackupManifest, zipBytes: ByteArray, cards: List<BackupCard>) {
        check(!manifest.includesSecrets) { "Backup must set includesSecrets=false" }
        check(cards.all { it.cvvCipherId == null && !it.rememberCvv && it.panCipherId == null }) {
            "Sanitized cards must not carry cipher ids"
        }
        val haystack = unzippedText(zipBytes)
        check("\"includesSecrets\":false" in haystack.replace(" ", "")) {
            "manifest must include includesSecrets=false"
        }
        check(!containsCvvMaterial(zipBytes)) { "CVV ciphertext must not appear in backup zip" }
        check("cvv-super-secret" !in haystack)
    }
}

object PassphraseKdf {
    const val PBKDF2_ITERATIONS = 120_000
    const val SALT_BYTES = 16
    const val KEY_BYTES = 32
    const val IV_BYTES = 12
}
