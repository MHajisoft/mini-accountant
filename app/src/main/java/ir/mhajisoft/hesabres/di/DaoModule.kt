package ir.mhajisoft.hesabres.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
import ir.mhajisoft.hesabres.data.local.db.MiniAccountantDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DaoModule {
    @Provides @Singleton fun accounts(db: MiniAccountantDatabase): AccountDao = db.accounts()
    @Provides @Singleton fun categories(db: MiniAccountantDatabase): CategoryDao = db.categories()
    @Provides @Singleton fun fiscalYears(db: MiniAccountantDatabase): FiscalYearDao = db.fiscalYears()
    @Provides @Singleton fun transactions(db: MiniAccountantDatabase): TransactionDao = db.transactions()
    @Provides @Singleton fun transfers(db: MiniAccountantDatabase): TransferDao = db.transfers()
    @Provides @Singleton fun people(db: MiniAccountantDatabase): PersonDao = db.people()
    @Provides @Singleton fun cards(db: MiniAccountantDatabase): BankCardDao = db.cards()
    @Provides @Singleton fun bankAccounts(db: MiniAccountantDatabase): BankAccountDao = db.bankAccounts()
    @Provides @Singleton fun openings(db: MiniAccountantDatabase): OpeningBalanceDao = db.openings()
    @Provides @Singleton fun snapshots(db: MiniAccountantDatabase): SnapshotDao = db.snapshots()
    @Provides @Singleton fun archives(db: MiniAccountantDatabase): ArchiveDao = db.archives()
    @Provides @Singleton fun backups(db: MiniAccountantDatabase): BackupMetadataDao = db.backups()
    @Provides @Singleton fun secrets(db: MiniAccountantDatabase): SecretBlobDao = db.secrets()
    @Provides @Singleton fun writes(db: MiniAccountantDatabase): LedgerWriteDao = db.ledgerWrite()
}
