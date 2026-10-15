package ir.mhajisoft.hesabres.domain.backup

object BackupReminder {
    const val OVERDUE_DAYS = 3
    private const val DAY_MS = 86_400_000L

    fun isOverdue(lastBackupAt: Long?, now: Long, overdueDays: Int = OVERDUE_DAYS): Boolean {
        if (lastBackupAt == null || lastBackupAt <= 0L) return true
        val elapsed = now - lastBackupAt
        return elapsed >= overdueDays * DAY_MS
    }
}
