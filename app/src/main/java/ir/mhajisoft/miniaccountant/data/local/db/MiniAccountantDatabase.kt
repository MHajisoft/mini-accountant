package ir.mhajisoft.miniaccountant.data.local.db

import androidx.room3.Database
import androidx.room3.RoomDatabase
import ir.mhajisoft.miniaccountant.data.local.dao.AccountDao
import ir.mhajisoft.miniaccountant.data.local.dao.ArchiveDao
import ir.mhajisoft.miniaccountant.data.local.dao.BackupMetadataDao
import ir.mhajisoft.miniaccountant.data.local.dao.BankAccountDao
import ir.mhajisoft.miniaccountant.data.local.dao.BankCardDao
import ir.mhajisoft.miniaccountant.data.local.dao.CategoryDao
import ir.mhajisoft.miniaccountant.data.local.dao.FiscalYearDao
import ir.mhajisoft.miniaccountant.data.local.dao.LedgerWriteDao
import ir.mhajisoft.miniaccountant.data.local.dao.OpeningBalanceDao
import ir.mhajisoft.miniaccountant.data.local.dao.PersonDao
import ir.mhajisoft.miniaccountant.data.local.dao.SecretBlobDao
import ir.mhajisoft.miniaccountant.data.local.dao.SnapshotDao
import ir.mhajisoft.miniaccountant.data.local.dao.TransactionDao
import ir.mhajisoft.miniaccountant.data.local.dao.TransferDao
import ir.mhajisoft.miniaccountant.data.local.entity.AccountEntity
import ir.mhajisoft.miniaccountant.data.local.entity.AccountOpeningBalanceEntity
import ir.mhajisoft.miniaccountant.data.local.entity.ArchiveEntity
import ir.mhajisoft.miniaccountant.data.local.entity.BackupMetadataEntity
import ir.mhajisoft.miniaccountant.data.local.entity.BalanceSnapshotEntity
import ir.mhajisoft.miniaccountant.data.local.entity.BankAccountEntity
import ir.mhajisoft.miniaccountant.data.local.entity.BankCardEntity
import ir.mhajisoft.miniaccountant.data.local.entity.CategoryEntity
import ir.mhajisoft.miniaccountant.data.local.entity.FiscalYearEntity
import ir.mhajisoft.miniaccountant.data.local.entity.PersonEntity
import ir.mhajisoft.miniaccountant.data.local.entity.SecretBlobEntity
import ir.mhajisoft.miniaccountant.data.local.entity.TransactionEntity
import ir.mhajisoft.miniaccountant.data.local.entity.TransferEntity

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        FiscalYearEntity::class,
        TransactionEntity::class,
        TransferEntity::class,
        PersonEntity::class,
        BankCardEntity::class,
        BankAccountEntity::class,
        AccountOpeningBalanceEntity::class,
        BalanceSnapshotEntity::class,
        ArchiveEntity::class,
        BackupMetadataEntity::class,
        SecretBlobEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class MiniAccountantDatabase : RoomDatabase() {
    abstract fun accounts(): AccountDao
    abstract fun categories(): CategoryDao
    abstract fun fiscalYears(): FiscalYearDao
    abstract fun transactions(): TransactionDao
    abstract fun transfers(): TransferDao
    abstract fun people(): PersonDao
    abstract fun cards(): BankCardDao
    abstract fun bankAccounts(): BankAccountDao
    abstract fun openings(): OpeningBalanceDao
    abstract fun snapshots(): SnapshotDao
    abstract fun archives(): ArchiveDao
    abstract fun backups(): BackupMetadataDao
    abstract fun secrets(): SecretBlobDao
    abstract fun ledgerWrite(): LedgerWriteDao

    companion object {
        const val FILE_NAME = "mini_accountant.db"
        const val SCHEMA_VERSION = 2
    }
}
