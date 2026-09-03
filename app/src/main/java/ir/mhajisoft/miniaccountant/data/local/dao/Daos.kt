package ir.mhajisoft.miniaccountant.data.local.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
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
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE archived = 0 ORDER BY sortOrder, name")
    fun observeActive(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun get(id: String): AccountEntity?

    @Query("SELECT * FROM accounts")
    suspend fun getAll(): List<AccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AccountEntity)

    @Update
    suspend fun update(entity: AccountEntity)

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun get(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<CategoryEntity>)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}

@Dao
interface FiscalYearDao {
    @Query("SELECT * FROM fiscal_years ORDER BY startEpoch")
    fun observeAll(): Flow<List<FiscalYearEntity>>

    @Query("SELECT * FROM fiscal_years WHERE isCurrent = 1 LIMIT 1")
    fun observeCurrent(): Flow<FiscalYearEntity?>

    @Query("SELECT * FROM fiscal_years WHERE isCurrent = 1 LIMIT 1")
    suspend fun getCurrent(): FiscalYearEntity?

    @Query("SELECT * FROM fiscal_years WHERE id = :id")
    suspend fun get(id: String): FiscalYearEntity?

    @Query("SELECT * FROM fiscal_years ORDER BY startEpoch")
    suspend fun getAll(): List<FiscalYearEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FiscalYearEntity)

    @Query("UPDATE fiscal_years SET isCurrent = 0")
    suspend fun clearCurrent()

    @Query("UPDATE fiscal_years SET closedAt = :closedAt WHERE id = :id")
    suspend fun close(id: String, closedAt: Long)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC, createdAt DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query(
        "SELECT * FROM transactions WHERE fiscalYearId = :fyId ORDER BY occurredAt DESC",
    )
    fun observeForYear(fyId: String): Flow<List<TransactionEntity>>

    @Query(
        "SELECT * FROM transactions WHERE fiscalYearId = :fyId AND jalaliMonth = :month ORDER BY occurredAt DESC",
    )
    fun observeMonth(fyId: String, month: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions")
    suspend fun getAll(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun get(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE transferId = :transferId")
    suspend fun forTransfer(transferId: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId")
    suspend fun forAccount(accountId: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE fiscalYearId = :fyId")
    suspend fun forYear(fyId: String): List<TransactionEntity>

    @Query(
        "SELECT * FROM transactions WHERE fiscalYearId = :fyId AND transferId IS NULL",
    )
    suspend fun pnlForYear(fyId: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE (:query = '' OR note LIKE '%' || :query || '%' )
        ORDER BY occurredAt DESC
        """,
    )
    fun search(query: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<TransactionEntity>)

    @Update
    suspend fun update(entity: TransactionEntity)

    @Delete
    suspend fun delete(entity: TransactionEntity)

    @Query("DELETE FROM transactions WHERE transferId = :transferId")
    suspend fun deleteByTransfer(transferId: String)

    @Query("DELETE FROM transactions WHERE fiscalYearId = :fyId")
    suspend fun deleteForYear(fyId: String)

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE fiscalYearId = :fyId")
    suspend fun countForYear(fyId: String): Int
}

@Dao
interface TransferDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TransferEntity)

    @Query("SELECT * FROM transfers WHERE id = :id")
    suspend fun get(id: String): TransferEntity?

    @Query("DELETE FROM transfers WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM transfers WHERE fiscalYearId = :fyId")
    suspend fun deleteForYear(fyId: String)

    @Query("SELECT * FROM transfers WHERE fiscalYearId = :fyId")
    suspend fun forYear(fyId: String): List<TransferEntity>
}

@Dao
interface PersonDao {
    @Query("SELECT * FROM people ORDER BY name")
    fun observeAll(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people")
    suspend fun getAll(): List<PersonEntity>

    @Query("SELECT * FROM people WHERE id = :id")
    suspend fun get(id: String): PersonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PersonEntity)

    @Delete
    suspend fun delete(entity: PersonEntity)
}

@Dao
interface BankCardDao {
    @Query("SELECT * FROM bank_cards ORDER BY last4")
    fun observeAll(): Flow<List<BankCardEntity>>

    @Query("SELECT * FROM bank_cards")
    suspend fun getAll(): List<BankCardEntity>

    @Query("SELECT * FROM bank_cards WHERE id = :id")
    suspend fun get(id: String): BankCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BankCardEntity)

    @Query(
        "UPDATE bank_cards SET cvvCipherId = NULL, rememberCvv = 0 WHERE id = :id",
    )
    suspend fun clearCvv(id: String)

    @Query("UPDATE bank_cards SET cvvCipherId = NULL, rememberCvv = 0, panCipherId = NULL")
    suspend fun stripSecrets()

    @Delete
    suspend fun delete(entity: BankCardEntity)
}

@Dao
interface BankAccountDao {
    @Query("SELECT * FROM bank_accounts")
    fun observeAll(): Flow<List<BankAccountEntity>>

    @Query("SELECT * FROM bank_accounts")
    suspend fun getAll(): List<BankAccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BankAccountEntity)

    @Delete
    suspend fun delete(entity: BankAccountEntity)
}

@Dao
interface OpeningBalanceDao {
    @Query("SELECT * FROM account_opening_balances")
    fun observeAll(): Flow<List<AccountOpeningBalanceEntity>>

    @Query("SELECT * FROM account_opening_balances WHERE fiscalYearId = :fyId")
    fun observeForYear(fyId: String): Flow<List<AccountOpeningBalanceEntity>>

    @Query("SELECT * FROM account_opening_balances WHERE fiscalYearId = :fyId")
    suspend fun forYear(fyId: String): List<AccountOpeningBalanceEntity>

    @Query(
        "SELECT * FROM account_opening_balances WHERE fiscalYearId = :fyId AND accountId = :accountId",
    )
    suspend fun get(fyId: String, accountId: String): AccountOpeningBalanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AccountOpeningBalanceEntity)
}

@Dao
interface SnapshotDao {
    @Query("SELECT * FROM balance_snapshots")
    fun observeAll(): Flow<List<BalanceSnapshotEntity>>

    @Query("SELECT * FROM balance_snapshots WHERE fiscalYearId = :fyId")
    suspend fun forYear(fyId: String): List<BalanceSnapshotEntity>

    @Query("SELECT * FROM balance_snapshots")
    suspend fun getAll(): List<BalanceSnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<BalanceSnapshotEntity>)
}

@Dao
interface ArchiveDao {
    @Query("SELECT * FROM archives ORDER BY fileName DESC")
    fun observeAll(): Flow<List<ArchiveEntity>>

    @Query("SELECT * FROM archives")
    suspend fun getAll(): List<ArchiveEntity>

    @Query("SELECT * FROM archives WHERE fiscalYearId = :fyId")
    suspend fun forYear(fyId: String): ArchiveEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ArchiveEntity)
}

@Dao
interface BackupMetadataDao {
    @Query("SELECT * FROM backup_metadata ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BackupMetadataEntity>>

    @Query(
        "SELECT * FROM backup_metadata WHERE provider = :provider ORDER BY createdAt DESC",
    )
    suspend fun forProvider(provider: String): List<BackupMetadataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BackupMetadataEntity)

    @Query("DELETE FROM backup_metadata WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface SecretBlobDao {
    @Query("SELECT * FROM secret_blobs WHERE id = :id")
    suspend fun get(id: String): SecretBlobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SecretBlobEntity)

    @Query("DELETE FROM secret_blobs WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM secret_blobs WHERE kind = 'cvv'")
    suspend fun deleteAllCvv()

    @Query("SELECT id FROM secret_blobs WHERE kind = 'cvv'")
    suspend fun cvvIds(): List<String>
}
