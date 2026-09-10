package ir.mhajisoft.miniaccountant.domain.model

enum class AccountType {
    CASH,
    BANK,
    CARD,
    PERSON,
}

enum class Direction {
    IN,
    OUT,
}

enum class CategoryKind {
    EXPENSE,
    INCOME,
    TRANSFER,
}

enum class BackupProvider {
    LOCAL_SAF,
    DRIVE_APPDATA,
    ONEDRIVE_APPROOT,
}

enum class ArchiveStatus {
    WRITING,
    READY,
    FAILED,
}

data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val currency: String = "IRR",
    val includeInTotal: Boolean = true,
    val archived: Boolean = false,
    val color: Long,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

data class LedgerTransaction(
    val id: String,
    val accountId: String,
    val categoryId: String?,
    val personId: String?,
    val amount: Long,
    val direction: Direction,
    val note: String,
    val occurredAt: Long,
    val jalaliYear: Int,
    val jalaliMonth: Int,
    val jalaliDay: Int,
    val fiscalYearId: String,
    val transferId: String?,
    val createdAt: Long,
) {
    init {
        require(amount >= 0L) { "Amount must be a non-negative rial Long" }
    }

    val signedRials: Long
        get() = if (direction == Direction.IN) amount else -amount
}

data class Transfer(
    val id: String,
    val fromAccountId: String,
    val toAccountId: String,
    val amount: Long,
    val fee: Long?,
    val occurredAt: Long,
    val fiscalYearId: String,
    val note: String,
) {
    init {
        require(amount > 0L) { "Transfer amount must be positive rials" }
        require(fee == null || fee >= 0L) { "Fee must be null or non-negative" }
        require(fromAccountId != toAccountId) { "Transfer accounts must differ" }
    }
}

data class Category(
    val id: String,
    val name: String,
    val iconKey: String,
    val color: Long,
    val kind: CategoryKind,
    val parentId: String?,
    val isSystem: Boolean,
    val sortOrder: Int,
)

data class Person(
    val id: String,
    val accountId: String,
    val name: String,
    val phone: String?,
    val note: String?,
    val firstName: String = "",
    val lastName: String = "",
    val email: String? = null,
    val instagram: String? = null,
    val telegram: String? = null,
    val whatsapp: String? = null,
    val avatarColor: Long = 0xFF0F766E,
) {
    val displayName: String
        get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { name }

    val initials: String
        get() {
            val a = firstName.trim().firstOrNull()
            val b = lastName.trim().firstOrNull()
            return when {
                a != null && b != null -> "$a$b"
                a != null -> a.toString()
                displayName.isNotBlank() -> displayName.take(1)
                else -> "؟"
            }
        }
}

data class BankCard(
    val id: String,
    val accountId: String,
    val last4: String,
    val bin6: String,
    val bankCode: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val holderName: String?,
    val panCipherId: String?,
    val cvvCipherId: String?,
    val rememberCvv: Boolean = false,
    val personId: String? = null,
)

data class BankAccount(
    val id: String,
    val accountId: String,
    val accountNumber: String,
    val iban: String,
    val bankCode: String,
    val bankName: String,
    val personId: String? = null,
)

data class FiscalYear(
    val id: String,
    val label: String,
    val startJalaliYear: Int,
    val startJalaliMonth: Int,
    val startJalaliDay: Int,
    val endJalaliYear: Int,
    val endJalaliMonth: Int,
    val endJalaliDay: Int,
    val startEpoch: Long,
    val endEpoch: Long,
    val isCurrent: Boolean,
    val closedAt: Long?,
)

data class AccountOpeningBalance(
    val fiscalYearId: String,
    val accountId: String,
    val amountSigned: Long,
)

data class BalanceSnapshot(
    val id: String,
    val fiscalYearId: String,
    val accountId: String,
    val amountSigned: Long,
    val capturedAt: Long,
)

data class ArchiveRecord(
    val id: String,
    val fiscalYearId: String,
    val fileName: String,
    val byteSize: Long,
    val sha256: String,
    val rowCount: Int,
    val status: ArchiveStatus,
)

data class BackupMetadata(
    val id: String,
    val provider: BackupProvider,
    val remoteIdOrUri: String,
    val fileName: String,
    val checksum: String,
    val schemaVersion: Int,
    val includesSecrets: Boolean = false,
    val createdAt: Long,
)

data class AppLockSettings(
    val enabled: Boolean = false,
    val timeoutSec: Int = 60,
    val lockOnLeave: Boolean = true,
)
