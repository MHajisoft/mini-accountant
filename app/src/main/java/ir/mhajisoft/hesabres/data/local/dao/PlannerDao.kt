package ir.mhajisoft.hesabres.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import ir.mhajisoft.hesabres.data.local.entity.ChequeEntity
import ir.mhajisoft.hesabres.data.local.entity.InstallmentEntity
import ir.mhajisoft.hesabres.data.local.entity.MonthBudgetEntity
import ir.mhajisoft.hesabres.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannerDao {
    @Query("SELECT * FROM notes ORDER BY occurredAt DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(entity: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: String)

    @Query("SELECT * FROM cheques ORDER BY dueAt ASC")
    fun observeCheques(): Flow<List<ChequeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCheque(entity: ChequeEntity)

    @Query("DELETE FROM cheques WHERE id = :id")
    suspend fun deleteCheque(id: String)

    @Query("SELECT * FROM installments ORDER BY nextDueAt ASC")
    fun observeInstallments(): Flow<List<InstallmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInstallment(entity: InstallmentEntity)

    @Query("DELETE FROM installments WHERE id = :id")
    suspend fun deleteInstallment(id: String)

    @Query("SELECT * FROM month_budgets")
    fun observeBudgets(): Flow<List<MonthBudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(entity: MonthBudgetEntity)
}
