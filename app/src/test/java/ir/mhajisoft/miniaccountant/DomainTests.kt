package ir.mhajisoft.miniaccountant

import com.google.common.truth.Truth.assertThat
import ir.mhajisoft.miniaccountant.domain.backup.BackupCard
import ir.mhajisoft.miniaccountant.domain.backup.BackupSanitizer
import ir.mhajisoft.miniaccountant.domain.bank.BankDirectory
import ir.mhajisoft.miniaccountant.domain.bank.BankInfo
import ir.mhajisoft.miniaccountant.domain.bank.BankMatch
import ir.mhajisoft.miniaccountant.domain.bank.CardMath
import ir.mhajisoft.miniaccountant.domain.bank.IbanMath
import ir.mhajisoft.miniaccountant.domain.fiscal.FiscalRebucketer
import ir.mhajisoft.miniaccountant.domain.fiscal.FiscalYearCalculator
import ir.mhajisoft.miniaccountant.domain.jalali.BirashkAlgorithm
import ir.mhajisoft.miniaccountant.domain.jalali.JalaliConverter
import ir.mhajisoft.miniaccountant.domain.jalali.JalaliYmd
import ir.mhajisoft.miniaccountant.domain.ledger.BalanceMath
import ir.mhajisoft.miniaccountant.domain.ledger.SystemCategories
import ir.mhajisoft.miniaccountant.domain.ledger.TransferPoster
import ir.mhajisoft.miniaccountant.domain.model.Direction
import ir.mhajisoft.miniaccountant.domain.model.FiscalYear
import ir.mhajisoft.miniaccountant.domain.model.LedgerTransaction
import ir.mhajisoft.miniaccountant.domain.money.Money
import ir.mhajisoft.miniaccountant.domain.money.PersianDigits
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.io.ByteArrayOutputStream

class MoneyMathTest {
    @Test
    fun rialTomanRoundTrip() {
        assertThat(Money.toDisplayUnit(10, toman = true)).isEqualTo(1)
        assertThat(Money.fromDisplayUnit(12, toman = true)).isEqualTo(120)
        assertThat(Money.parseDisplayAmount("۱۲٬۰۰۰", toman = false)).isEqualTo(12_000L)
        assertThat(Money.parseDisplayAmount("۱۲", toman = true)).isEqualTo(120L)
    }

    @Test
    fun runningBalanceOpeningPlusLive() {
        val opening = 1_000_000L
        val live = listOf(500_000L, -200_000L)
        assertThat(Money.runningBalance(opening, live)).isEqualTo(1_300_000L)
        val txnIn = LedgerTransaction("1", "a", null, null, 500_000, Direction.IN, "", 1, 1405, 1, 1, "fy", null, 1)
        val txnOut = txnIn.copy(id = "2", amount = 200_000, direction = Direction.OUT)
        assertThat(BalanceMath.accountBalance(opening, listOf(txnIn, txnOut))).isEqualTo(1_300_000L)
    }

    @Test
    fun persianDigitsUiOnly() {
        assertThat(PersianDigits.toPersian("1405")).isEqualTo("۱۴۰۵")
        assertThat(PersianDigits.toAscii("۱۴۰۵")).isEqualTo("1405")
    }
}

class BirashkLeapTest {
    @Test
    fun knownLeapYears() {
        assertThat(BirashkAlgorithm.isJalaliLeapYear(1399)).isTrue()
        assertThat(BirashkAlgorithm.isJalaliLeapYear(1403)).isTrue()
        assertThat(BirashkAlgorithm.isJalaliLeapYear(1404)).isFalse()
        assertThat(BirashkAlgorithm.esfandLength(1403)).isEqualTo(30)
        assertThat(BirashkAlgorithm.esfandLength(1404)).isEqualTo(29)
    }
}

class FiscalYearTest {
    @Test
    fun shahrivar15_1405_isFarvardinToEsfand() {
        val epoch = JalaliConverter.toEpochMillisStartOfDay(JalaliYmd(1405, 6, 15))
        val today = JalaliConverter.fromEpochMillis(epoch)
        assertThat(today.year).isEqualTo(1405)
        assertThat(today.month).isEqualTo(6)
        assertThat(today.day).isEqualTo(15)
        val window = FiscalYearCalculator.defaultWindowFor(epoch)
        assertThat(window.start).isEqualTo(JalaliYmd(1405, 1, 1))
        assertThat(window.end.year).isEqualTo(1405)
        assertThat(window.end.month).isEqualTo(12)
        assertThat(window.end.day).isEqualTo(BirashkAlgorithm.esfandLength(1405))
    }

