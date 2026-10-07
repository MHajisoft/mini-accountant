package ir.mhajisoft.hesabres

import com.google.common.truth.Truth.assertThat
import ir.mhajisoft.hesabres.domain.crash.ArgbColors
import ir.mhajisoft.hesabres.domain.crash.BiometricPromptPolicy
import ir.mhajisoft.hesabres.domain.crash.DomainLists
import ir.mhajisoft.hesabres.domain.crash.SheetBounds
import ir.mhajisoft.hesabres.domain.crash.TabStack
import ir.mhajisoft.hesabres.domain.crash.WriteFailures
import ir.mhajisoft.hesabres.domain.ledger.ComposerRules
import ir.mhajisoft.hesabres.domain.model.Direction
import ir.mhajisoft.hesabres.domain.model.LedgerTransaction
import org.junit.Test

class SheetBoundsTest {
    @Test
    fun maxHeightStaysInsideTheScreen() {
        listOf(1, 200, 320, 640, 800, 1280).forEach { screen ->
            val max = SheetBounds.maxHeightDp(screen)
            assertThat(max).isAtLeast(1)
            assertThat(max).isAtMost(screen)
        }
    }

    @Test
    fun zeroOrNegativeScreenDoesNotProduceZeroHeight() {
        assertThat(SheetBounds.maxHeightDp(0)).isAtLeast(1)
        assertThat(SheetBounds.maxHeightDp(-40)).isAtLeast(1)
    }
}

class TabStackTest {
    @Test
    fun switchNeverEmptiesTheStack() {
        val stack = mutableListOf("home", "person", "edit")
        val seen = mutableListOf<List<String>>()
        val original = stack.toMutableList()
        // Observe every mutation the helper performs.
        val watching = object : MutableList<String> by original {
            override fun removeAt(index: Int): String {
                val removed = original.removeAt(index)
                seen += original.toList()
                return removed
            }

            override fun add(element: String): Boolean {
                val added = original.add(element)
                seen += original.toList()
                return added
            }

            override fun set(index: Int, element: String): String {
                val previous = original.set(index, element)
                seen += original.toList()
                return previous
            }

            override val size: Int get() = original.size
            override fun get(index: Int): String = original[index]
            override fun isEmpty(): Boolean = original.isEmpty()
        }
        TabStack.switchTo(watching, "reports")
        assertThat(watching).containsExactly("reports")
        assertThat(seen).isNotEmpty()
        assertThat(seen.all { it.isNotEmpty() }).isTrue()
    }

    @Test
    fun emptyStackGainsTheTab() {
        val stack = mutableListOf<String>()
        TabStack.switchTo(stack, "home")
        assertThat(stack).containsExactly("home")
    }

    @Test
    fun sameRootIsLeftAlone() {
        val stack = mutableListOf("home")
        TabStack.switchTo(stack, "home")
        assertThat(stack).containsExactly("home")
    }
}

class BiometricPromptPolicyTest {
    @Test
    fun cryptoPromptNeverAllowsDeviceCredential() {
        assertThat(BiometricPromptPolicy.allowsDeviceCredential(sdkInt = 33, needsCrypto = true)).isFalse()
        assertThat(BiometricPromptPolicy.allowsDeviceCredential(sdkInt = 28, needsCrypto = true)).isFalse()
    }

    @Test
    fun deviceCredentialOnlyForNonCryptoOnApi30() {
        assertThat(BiometricPromptPolicy.allowsDeviceCredential(sdkInt = 30, needsCrypto = false)).isTrue()
        assertThat(BiometricPromptPolicy.allowsDeviceCredential(sdkInt = 29, needsCrypto = false)).isFalse()
    }

    @Test
    fun perUseCvvKeyCannotIncludeDeviceCredential() {
        assertThat(BiometricPromptPolicy.cvvPerUseAllowsDeviceCredential(timeoutSec = 0)).isFalse()
        assertThat(BiometricPromptPolicy.cvvPerUseAllowsDeviceCredential(timeoutSec = 30)).isTrue()
    }
}

class WriteFailuresTest {
    @Test
    fun foreignKeyAndMissingYearStayPersian() {
        assertThat(WriteFailures.map(IllegalStateException("FOREIGN KEY constraint failed")))
            .isEqualTo(ComposerRules.ERR_ACCOUNT_GONE)
        assertThat(WriteFailures.map(IllegalStateException("no fiscal year")))
            .isEqualTo(ComposerRules.ERR_NO_FY)
        assertThat(WriteFailures.map(IllegalStateException("سال مالی جاری پیدا نشد")))
            .isEqualTo("سال مالی جاری پیدا نشد")
    }

    @Test
    fun unknownFailuresDoNotLeakEnglish() {
        assertThat(WriteFailures.map(IllegalStateException("database is locked")))
            .isEqualTo(WriteFailures.ERR_GENERIC)
    }
}

class DomainListsTest {
    @Test
    fun poisonTransactionDoesNotDropTheRest() {
        val good = LedgerTransaction(
            id = "ok",
            accountId = "a",
            categoryId = null,
            personId = null,
            amount = 10,
            direction = Direction.OUT,
            note = "",
            occurredAt = 1,
            jalaliYear = 1405,
            jalaliMonth = 1,
            jalaliDay = 1,
            fiscalYearId = "fy",
            transferId = null,
            createdAt = 1,
        )
        val rows = listOf("bad", "ok")
        val mapped = DomainLists.safeMap(rows) { id ->
            if (id == "bad") {
                good.copy(id = id, amount = -1)
            } else {
                good.copy(id = id)
            }
        }
        assertThat(mapped.map { it.id }).containsExactly("ok")
    }
}

class ArgbColorsTest {
    @Test
    fun argbLongKeepsEightHexDigits() {
        assertThat(ArgbColors.toArgbInt(0xFF0B6E4F)).isEqualTo(0xFF0B6E4F.toInt())
        assertThat(ArgbColors.toArgbInt(0xFF0B6E4FL)).isEqualTo(0xFF0B6E4F.toInt())
    }
}
