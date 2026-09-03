package ir.mhajisoft.miniaccountant.data.backup

import android.content.Context
import android.net.Uri
import androidx.room3.useWriterConnection
import androidx.sqlite.execSQL
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.mhajisoft.miniaccountant.data.cloud.CloudBackupClient
import ir.mhajisoft.miniaccountant.data.cloud.CloudKind
import ir.mhajisoft.miniaccountant.data.local.dao.BackupMetadataDao
import ir.mhajisoft.miniaccountant.data.local.dao.BankCardDao
import ir.mhajisoft.miniaccountant.data.local.dao.SecretBlobDao
import ir.mhajisoft.miniaccountant.data.local.db.MiniAccountantDatabase
import ir.mhajisoft.miniaccountant.data.local.toEntity
import ir.mhajisoft.miniaccountant.domain.backup.BACKUP_FILE_EXTENSION
import ir.mhajisoft.miniaccountant.domain.backup.BACKUP_SCHEMA_VERSION
import ir.mhajisoft.miniaccountant.domain.backup.BackupCard
import ir.mhajisoft.miniaccountant.domain.backup.BackupManifest
import ir.mhajisoft.miniaccountant.domain.backup.BackupSanitizer
import ir.mhajisoft.miniaccountant.domain.backup.PassphraseKdf
import ir.mhajisoft.miniaccountant.domain.model.BackupMetadata
import ir.mhajisoft.miniaccountant.domain.model.BackupProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: MiniAccountantDatabase,
    private val metadataDao: BackupMetadataDao,
    private val cards: BankCardDao,
    private val secrets: SecretBlobDao,
    private val drive: ir.mhajisoft.miniaccountant.data.cloud.DriveBackupClient,
    private val oneDrive: ir.mhajisoft.miniaccountant.data.cloud.OneDriveBackupClient,
) {
    private val json = Json { encodeDefaults = true }

    val metadata: Flow<List<BackupMetadata>> = metadataDao.observeAll().map { list ->
        list.map { it.toDomainCompat() }
    }

    fun driveAvailability() = drive.availability()
    fun oneDriveAvailability() = oneDrive.availability()

    suspend fun createEncryptedBackup(passphrase: String): ByteArray = withContext(Dispatchers.IO) {
        checkpoint()
        val cardsSanitized = BackupSanitizer.sanitizeCards(
            cards.getAll().map {
                BackupCard(
                    id = it.id,
                    accountId = it.accountId,
                    last4 = it.last4,
                    bin6 = it.bin6,
                    bankCode = it.bankCode,
                    expiryMonth = it.expiryMonth,
                    expiryYear = it.expiryYear,
                    holderName = it.holderName,
                    panCipherId = it.panCipherId,
                    cvvCipherId = it.cvvCipherId,
                    rememberCvv = it.rememberCvv,
                )
            },
        )
        val sqlite = copySanitizedSqlite()
        val sqliteGz = gzip(sqlite)
        val manifest = BackupSanitizer.buildManifest(
            createdAtEpoch = System.currentTimeMillis(),
            rowCount = sqlite.size,
        )
        val zip = buildZip(manifest, sqliteGz, cardsSanitized)
        encrypt(zip, passphrase)
    }

    suspend fun restoreEncrypted(bytes: ByteArray, passphrase: String) = withContext(Dispatchers.IO) {
        val zip = decrypt(bytes, passphrase)
        val files = unzip(zip)
        val manifestBytes = files["manifest.json"] ?: error("manifest")
        val manifest = json.decodeFromString(BackupManifest.serializer(), manifestBytes.decodeToString())
        check(!manifest.includesSecrets)
        val sqliteGz = files["ledger.sqlite.gz"] ?: error("sqlite")
        val sqlite = gunzip(sqliteGz)
        val dest = context.getDatabasePath(MiniAccountantDatabase.FILE_NAME)
        db.close()
        dest.delete()
        FileSibling.deleteSidecars(dest)
        dest.parentFile?.mkdirs()
        dest.writeBytes(sqlite)
    }

    suspend fun writeToSaf(uri: Uri, bytes: ByteArray) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
            ?: error("cannot write")
    }

    suspend fun readFromSaf(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("cannot read")
    }

    suspend fun recordLocal(uri: String, bytes: ByteArray) {
        val checksum = sha256(bytes)
        metadataDao.upsert(
            BackupMetadata(
                id = UUID.randomUUID().toString(),
                provider = BackupProvider.LOCAL_SAF,
                remoteIdOrUri = uri,
                fileName = "mini-accountant.$BACKUP_FILE_EXTENSION",
                checksum = checksum,
                schemaVersion = BACKUP_SCHEMA_VERSION,
                includesSecrets = false,
                createdAt = System.currentTimeMillis(),
            ).toEntity(),
        )
    }

    suspend fun uploadCloud(kind: CloudKind, bytes: ByteArray): Result<Unit> {
        val client: CloudBackupClient = if (kind == CloudKind.DRIVE) drive else oneDrive
        val avail = client.availability()
        if (!avail.available) return Result.failure(IllegalStateException(avail.reasonFa))
        val name = "mini-accountant-${System.currentTimeMillis()}.$BACKUP_FILE_EXTENSION"
        val uploaded = client.upload(name, bytes)
        return uploaded.map { snap ->
            val provider = if (kind == CloudKind.DRIVE) BackupProvider.DRIVE_APPDATA else BackupProvider.ONEDRIVE_APPROOT
            metadataDao.upsert(
                BackupMetadata(
                    id = UUID.randomUUID().toString(),
                    provider = provider,
                    remoteIdOrUri = snap.remoteId,
                    fileName = snap.fileName,
                    checksum = snap.checksum,
                    schemaVersion = BACKUP_SCHEMA_VERSION,
                    includesSecrets = false,
                    createdAt = System.currentTimeMillis(),
                ).toEntity(),
            )
            pruneCloud(provider.name, keep = 3)
        }
    }

    private suspend fun pruneCloud(provider: String, keep: Int) {
        val all = metadataDao.forProvider(provider)
        all.drop(keep).forEach { metadataDao.delete(it.id) }
    }

    private suspend fun checkpoint() {
        db.useWriterConnection { conn ->
            conn.usePrepared("PRAGMA wal_checkpoint(FULL)") { stmt ->
                stmt.step()
                Unit
            }
        }
    }

    private suspend fun copySanitizedSqlite(): ByteArray {
        checkpoint()
        val src = context.getDatabasePath(MiniAccountantDatabase.FILE_NAME)
        if (!src.exists()) return ByteArray(0)
        val tmp = java.io.File(context.cacheDir, "backup-sanitize.db")
        src.copyTo(tmp, overwrite = true)
        java.io.File(src.path + "-wal").takeIf { it.exists() }?.copyTo(java.io.File(tmp.path + "-wal"), true)
        java.io.File(src.path + "-shm").takeIf { it.exists() }?.copyTo(java.io.File(tmp.path + "-shm"), true)
        val driver = androidx.sqlite.driver.bundled.BundledSQLiteDriver()
        val conn = driver.open(tmp.absolutePath)
        conn.execSQL("UPDATE bank_cards SET cvvCipherId=NULL, rememberCvv=0, panCipherId=NULL")
        conn.execSQL("DELETE FROM secret_blobs")
        conn.execSQL("PRAGMA wal_checkpoint(FULL)")
        conn.close()
        val bytes = tmp.readBytes()
        tmp.delete()
        java.io.File(tmp.path + "-wal").delete()
        java.io.File(tmp.path + "-shm").delete()
        return bytes
    }

    private fun buildZip(
        manifest: BackupManifest,
        sqliteGz: ByteArray,
        cards: List<BackupCard>,
    ): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(json.encodeToString(manifest).encodeToByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("ledger.sqlite.gz"))
            zip.write(sqliteGz)
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("cards.json"))
            zip.write(json.encodeToString(cards).encodeToByteArray())
            zip.closeEntry()
        }
        val bytes = bos.toByteArray()
        BackupSanitizer.assertSafe(manifest, bytes, cards)
        return bytes
    }

    private fun encrypt(plainZip: ByteArray, passphrase: String): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(PassphraseKdf.SALT_BYTES).also { random.nextBytes(it) }
        val iv = ByteArray(PassphraseKdf.IV_BYTES).also { random.nextBytes(it) }
        val key = derive(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(plainZip)
        return "PFBK".toByteArray() + byteArrayOf(1) + salt + iv + ciphertext
    }

    private fun decrypt(blob: ByteArray, passphrase: String): ByteArray {
        require(blob.size > 4 + 1 + PassphraseKdf.SALT_BYTES + PassphraseKdf.IV_BYTES)
        require(blob.copyOfRange(0, 4).decodeToString() == "PFBK")
        var offset = 5
        val salt = blob.copyOfRange(offset, offset + PassphraseKdf.SALT_BYTES)
        offset += PassphraseKdf.SALT_BYTES
        val iv = blob.copyOfRange(offset, offset + PassphraseKdf.IV_BYTES)
        offset += PassphraseKdf.IV_BYTES
        val ciphertext = blob.copyOfRange(offset, blob.size)
        val key = derive(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun derive(passphrase: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(
            passphrase.toCharArray(),
            salt,
            PassphraseKdf.PBKDF2_ITERATIONS,
            PassphraseKdf.KEY_BYTES * 8,
        )
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun gzip(data: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(data) }
        return bos.toByteArray()
    }

    private fun gunzip(data: ByteArray): ByteArray =
        GZIPInputStream(ByteArrayInputStream(data)).use { it.readBytes() }

    private fun unzip(data: ByteArray): Map<String, ByteArray> {
        val out = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(data)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                out[entry.name] = zip.readBytes()
            }
        }
        return out
    }

    private fun sha256(data: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256").digest(data)
        return md.joinToString("") { "%02x".format(it) }
    }

    private fun ir.mhajisoft.miniaccountant.data.local.entity.BackupMetadataEntity.toDomainCompat() =
        ir.mhajisoft.miniaccountant.domain.model.BackupMetadata(
            id, ir.mhajisoft.miniaccountant.domain.model.BackupProvider.valueOf(provider),
            remoteIdOrUri, fileName, checksum, schemaVersion, includesSecrets, createdAt,
        )
}

private object FileSibling {
    fun deleteSidecars(dbFile: java.io.File) {
        java.io.File(dbFile.path + "-wal").delete()
        java.io.File(dbFile.path + "-shm").delete()
    }
}
