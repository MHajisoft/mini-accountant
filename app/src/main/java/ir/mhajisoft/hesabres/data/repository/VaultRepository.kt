package ir.mhajisoft.hesabres.data.repository

import ir.mhajisoft.hesabres.data.bank.BankDirectoryRepository
import ir.mhajisoft.hesabres.data.local.dao.BankAccountDao
import ir.mhajisoft.hesabres.data.local.dao.BankCardDao
import ir.mhajisoft.hesabres.data.local.toDomain
import ir.mhajisoft.hesabres.data.local.toEntity
import ir.mhajisoft.hesabres.data.secret.SecretVault
import ir.mhajisoft.hesabres.domain.bank.CardMath
import ir.mhajisoft.hesabres.domain.bank.IbanMath
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
        val digits = CardMath.normalizeDigits(panAscii)
        if (!CardMath.luhnValid(digits)) {
            return Result.failure(IllegalArgumentException("luhn"))
        }
        val match = directory.resolvePan(digits)
        val bank = (match as? ir.mhajisoft.hesabres.domain.bank.BankMatch.Known)?.bank
        val panId = if (storePan) vault.encryptPan(digits) else null
        val cvvId = if (rememberCvv && !cvvAscii.isNullOrBlank() && encryptCvv != null) {
            encryptCvv(CardMath.normalizeDigits(cvvAscii))
        } else {
            null
        }
        val card = BankCard(
            id = existingId ?: UUID.randomUUID().toString(),
            accountId = accountId,
            last4 = CardMath.last4(digits),
            bin6 = CardMath.bin6(digits),
            bankCode = bank?.id ?: "unknown",
            expiryMonth = expiryMonth,
            expiryYear = expiryYear,
            holderName = holderName,
            panCipherId = panId,
            cvvCipherId = cvvId,
            rememberCvv = cvvId != null,
            personId = personId,
        )
        cards.upsert(card.toEntity())
        return Result.success(card)
    }

    suspend fun saveBankAccount(
        accountId: String,
        accountNumber: String,
        ibanRaw: String,
        existing: BankAccount? = null,
        personId: String? = null,
    ): Result<BankAccount> {
        if (!IbanMath.isValidIranIban(ibanRaw)) {
            return Result.failure(IllegalArgumentException("iban"))
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
            personId = personId,
        )
        bankAccounts.upsert(row.toEntity())
        return Result.success(row)
    }

    suspend fun decryptPan(id: String): String? = vault.decryptPan(id)
    val secretVault get() = vault
}
