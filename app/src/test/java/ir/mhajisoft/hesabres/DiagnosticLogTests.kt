package ir.mhajisoft.hesabres

import com.google.common.truth.Truth.assertThat
import ir.mhajisoft.hesabres.diag.BreadcrumbRing
import ir.mhajisoft.hesabres.diag.CrashText
import ir.mhajisoft.hesabres.diag.LogRedactor
import ir.mhajisoft.hesabres.diag.LogStore
import org.junit.Test
import java.io.File
import java.nio.file.Files

class LogRedactorTest {
    @Test
    fun redactsCardsIbansCvvPhonesEmailsAndNames() {
        val raw = """
            card 6104337812345678
            spaced 6104-3378-1234-5678
            persian ۶۱۰۴۳۳۷۸۱۲۳۴۵۶۷۸
            iban IR820540102680020817909002
            sheba 820540102680020817909002
            mail ali@example.com
            phone 09121234567
            cvv: 123
            رمز دوم: ۴۵۶
            name: علی رضایی
            نام: سارا
            "holderName":"Sara Ahmadi"
            at ir.mhajisoft.hesabres.ui.AppRoot.kt:266
            ثبت انجام نشد
        """.trimIndent()
        val clean = LogRedactor.redact(raw)
        assertThat(clean).doesNotContain("6104337812345678")
        assertThat(clean).doesNotContain("6104-3378-1234-5678")
        assertThat(clean).doesNotContain("۶۱۰۴۳۳۷۸۱۲۳۴۵۶۷۸")
        assertThat(clean).doesNotContain("IR820540102680020817909002")
        assertThat(clean).doesNotContain("820540102680020817909002")
        assertThat(clean).doesNotContain("ali@example.com")
        assertThat(clean).doesNotContain("09121234567")
        assertThat(clean).doesNotContain("123")
        assertThat(clean).doesNotContain("۴۵۶")
        assertThat(clean).doesNotContain("علی رضایی")
        assertThat(clean).doesNotContain("سارا")
        assertThat(clean).doesNotContain("Sara Ahmadi")
        assertThat(clean).contains("[card]")
        assertThat(clean).contains("IR[redacted]")
        assertThat(clean).contains("[iban]")
        assertThat(clean).contains("[email]")
        assertThat(clean).contains("[phone]")
        assertThat(clean).contains("AppRoot.kt:266")
        assertThat(clean).contains("ثبت انجام نشد")
    }
}

class CrashTextTest {
    @Test
    fun crashIncludesThreadTimestampAndCauseChain() {
        val root = IllegalStateException("outer")
        root.initCause(IllegalArgumentException("inner"))
        val text = CrashText.format(
            timestamp = "2026-10-13T02:49:00+00:00",
            threadName = "main",
            error = root,
            breadcrumbs = listOf("nav RouteHome"),
        )
        assertThat(text).contains("thread=main")
        assertThat(text).contains("2026-10-13T02:49:00+00:00")
        assertThat(text).contains("nav RouteHome")
        assertThat(text).contains("Caused by:")
        assertThat(text).contains("IllegalArgumentException")
        assertThat(text).contains("IllegalStateException")
    }
}

class BreadcrumbRingTest {
    @Test
    fun dropsOldestWhenFull() {
        val ring = BreadcrumbRing(3)
        ring.add("a")
        ring.add("b")
        ring.add("c")
        ring.add("d")
        assertThat(ring.snapshot()).containsExactly("b", "c", "d").inOrder()
    }
}

class LogStoreTest {
    @Test
    fun keepsNewestAndDeletesOldestPastTheCap() {
        val dir = Files.createTempDirectory("hesabres-logs").toFile()
        var now = 1_700_000_000_000L
        val store = LogStore(
            directory = dir,
            maxTotalBytes = 300,
            rotateFileBytes = 100,
            clock = { now },
        )
        store.append("a".repeat(120))
        now += 60_000
        store.append("b".repeat(120))
        now += 60_000
        store.append("END".repeat(40))
        val files = store.files()
        assertThat(files).isNotEmpty()
        assertThat(files.sumOf { it.length() }).isAtMost(300)
        val combined = files.joinToString("\n") { it.readText() }
        assertThat(combined).contains("END")
        assertThat(combined).doesNotContain("a".repeat(120))
        dir.deleteRecursively()
    }

    @Test
    fun singleOversizedFileKeepsTheTail() {
        val dir = File(Files.createTempDirectory("hesabres-one").toFile(), "logs")
        val store = LogStore(
            directory = dir,
            maxTotalBytes = 200,
            rotateFileBytes = 10_000,
        )
        store.append("HEAD".repeat(80) + "TAILMARKER")
        val text = store.files().single().readText()
        assertThat(text.length).isAtMost(200)
        assertThat(text).contains("TAILMARKER")
        assertThat(text).doesNotContain("HEAD".repeat(80))
        dir.deleteRecursively()
    }
}
