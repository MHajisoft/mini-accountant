package ir.mhajisoft.hesabres.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import ir.mhajisoft.hesabres.data.local.entity.AccountEntity
import ir.mhajisoft.hesabres.data.local.entity.PersonEntity
import ir.mhajisoft.hesabres.data.local.entity.TransactionEntity
import ir.mhajisoft.hesabres.data.local.entity.TransferEntity

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSocialLinks(entities: List<ir.mhajisoft.hesabres.data.local.entity.PersonSocialLinkEntity>)

    @Query("DELETE FROM person_social_links WHERE personId = :personId")
    suspend fun deleteSocialLinks(personId: String)

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
    suspend fun createPersonAtomic(
        account: AccountEntity,
        person: PersonEntity,
        links: List<ir.mhajisoft.hesabres.data.local.entity.PersonSocialLinkEntity>,
    ) {
        insertAccount(account)
        insertPerson(person)
        if (links.isNotEmpty()) insertSocialLinks(links)
    }

    @Transaction
    suspend fun replacePersonSocials(personId: String, links: List<ir.mhajisoft.hesabres.data.local.entity.PersonSocialLinkEntity>) {
        deleteSocialLinks(personId)
        if (links.isNotEmpty()) insertSocialLinks(links)
    }
}
