package ir.mhajisoft.miniaccountant.data.archive

import android.content.Context
import androidx.room3.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.mhajisoft.miniaccountant.data.local.dao.ArchiveDao
import ir.mhajisoft.miniaccountant.data.local.dao.FiscalYearDao
import ir.mhajisoft.miniaccountant.data.local.dao.SnapshotDao
import ir.mhajisoft.miniaccountant.data.local.dao.TransactionDao
import ir.mhajisoft.miniaccountant.data.local.dao.TransferDao
import ir.mhajisoft.miniaccountant.data.local.db.MiniAccountantDatabase
import ir.mhajisoft.miniaccountant.data.local.entity.ArchiveEntity
import ir.mhajisoft.miniaccountant.data.local.entity.BalanceSnapshotEntity
import ir.mhajisoft.miniaccountant.data.local.toDomain
import ir.mhajisoft.miniaccountant.data.repository.LedgerRepository
import ir.mhajisoft.miniaccountant.domain.model.ArchiveRecord
import ir.mhajisoft.miniaccountant.domain.model.ArchiveStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArchiveRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: MiniAccountantDatabase,
    private val ledger: LedgerRepository,
    private val archives: ArchiveDao,
    private val snapshots: SnapshotDao,
    private val fiscalYears: FiscalYearDao,
    private val txns: TransactionDao,
    private val transfers: TransferDao,
) {
    val records: Flow<List<ArchiveRecord>> = archives.observeAll().map { list ->
        list.map { it.toDomain() }
    }

    private fun archivesDir(): File = File(context.filesDir, "archives").apply { mkdirs() }

    suspend fun archiveClosedYear(fiscalYearId: String): ArchiveRecord = withContext(Dispatchers.IO) {
        val fy = fiscalYears.get(fiscalYearId) ?: error("missing fy")
        require(!fy.isCurrent) { "سال جاری را ابتدا ببندید" }
        val now = System.currentTimeMillis()
        if (snapshots.forYear(fiscalYearId).isEmpty()) {
            val balances = ledger.balancesForYear(fiscalYearId)
            snapshots.upsertAll(
                balances.map { ab ->
                    BalanceSnapshotEntity(
                        id = UUID.randomUUID().toString(),
                        fiscalYearId = fiscalYearId,
                        accountId = ab.account.id,
                        amountSigned = ab.balanceSigned,
                        capturedAt = now,
                    )
                },
            )
        }
        checkpoint()
        val src = context.getDatabasePath(MiniAccountantDatabase.FILE_NAME)
        val raw = File(context.cacheDir, "fy-${fy.label}.db")
        src.copyTo(raw, overwrite = true)
        val driver = BundledSQLiteDriver()
        val conn = driver.open(raw.absolutePath)
        conn.prepare("DELETE FROM transactions WHERE fiscalYearId != ?").use { stmt ->
            stmt.bindText(1, fiscalYearId)
            stmt.step()
        }
        conn.prepare("DELETE FROM transfers WHERE fiscalYearId != ?").use { stmt ->
            stmt.bindText(1, fiscalYearId)
            stmt.step()
        }
        conn.execSQL("DELETE FROM secret_blobs")
        conn.execSQL("UPDATE bank_cards SET cvvCipherId=NULL, rememberCvv=0, panCipherId=NULL")
        conn.execSQL("VACUUM")
        conn.close()
        val gzName = "fy-${fy.label}.db.gz"
        val gz = File(archivesDir(), gzName)
        GZIPOutputStream(FileOutputStream(gz)).use { out ->
            FileInputStream(raw).use { it.copyTo(out) }
        }
        raw.delete()
        val rowCount = txns.countForYear(fiscalYearId)
        txns.deleteForYear(fiscalYearId)
        transfers.deleteForYear(fiscalYearId)
        fiscalYears.close(fiscalYearId, now)
        vacuumLive()
        val record = ArchiveRecord(
            id = UUID.randomUUID().toString(),
            fiscalYearId = fiscalYearId,
            fileName = gzName,
            byteSize = gz.length(),
            sha256 = sha256(gz.readBytes()),
            rowCount = rowCount,
            status = ArchiveStatus.READY,
        )
        archives.upsert(
            ArchiveEntity(
                record.id, record.fiscalYearId, record.fileName,
                record.byteSize, record.sha256, record.rowCount, record.status.name,
            ),
        )
        record
    }

    suspend fun openArchiveFile(fileName: String): File = withContext(Dispatchers.IO) {
        val gz = File(archivesDir(), fileName)
        val dest = File(context.cacheDir, fileName.removeSuffix(".gz"))
        GZIPInputStream(FileInputStream(gz)).use { input ->
            FileOutputStream(dest).use { input.copyTo(it) }
        }
        dest
    }

    /**
     * Read-only viewer for an archived FY. Opens a dedicated SQLite connection
     * (equivalent to ATTACH of `archives/fy-XXXX.db.gz`) with query_only=ON.
     */
    suspend fun readArchivedTransactions(fileName: String): List<ArchivedTxn> = withContext(Dispatchers.IO) {
        val dest = openArchiveFile(fileName)
        val driver = BundledSQLiteDriver()
        val conn = driver.open(dest.absolutePath)
        conn.execSQL("PRAGMA query_only = ON")
        val out = mutableListOf<ArchivedTxn>()
        conn.prepare(
            "SELECT id, accountId, amount, direction, note, occurredAt, jalaliYear, jalaliMonth, jalaliDay, categoryId FROM transactions ORDER BY occurredAt DESC",
        ).use { stmt ->
            while (stmt.step()) {
                out += ArchivedTxn(
                    id = stmt.getText(0),
                    accountId = stmt.getText(1),
                    amount = stmt.getLong(2),
                    direction = stmt.getText(3),
                    note = stmt.getText(4),
                    occurredAt = stmt.getLong(5),
                    jalaliYear = stmt.getInt(6),
                    jalaliMonth = stmt.getInt(7),
                    jalaliDay = stmt.getInt(8),
                    categoryId = if (stmt.isNull(9)) null else stmt.getText(9),
                )
            }
        }
        conn.close()
        out
    }

    private suspend fun checkpoint() {
        db.useWriterConnection { conn ->
            conn.usePrepared("PRAGMA wal_checkpoint(FULL)") { stmt ->
                stmt.step()
                Unit
            }
        }
    }

    private suspend fun vacuumLive() {
        db.useWriterConnection { conn ->
            conn.usePrepared("VACUUM") { stmt ->
                stmt.step()
                Unit
            }
        }
    }

    private fun sha256(data: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) }
}

data class ArchivedTxn(
    val id: String,
    val accountId: String,
    val amount: Long,
    val direction: String,
    val note: String,
    val occurredAt: Long,
    val jalaliYear: Int,
    val jalaliMonth: Int,
    val jalaliDay: Int,
    val categoryId: String?,
)
