package ir.mhajisoft.hesabres.data.local.db

import androidx.room3.Database
import androidx.room3.RoomDatabase
import ir.mhajisoft.hesabres.data.local.dao.AccountDao
import ir.mhajisoft.hesabres.data.local.dao.ArchiveDao
import ir.mhajisoft.hesabres.data.local.dao.BackupMetadataDao
import ir.mhajisoft.hesabres.data.local.dao.BankAccountDao
import ir.mhajisoft.hesabres.data.local.dao.BankCardDao
import ir.mhajisoft.hesabres.data.local.dao.CategoryDao
import ir.mhajisoft.hesabres.data.local.dao.FiscalYearDao
import ir.mhajisoft.hesabres.data.local.dao.LedgerWriteDao
import ir.mhajisoft.hesabres.data.local.dao.OpeningBalanceDao
import ir.mhajisoft.hesabres.data.local.dao.PersonDao
import ir.mhajisoft.hesabres.data.local.dao.SecretBlobDao
import ir.mhajisoft.hesabres.data.local.dao.SnapshotDao
import ir.mhajisoft.hesabres.data.local.dao.TransactionDao
import ir.mhajisoft.hesabres.data.local.dao.TransferDao
import ir.mhajisoft.hesabres.data.local.entity.AccountEntity
import ir.mhajisoft.hesabres.data.local.entity.AccountOpeningBalanceEntity
import ir.mhajisoft.hesabres.data.local.entity.ArchiveEntity
import ir.mhajisoft.hesabres.data.local.entity.BackupMetadataEntity
import ir.mhajisoft.hesabres.data.local.entity.BalanceSnapshotEntity
import ir.mhajisoft.hesabres.data.local.entity.BankAccountEntity
import ir.mhajisoft.hesabres.data.local.entity.BankCardEntity
import ir.mhajisoft.hesabres.data.local.entity.CategoryEntity
import ir.mhajisoft.hesabres.data.local.entity.FiscalYearEntity
import ir.mhajisoft.hesabres.data.local.entity.PersonEntity
import ir.mhajisoft.hesabres.data.local.entity.SecretBlobEntity
import ir.mhajisoft.hesabres.data.local.entity.TransactionEntity
import ir.mhajisoft.hesabres.data.local.entity.TransferEntity

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
