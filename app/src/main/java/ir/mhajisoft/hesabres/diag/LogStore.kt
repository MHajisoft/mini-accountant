package ir.mhajisoft.hesabres.diag

import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * One plain-text file per process session. Rolls the active file after
 * [rotateFileBytes] and deletes the oldest files once the directory passes
 * [maxTotalBytes]. The newest file is never deleted; if it alone is over the
 * cap, only its tail is kept.
 */
class LogStore(
    val directory: File,
    private val maxTotalBytes: Long = MAX_TOTAL_BYTES,
    private val rotateFileBytes: Long = ROTATE_FILE_BYTES,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    private val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(zone)
    private var active: File? = null

    @Synchronized
    fun append(text: String): File {
        if (text.isEmpty()) return active ?: File(directory, "hesabres-pending.txt")
        directory.mkdirs()
        var file = active?.takeIf { it.exists() } ?: newFile()
        if (file.length() >= rotateFileBytes) file = newFile()
        file.appendText(text)
        active = file
        trimKeeping(file)
        return file
    }

    @Synchronized
    fun activeFile(): File? = active?.takeIf { it.exists() }

    @Synchronized
    fun files(): List<File> = listFiles()

    @Synchronized
    fun clear() {
        listFiles().forEach { it.delete() }
        active = null
    }

    private fun newFile(): File {
        val base = stamp.format(Instant.ofEpochMilli(clock()))
        var file = File(directory, "hesabres-$base.txt")
        var n = 2
        while (file.exists()) {
            file = File(directory, "hesabres-$base-$n.txt")
            n++
        }
        active = file
        return file
    }

    private fun trimKeeping(keep: File) {
        var files = listFiles()
        while (files.sumOf { it.length() } > maxTotalBytes && files.size > 1) {
            val oldest = files.firstOrNull { it.absolutePath != keep.absolutePath } ?: break
            oldest.delete()
            files = listFiles()
        }
        if (!keep.exists()) return
        if (keep.length() <= maxTotalBytes) return
        val bytes = keep.readBytes()
        val tail = (maxTotalBytes * 3 / 4).toInt().coerceAtMost(bytes.size)
        var start = (bytes.size - tail).coerceAtLeast(0)
        while (start < bytes.size && bytes[start] != '\n'.code.toByte()) start++
        if (start >= bytes.size) start = (bytes.size - tail).coerceAtLeast(0)
        val trimmed = bytes.copyOfRange(start.coerceAtMost(bytes.lastIndex), bytes.size)
        keep.writeBytes(trimmed)
    }

    private fun listFiles(): List<File> {
        return directory.listFiles()
            ?.filter { it.isFile && it.name.startsWith("hesabres-") && it.name.endsWith(".txt") }
            ?.sortedBy { it.name }
            .orEmpty()
    }

    companion object {
        const val MAX_TOTAL_BYTES = 5L * 1024 * 1024
        const val ROTATE_FILE_BYTES = 2L * 1024 * 1024
    }
}
