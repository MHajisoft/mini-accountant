package ir.mhajisoft.miniaccountant.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import ir.mhajisoft.miniaccountant.data.local.entity.AccountEntity
import ir.mhajisoft.miniaccountant.data.local.entity.PersonEntity
import ir.mhajisoft.miniaccountant.data.local.entity.TransactionEntity
import ir.mhajisoft.miniaccountant.data.local.entity.TransferEntity

@Dao
interface LedgerWriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(entity: TransferEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTxns(entities: List<TransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(entity: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(entity: PersonEntity)

    @Query("DELETE FROM transactions WHERE transferId = :transferId")
    suspend fun deleteTxnsByTransfer(transferId: String)

    @Query("DELETE FROM transfers WHERE id = :id")
    suspend fun deleteTransfer(id: String)

    @Transaction
    suspend fun postTransferAtomic(transfer: TransferEntity, legs: List<TransactionEntity>) {
        insertTransfer(transfer)
        insertTxns(legs)
    }

    @Transaction
    suspend fun deleteTransferAtomic(transferId: String) {
        deleteTxnsByTransfer(transferId)
        deleteTransfer(transferId)
    }

    @Transaction
    suspend fun createPersonAtomic(account: AccountEntity, person: PersonEntity) {
        insertAccount(account)
        insertPerson(person)
    }
}
