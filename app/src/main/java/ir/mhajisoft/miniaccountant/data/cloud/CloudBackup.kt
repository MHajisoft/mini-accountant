package ir.mhajisoft.miniaccountant.data.cloud

import android.net.Uri

enum class CloudKind { DRIVE, ONEDRIVE }

data class CloudAvailability(
    val kind: CloudKind,
    val available: Boolean,
    val reasonFa: String,
)

data class CloudSnapshot(
    val remoteId: String,
    val fileName: String,
    val checksum: String,
    val bytes: Long,
)

interface CloudBackupClient {
    val kind: CloudKind
    fun availability(): CloudAvailability
    suspend fun upload(fileName: String, bytes: ByteArray): Result<CloudSnapshot>
    suspend fun download(remoteId: String): Result<ByteArray>
    suspend fun listRecent(): Result<List<CloudSnapshot>>
    suspend fun delete(remoteId: String): Result<Unit>
}

interface SafBackupWriter {
    suspend fun write(uri: Uri, bytes: ByteArray)
    suspend fun read(uri: Uri): ByteArray
}