    @Test
    fun rebucketMovesTxnWithoutDelete() {
        val y1 = FiscalYear("fy1", "1404", 1404, 1, 1, 1404, 12, 29, 1, 100, false, null)
        val y2 = FiscalYear("fy2", "1405", 1405, 1, 1, 1405, 12, 29, 101, 200, true, null)
        val txn = LedgerTransaction("t", "a", "sys-food", null, 10, Direction.OUT, "", 150, 1405, 2, 1, "fy1", null, 150)
        val result = FiscalRebucketer.rebucket(listOf(txn), listOf(y1, y2))
        assertThat(result.updated.single().fiscalYearId).isEqualTo("fy2")
        assertThat(result.updated.single().id).isEqualTo("t")
    }
}

class CardIbanTest {
    private val directory = BankDirectory(
        version = 1,
        sources = listOf("pishkhanak", "jadvalino"),
        banks = listOf(
            BankInfo("mellat", "بانک ملت", "Mellat", "012", listOf("610433", "991975"), "bank_mellat"),
            BankInfo("melli", "بانک ملی ایران", "Melli", "017", listOf("603799", "170019"), "bank_melli"),
            BankInfo("saman", "بانک سامان", "Saman", "056", listOf("621986"), "bank_saman"),
            BankInfo("pasargad", "بانک پاسارگاد", "Pasargad", "057", listOf("502229", "639347"), "bank_pasargad"),
            BankInfo("tejarat", "بانک تجارت", "Tejarat", "018", listOf("627353", "585983"), "bank_tejarat"),
            BankInfo("gardeshgari", "بانک گردشگری", "Tourism", "064", listOf("505416", "505426"), "bank_gardeshgari"),
            BankInfo("sepah", "بانک سپه", "Sepah", "015", listOf("589210"), "bank_sepah"),
            BankInfo("ansar", "بانک سپه", "Sepah", "015", listOf("627381"), "bank_sepah", mergedInto = "sepah", formerNameFa = "انصار سابق"),
        ),
    )

    @Test
    fun binLookup() {
        fun name(bin: String) = (directory.resolvePan(bin + "0000000000") as BankMatch.Known).bank
        assertThat(name("610433").id).isEqualTo("mellat")
        assertThat(name("603799").id).isEqualTo("melli")
        assertThat(name("621986").id).isEqualTo("saman")
        assertThat(name("502229").id).isEqualTo("pasargad")
        assertThat(name("627353").id).isEqualTo("tejarat")
        assertThat(name("585983").id).isEqualTo("tejarat")
        val g = name("505416")
        assertThat(g.id).isEqualTo("gardeshgari")
        val ansar = name("627381")
        assertThat(ansar.mergedInto).isEqualTo("sepah")
        assertThat(ansar.formerNameFa).isEqualTo("انصار سابق")
        assertThat(directory.resolvePan("0000001111111111")).isEqualTo(BankMatch.Unknown)
    }

    @Test
    fun luhnRejectsInvalid() {
        assertThat(CardMath.luhnValid("0000000000000000")).isTrue()
        assertThat(CardMath.luhnValid("6104330000000001")).isFalse()
        assertThat(CardMath.luhnValid("۶۱۰۴۳۳۰۰۰۰۰۰۰۰۰۱")).isFalse()
    }

    @Test
    fun ibanMod97AndShebaLogo() {
        // IR + 24 zeros is invalid; construct a valid IBAN for bank 012 (Mellat).
        val valid = validIranIban("012", "0000000000000000000")
        assertThat(IbanMath.isValidIranIban(valid)).isTrue()
        assertThat(IbanMath.shebaBankCode(valid)).isEqualTo("012")
        val match = directory.resolveIban(valid) as BankMatch.Known
        assertThat(match.bank.id).isEqualTo("mellat")
        val persian = PersianDigits.toPersian(valid)
        assertThat(IbanMath.isValidIranIban(persian.chunked(4).joinToString(" "))).isTrue()
        assertThat(IbanMath.isValidIranIban("IR000000000000000000000000")).isFalse()
    }

    private fun validIranIban(bank3: String, rest19: String): String {
        require(bank3.length == 3 && rest19.length == 19)
        val body = bank3 + rest19
        for (a in 0..9) {
            for (b in 0..9) {
                val candidate = "IR$a$b$body"
                if (IbanMath.iso13616Mod97(candidate) == 1) return candidate
            }
        }
        error("no iban")
    }
}

