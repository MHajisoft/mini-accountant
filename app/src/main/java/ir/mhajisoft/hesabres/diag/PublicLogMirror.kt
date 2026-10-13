package ir.mhajisoft.hesabres.diag

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.File
import java.io.OutputStream

/**
 * Copies session logs into Documents/Hesabres/logs so a normal file manager
 * can open them. API 29+ uses MediaStore (no storage permission). API 26–28
 * writes the public Documents directory when WRITE_EXTERNAL_STORAGE is granted.
 */
internal class PublicLogMirror(private val context: Context) {
    fun publish(file: File) {
        if (!file.exists()) return
        val bytes = file.readBytes()
        if (Build.VERSION.SDK_INT >= 29) {
            publishMediaStore(file.name, bytes)
        } else {
            publishLegacy(file.name, bytes)
        }
    }

    fun clear() {
        if (Build.VERSION.SDK_INT >= 29) {
            queryOurs().forEach { uri ->
                runCatching { context.contentResolver.delete(uri, null, null) }
            }
        } else {
            legacyDir()?.listFiles()?.forEach { runCatching { it.delete() } }
        }
    }

    fun canWritePublic(): Boolean {
        if (Build.VERSION.SDK_INT >= 29) return true
        return context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(29)
    private fun publishMediaStore(displayName: String, bytes: ByteArray) {
        val resolver = context.contentResolver
        val existing = findOwned(displayName)
        if (existing != null) {
            val wrote = runCatching {
                val stream = resolver.openOutputStream(existing, "wt") ?: return@runCatching false
                stream.use { it.write(bytes) }
                true
            }.getOrDefault(false)
            if (wrote) {
                markVisible(existing)
                return
            }
            runCatching { resolver.delete(existing, null, null) }
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_PATH)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection(), values) ?: return
        resolver.openOutputStream(uri)?.use { stream: OutputStream -> stream.write(bytes) }
        markVisible(uri)
    }

    @RequiresApi(29)
    private fun markVisible(uri: Uri) {
        val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
        context.contentResolver.update(uri, done, null, null)
    }

    @RequiresApi(29)
    private fun findOwned(displayName: String): Uri? {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.RELATIVE_PATH,
        )
        context.contentResolver.query(
            collection(),
            projection,
            "${MediaStore.MediaColumns.DISPLAY_NAME}=?",
            arrayOf(displayName),
            null,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val pathCol = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            while (cursor.moveToNext()) {
                val relative = if (pathCol >= 0) cursor.getString(pathCol).orEmpty() else ""
                if (!relative.replace('\\', '/').contains("Hesabres/logs")) continue
                return ContentUris.withAppendedId(collection(), cursor.getLong(idCol))
            }
        }
        return null
    }

    private fun publishLegacy(displayName: String, bytes: ByteArray) {
        if (!canWritePublic()) return
        val dir = legacyDir() ?: return
        if (!dir.exists() && !dir.mkdirs()) return
        File(dir, displayName).writeBytes(bytes)
    }

    private fun legacyDir(): File? {
        @Suppress("DEPRECATION")
        val documents = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        return File(documents, "Hesabres/logs")
    }

    private fun collection(): Uri {
        return if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Files.getContentUri("external")
        }
    }

    private fun queryOurs(): List<Uri> {
        if (Build.VERSION.SDK_INT < 29) return emptyList()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH,
        )
        val found = mutableListOf<Uri>()
        context.contentResolver.query(
            collection(),
            projection,
            "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?",
            arrayOf("hesabres-%.txt"),
            null,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val pathCol = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            val nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val relative = if (pathCol >= 0) cursor.getString(pathCol).orEmpty() else ""
                val name = if (nameCol >= 0) cursor.getString(nameCol).orEmpty() else ""
                if (!name.startsWith("hesabres-") || !name.endsWith(".txt")) continue
                if (!relative.replace('\\', '/').contains("Hesabres/logs")) continue
                found += ContentUris.withAppendedId(collection(), cursor.getLong(idCol))
            }
        }
        return found
    }

    companion object {
        const val RELATIVE_PATH = "Documents/Hesabres/logs/"
        const val FOLDER_LABEL = "Documents/Hesabres/logs"
    }
}
