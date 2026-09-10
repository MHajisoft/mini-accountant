package ir.mhajisoft.miniaccountant.data.local

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
import ir.mhajisoft.miniaccountant.data.local.entity.TransactionEntity
import ir.mhajisoft.miniaccountant.data.local.entity.TransferEntity
import ir.mhajisoft.miniaccountant.domain.model.Account
import ir.mhajisoft.miniaccountant.domain.model.AccountOpeningBalance
import ir.mhajisoft.miniaccountant.domain.model.AccountType
import ir.mhajisoft.miniaccountant.domain.model.ArchiveRecord
import ir.mhajisoft.miniaccountant.domain.model.ArchiveStatus
import ir.mhajisoft.miniaccountant.domain.model.BackupMetadata
import ir.mhajisoft.miniaccountant.domain.model.BackupProvider
import ir.mhajisoft.miniaccountant.domain.model.BalanceSnapshot
import ir.mhajisoft.miniaccountant.domain.model.BankAccount
import ir.mhajisoft.miniaccountant.domain.model.BankCard
import ir.mhajisoft.miniaccountant.domain.model.Category
import ir.mhajisoft.miniaccountant.domain.model.CategoryKind
import ir.mhajisoft.miniaccountant.domain.model.Direction
import ir.mhajisoft.miniaccountant.domain.model.FiscalYear
import ir.mhajisoft.miniaccountant.domain.model.LedgerTransaction
import ir.mhajisoft.miniaccountant.domain.model.Person
import ir.mhajisoft.miniaccountant.domain.model.Transfer

fun AccountEntity.toDomain() = Account(
    id, name, AccountType.valueOf(type), currency, includeInTotal, archived, color, sortOrder, createdAt, updatedAt,
)

fun Account.toEntity() = AccountEntity(
    id, name, type.name, currency, includeInTotal, archived, color, sortOrder, createdAt, updatedAt,
)

fun CategoryEntity.toDomain() = Category(
    id, name, iconKey, color, CategoryKind.valueOf(kind), parentId, isSystem, sortOrder,
)

fun Category.toEntity() = CategoryEntity(
    id, name, iconKey, color, kind.name, parentId, isSystem, sortOrder,
)

fun FiscalYearEntity.toDomain() = FiscalYear(
    id, label, startJalaliYear, startJalaliMonth, startJalaliDay,
    endJalaliYear, endJalaliMonth, endJalaliDay, startEpoch, endEpoch, isCurrent, closedAt,
)

fun FiscalYear.toEntity() = FiscalYearEntity(
    id, label, startJalaliYear, startJalaliMonth, startJalaliDay,
    endJalaliYear, endJalaliMonth, endJalaliDay, startEpoch, endEpoch, isCurrent, closedAt,
)

fun TransactionEntity.toDomain() = LedgerTransaction(
    id, accountId, categoryId, personId, amount, Direction.valueOf(direction),
    note, occurredAt, jalaliYear, jalaliMonth, jalaliDay, fiscalYearId, transferId, createdAt,
)

fun LedgerTransaction.toEntity() = TransactionEntity(
    id, accountId, categoryId, personId, amount, direction.name,
    note, occurredAt, jalaliYear, jalaliMonth, jalaliDay, fiscalYearId, transferId, createdAt,
)

fun TransferEntity.toDomain() = Transfer(
    id, fromAccountId, toAccountId, amount, fee, occurredAt, fiscalYearId, note,
)

fun Transfer.toEntity() = TransferEntity(
    id, fromAccountId, toAccountId, amount, fee, occurredAt, fiscalYearId, note,
)

fun PersonEntity.toDomain() = Person(
    id, accountId, name, phone, note, firstName, lastName, email, instagram, telegram, whatsapp, avatarColor,
)
fun Person.toEntity() = PersonEntity(
    id, accountId, name, phone, note, firstName, lastName, email, instagram, telegram, whatsapp, avatarColor,
)

fun BankCardEntity.toDomain() = BankCard(
    id, accountId, last4, bin6, bankCode, expiryMonth, expiryYear, holderName, panCipherId, cvvCipherId, rememberCvv, personId,
)

fun BankCard.toEntity() = BankCardEntity(
    id, accountId, last4, bin6, bankCode, expiryMonth, expiryYear, holderName, panCipherId, cvvCipherId, rememberCvv, personId,
)

fun BankAccountEntity.toDomain() = BankAccount(id, accountId, accountNumber, iban, bankCode, bankName, personId)
fun BankAccount.toEntity() = BankAccountEntity(id, accountId, accountNumber, iban, bankCode, bankName, personId)

fun AccountOpeningBalanceEntity.toDomain() = AccountOpeningBalance(fiscalYearId, accountId, amountSigned)
fun AccountOpeningBalance.toEntity() = AccountOpeningBalanceEntity(fiscalYearId, accountId, amountSigned)

fun BalanceSnapshotEntity.toDomain() = BalanceSnapshot(id, fiscalYearId, accountId, amountSigned, capturedAt)
fun BalanceSnapshot.toEntity() = BalanceSnapshotEntity(id, fiscalYearId, accountId, amountSigned, capturedAt)

fun ArchiveEntity.toDomain() = ArchiveRecord(
    id, fiscalYearId, fileName, byteSize, sha256, rowCount, ArchiveStatus.valueOf(status),
)

fun ArchiveRecord.toEntity() = ArchiveEntity(
    id, fiscalYearId, fileName, byteSize, sha256, rowCount, status.name,
)

fun BackupMetadataEntity.toDomain() = BackupMetadata(
    id, BackupProvider.valueOf(provider), remoteIdOrUri, fileName, checksum, schemaVersion, includesSecrets, createdAt,
)

fun BackupMetadata.toEntity() = BackupMetadataEntity(
    id, provider.name, remoteIdOrUri, fileName, checksum, schemaVersion, includesSecrets, createdAt,
)