class TransferPostingTest {
    @Test
    fun atomicTwoLegsPlusOptionalFee() {
        val posting = TransferPoster.post(
            fromAccountId = "cash",
            toAccountId = "bank",
            amountRials = 1_000_000,
            feeRials = 5_000,
            occurredAt = JalaliConverter.toEpochMillisStartOfDay(JalaliYmd(1405, 1, 1)),
            fiscalYearId = "fy",
            note = "جابه‌جایی",
        )
        assertThat(posting.sourceLeg.direction).isEqualTo(Direction.OUT)
        assertThat(posting.destLeg.direction).isEqualTo(Direction.IN)
        assertThat(posting.sourceLeg.transferId).isEqualTo(posting.transfer.id)
        assertThat(posting.destLeg.transferId).isEqualTo(posting.transfer.id)
        val fee = checkNotNull(posting.feeLeg)
        assertThat(fee.categoryId).isEqualTo(SystemCategories.FEE_ID)
        assertThat(fee.amount).isEqualTo(5_000)
        assertThat(posting.allTransactions()).hasSize(3)
        val noFee = TransferPoster.post("cash", "bank", 10, null, posting.transfer.occurredAt, "fy", "")
        assertThat(noFee.feeLeg).isNull()
        assertThat(noFee.allTransactions()).hasSize(2)
    }
}

class BackupOmitsCvvTest {
    @Test
    fun sanitizedZipHasIncludesSecretsFalseAndNoCvvCipher() {
        val dirty = listOf(
            BackupCard(
                id = "c1",
                accountId = "a",
                last4 = "4331",
                bin6 = "610433",
                bankCode = "mellat",
                expiryMonth = 12,
                expiryYear = 1408,
                panCipherId = "pan-secret-bytes",
                cvvCipherId = "cvv-super-secret-ciphertext",
                rememberCvv = true,
            ),
        )
        val clean = BackupSanitizer.sanitizeCards(dirty)
        val manifest = BackupSanitizer.buildManifest(1L, 1)
        assertThat(manifest.includesSecrets).isFalse()
        assertThat(clean.single().cvvCipherId).isNull()
        assertThat(clean.single().panCipherId).isNull()
        assertThat(clean.single().rememberCvv).isFalse()
        val json = Json { encodeDefaults = true }
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(json.encodeToString(manifest).encodeToByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("cards.json"))
            zip.write(json.encodeToString(clean).encodeToByteArray())
            zip.closeEntry()
        }
        val zipBytes = bos.toByteArray()
        BackupSanitizer.assertSafe(manifest, zipBytes, clean)
        val unzipped = BackupSanitizer.unzippedText(zipBytes)
        assertThat(unzipped).doesNotContain("cvv-super-secret-ciphertext")
        assertThat(unzipped).doesNotContain("pan-secret-bytes")
        assertThat(unzipped.replace(" ", "")).contains("\"includesSecrets\":false")
    }
}

class CategoryRulesTest {
    @Test
    fun systemCategoriesCannotBeDeleted() {
        val system = ir.mhajisoft.miniaccountant.domain.ledger.CategoryCatalog.systemCategories().first()
        assertThat(ir.mhajisoft.miniaccountant.domain.ledger.CategoryRules.canDelete(system)).isFalse()
    }

    @Test
    fun customExpenseCategoryIsNotSystem() {
        val custom = ir.mhajisoft.miniaccountant.domain.ledger.CategoryRules.custom(
            name = "قهوه",
            iconKey = "restaurant",
            color = 0xFFE65100,
            kind = ir.mhajisoft.miniaccountant.domain.model.CategoryKind.EXPENSE,
        )
        assertThat(custom.isSystem).isFalse()
        assertThat(ir.mhajisoft.miniaccountant.domain.ledger.CategoryRules.canDelete(custom)).isTrue()
    }

    @Test(expected = IllegalArgumentException::class)
    fun customTransferCategoryRejected() {
        ir.mhajisoft.miniaccountant.domain.ledger.CategoryRules.custom(
            name = "جابه‌جایی سفارشی",
            iconKey = "swap_horiz",
            color = 0xFF78909C,
            kind = ir.mhajisoft.miniaccountant.domain.model.CategoryKind.TRANSFER,
        )
    }
}
