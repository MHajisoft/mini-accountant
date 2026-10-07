package ir.mhajisoft.hesabres.data.repository

import ir.mhajisoft.hesabres.data.bank.BankDirectoryRepository
import ir.mhajisoft.hesabres.data.local.dao.BankAccountDao
import ir.mhajisoft.hesabres.data.local.dao.BankCardDao
import ir.mhajisoft.hesabres.data.local.toDomain
import ir.mhajisoft.hesabres.data.local.toEntity
import ir.mhajisoft.hesabres.data.secret.SecretVault
import ir.mhajisoft.hesabres.domain.bank.BankMatch
import ir.mhajisoft.hesabres.domain.bank.CardMath
import ir.mhajisoft.hesabres.domain.bank.IbanMath
import ir.mhajisoft.hesabres.domain.bank.VaultInput
import ir.mhajisoft.hesabres.domain.model.BankAccount
import ir.mhajisoft.hesabres.domain.model.BankCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultRepository @Inject constructor(
    private val cards: BankCardDao,
    private val bankAccounts: BankAccountDao,
    private val vault: SecretVault,
    private val banks: BankDirectoryRepository,
) {
    val cardsFlow: Flow<List<BankCard>> = cards.observeAll().map { list -> list.map { it.toDomain() } }
    val bankAccountsFlow: Flow<List<BankAccount>> =
        bankAccounts.observeAll().map { list -> list.map { it.toDomain() } }

    val directory get() = banks.directory

    suspend fun saveCard(
        accountId: String,
        panAscii: String,
        expiryMonth: Int,
        expiryYear: Int,
        holderName: String?,
        rememberCvv: Boolean,
        cvvAscii: String?,
        existingId: String? = null,
        encryptCvv: (suspend (String) -> String)? = null,
        storePan: Boolean = true,
        personId: String? = null,
    ): Result<BankCard> {
        val existing = existingId?.let { cards.get(it)?.toDomain() }
        val digits = CardMath.normalizeDigits(panAscii)
        val inputError = VaultInput.validateCard(
            panRaw = panAscii,
            expiryMonth = expiryMonth,
            expiryYear = expiryYear,
            rememberCvv = rememberCvv,
            cvvRaw = cvvAscii.orEmpty(),
            editing = existing != null,
            hasStoredCvv = existing?.cvvCipherId != null,
        )
        if (inputError != null) {
            return Result.failure(IllegalArgumentException(inputError.name))
        }
        val panId: String?
        val last4: String
        val bin6: String
        val bankCode: String
        if (digits.isEmpty() && existing != null) {
            panId = existing.panCipherId
            last4 = existing.last4
            bin6 = existing.bin6
            bankCode = existing.bankCode
        } else {
            val match = directory.resolvePan(digits)
            val bank = (match as? BankMatch.Known)?.bank
            if (storePan) {
                val fresh = vault.encryptPan(digits)
                existing?.panCipherId?.let { previous ->
                    if (previous != fresh) vault.delete(previous)
                }
                panId = fresh
            } else {
                existing?.panCipherId?.let { vault.delete(it) }
                panId = null
            }
            last4 = CardMath.last4(digits)
            bin6 = CardMath.bin6(digits)
            bankCode = bank?.id ?: "unknown"
        }
        val cvvDigits = CardMath.normalizeDigits(cvvAscii.orEmpty())
        if (rememberCvv && cvvDigits.isNotEmpty() && encryptCvv == null) {
            return Result.failure(IllegalArgumentException("cvv"))
        }
        val cvvId = when {
            rememberCvv && cvvDigits.isNotEmpty() && encryptCvv != null -> {
                val fresh = encryptCvv(cvvDigits)
                existing?.cvvCipherId?.let { previous ->
                    if (previous != fresh) vault.delete(previous)
                }
                fresh
            }
            rememberCvv && cvvDigits.isEmpty() && existing != null && existing.cvvCipherId != null ->
                existing.cvvCipherId
            else -> {
                existing?.cvvCipherId?.let { vault.delete(it) }
                null
            }
        }
        val card = BankCard(
            id = existing?.id ?: existingId ?: UUID.randomUUID().toString(),
            accountId = accountId,
            last4 = last4,
            bin6 = bin6,
            bankCode = bankCode,
            expiryMonth = expiryMonth,
            expiryYear = expiryYear,
            holderName = holderName?.trim()?.ifBlank { null },
            panCipherId = panId,
            cvvCipherId = cvvId,
            rememberCvv = cvvId != null,
            personId = personId ?: existing?.personId,
        )
        cards.upsert(card.toEntity())
        return Result.success(card)
    }

    suspend fun deleteCard(id: String) {
        val entity = cards.get(id) ?: return
        entity.panCipherId?.let { vault.delete(it) }
        entity.cvvCipherId?.let { vault.delete(it) }
        cards.delete(entity)
    }

    suspend fun saveBankAccount(
        accountId: String,
        accountNumber: String,
        ibanRaw: String,
        existing: BankAccount? = null,
        personId: String? = null,
    ): Result<BankAccount> {
        val inputError = VaultInput.validateBankAccount(accountNumber, ibanRaw)
        if (inputError != null) {
            return Result.failure(IllegalArgumentException(inputError.name))
        }
        val compact = IbanMath.normalize(ibanRaw)
        val sheba = IbanMath.shebaBankCode(compact) ?: return Result.failure(IllegalArgumentException("iban"))
        val bank = directory.findBySheba(sheba)
        val row = BankAccount(
            id = existing?.id ?: UUID.randomUUID().toString(),
            accountId = accountId,
            accountNumber = CardMath.normalizeDigits(accountNumber),
            iban = compact,
            bankCode = bank?.id ?: sheba,
            bankName = bank?.nameFa ?: "بانک شناسایی نشد",
            personId = personId ?: existing?.personId,
        )
        bankAccounts.upsert(row.toEntity())
        return Result.success(row)
    }

    suspend fun deleteBankAccount(id: String) {
        val entity = bankAccounts.get(id) ?: return
        bankAccounts.delete(entity)
    }

    suspend fun decryptPan(id: String): String? = vault.decryptPan(id)
    val secretVault get() = vault
}
