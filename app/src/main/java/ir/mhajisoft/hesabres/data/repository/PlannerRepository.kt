package ir.mhajisoft.hesabres.data.repository

import ir.mhajisoft.hesabres.data.local.dao.PlannerDao
import ir.mhajisoft.hesabres.data.local.toDomain
import ir.mhajisoft.hesabres.data.local.toEntity
import ir.mhajisoft.hesabres.domain.crash.DomainLists
import ir.mhajisoft.hesabres.domain.jalali.JalaliConverter
import ir.mhajisoft.hesabres.domain.jalali.JalaliYmd
import ir.mhajisoft.hesabres.domain.planner.Cheque
import ir.mhajisoft.hesabres.domain.planner.InstallmentPlan
import ir.mhajisoft.hesabres.domain.planner.MonthBudget
import ir.mhajisoft.hesabres.domain.planner.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlannerRepository @Inject constructor(
    private val dao: PlannerDao,
) {
    val notes: Flow<List<Note>> = dao.observeNotes().map { rows -> DomainLists.safeMap(rows) { it.toDomain() } }
    val cheques: Flow<List<Cheque>> = dao.observeCheques().map { rows -> DomainLists.safeMap(rows) { it.toDomain() } }
    val installments: Flow<List<InstallmentPlan>> =
        dao.observeInstallments().map { rows -> DomainLists.safeMap(rows) { it.toDomain() } }
    val budgets: Flow<List<MonthBudget>> = dao.observeBudgets().map { rows -> DomainLists.safeMap(rows) { it.toDomain() } }

    suspend fun saveNote(body: String, ymd: JalaliYmd, existingId: String? = null) {
        val now = System.currentTimeMillis()
        val note = Note(
            id = existingId ?: UUID.randomUUID().toString(),
            body = body.trim(),
            year = ymd.year,
            month = ymd.month,
            day = ymd.day,
            occurredAt = JalaliConverter.toEpochMillisStartOfDay(ymd),
            createdAt = now,
        )
        dao.upsertNote(note.toEntity())
    }

    suspend fun deleteNote(id: String) = dao.deleteNote(id)

    suspend fun saveCheque(cheque: Cheque) = dao.upsertCheque(cheque.toEntity())

    suspend fun deleteCheque(id: String) = dao.deleteCheque(id)

    suspend fun saveInstallment(plan: InstallmentPlan) = dao.upsertInstallment(plan.toEntity())

    suspend fun deleteInstallment(id: String) = dao.deleteInstallment(id)

    suspend fun saveBudget(budget: MonthBudget) = dao.upsertBudget(budget.toEntity())
}
