package ir.mhajisoft.hesabres.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val currency: String,
    val includeInTotal: Boolean,
    val archived: Boolean,
    val color: Long,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "categories",
    indices = [Index("parentId")],
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconKey: String,
    val color: Long,
    val kind: String,
    val parentId: String?,
    val isSystem: Boolean,
    val sortOrder: Int,
)

@Entity(tableName = "fiscal_years")
data class FiscalYearEntity(
    @PrimaryKey val id: String,
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

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["accountId", "occurredAt"]),
        Index(value = ["fiscalYearId", "jalaliMonth"]),
        Index(value = ["categoryId", "occurredAt"]),
        Index(value = ["transferId"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = FiscalYearEntity::class,
            parentColumns = ["id"],
            childColumns = ["fiscalYearId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val categoryId: String?,
    val personId: String?,
    val amount: Long,
    val direction: String,
    val note: String,
    val occurredAt: Long,
    val jalaliYear: Int,
    val jalaliMonth: Int,
    val jalaliDay: Int,
    val fiscalYearId: String,
    val transferId: String?,
    val createdAt: Long,
)

@Entity(
    tableName = "transfers",
    indices = [Index("fiscalYearId"), Index("fromAccountId"), Index("toAccountId")],
)
data class TransferEntity(
    @PrimaryKey val id: String,
    val fromAccountId: String,
    val toAccountId: String,
    val amount: Long,
    val fee: Long?,
    val occurredAt: Long,
    val fiscalYearId: String,
    val note: String,
)

@Entity(
    tableName = "people",
    indices = [Index("accountId")],
)
data class PersonEntity(
    @PrimaryKey val id: String,
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
)

@Entity(
    tableName = "bank_cards",
    indices = [Index("accountId"), Index("personId")],
)
data class BankCardEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val last4: String,
    val bin6: String,
    val bankCode: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val holderName: String?,
    val panCipherId: String?,
    val cvvCipherId: String?,
    val rememberCvv: Boolean,
    val personId: String? = null,
)

@Entity(
    tableName = "bank_accounts",
    indices = [Index("accountId"), Index("personId")],
)
data class BankAccountEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val accountNumber: String,
    val iban: String,
    val bankCode: String,
    val bankName: String,
    val personId: String? = null,
)

@Entity(
    tableName = "account_opening_balances",
    primaryKeys = ["fiscalYearId", "accountId"],
)
data class AccountOpeningBalanceEntity(
    val fiscalYearId: String,
    val accountId: String,
    val amountSigned: Long,
)

@Entity(
    tableName = "balance_snapshots",
    indices = [Index(value = ["fiscalYearId", "accountId"], unique = true)],
)
data class BalanceSnapshotEntity(
    @PrimaryKey val id: String,
    val fiscalYearId: String,
    val accountId: String,
    val amountSigned: Long,
    val capturedAt: Long,
)

@Entity(tableName = "archives")
data class ArchiveEntity(
    @PrimaryKey val id: String,
    val fiscalYearId: String,
    val fileName: String,
    val byteSize: Long,
    val sha256: String,
    val rowCount: Int,
    val status: String,
)

@Entity(tableName = "backup_metadata")
data class BackupMetadataEntity(
    @PrimaryKey val id: String,
    val provider: String,
    val remoteIdOrUri: String,
    val fileName: String,
    val checksum: String,
    val schemaVersion: Int,
    val includesSecrets: Boolean,
    val createdAt: Long,
)

@Entity(
    tableName = "secret_blobs",
    indices = [Index("kind")],
)
data class SecretBlobEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val nonce: ByteArray,
    val ciphertext: ByteArray,
    val createdAt: Long,
)
