package ir.mhajisoft.hesabres.domain.crash

import ir.mhajisoft.hesabres.domain.ledger.ComposerRules

/**
 * Pure guards for crash paths that otherwise surface as IllegalArgumentException,
 * IllegalStateException, or an empty navigation back stack.
 */
object SheetBounds {
    /**
     * ModalBottomSheet drops the Expanded anchor when content is 0dp or taller than
     * the window (keyboard padding stacked outside the cap). The returned height is
     * always a positive fraction of the screen, never above it.
     */
    fun maxHeightDp(screenHeightDp: Int): Int {
        val screen = screenHeightDp.coerceAtLeast(1)
        return (screen * 0.85f).toInt().coerceIn(1, screen)
    }
}

object TabStack {
    /**
     * Switch the root tab without ever publishing an empty back stack.
     * NavDisplay crashes if it recomposes between clear() and add().
     */
    fun <T> switchTo(stack: MutableList<T>, tab: T) {
        if (stack.size == 1 && stack[0] == tab) return
        while (stack.size > 1) {
            stack.removeAt(stack.lastIndex)
        }
        if (stack.isEmpty()) {
            stack.add(tab)
            return
        }
        if (stack[0] != tab) stack[0] = tab
    }
}

object BiometricPromptPolicy {
    /**
     * CryptoObject authentication rejects DEVICE_CREDENTIAL (and any weak biometric).
     * Passing both throws IllegalArgumentException inside authenticate().
     */
    fun allowsDeviceCredential(sdkInt: Int, needsCrypto: Boolean): Boolean =
        sdkInt >= 30 && !needsCrypto

    /**
     * Keystore rejects timeout 0 combined with AUTH_DEVICE_CREDENTIAL:
     * "not supported for keys that require user authentication for every use".
     */
    fun cvvPerUseAllowsDeviceCredential(timeoutSec: Int): Boolean = timeoutSec > 0
}

object WriteFailures {
    const val ERR_GENERIC = "ثبت انجام نشد. دوباره تلاش کنید."
    const val ERR_BACKUP = "پشتیبان‌گیری انجام نشد. رمز یا فایل را بررسی کنید."
    const val ERR_RESTORE = "بازیابی انجام نشد. رمز یا فایل پشتیبان نادرست است."
    const val ERR_BIOMETRIC = "قفل دستگاه یا اثر انگشت آماده نیست. از تنظیمات گوشی یک قفل صفحه بگذارید."
    const val ERR_CVV = "ذخیرهٔ رمز دوم روی این دستگاه ممکن نیست."
    const val ERR_NO_ACTIVITY = "این کار از این صفحه ممکن نیست."

    fun map(t: Throwable): String {
        val msg = t.message.orEmpty()
        return when {
            msg.contains("FOREIGN", ignoreCase = true) -> ComposerRules.ERR_ACCOUNT_GONE
            msg.contains("fiscal", ignoreCase = true) -> ComposerRules.ERR_NO_FY
            msg.contains("سال") || msg.any { it in '\u0600'..'\u06FF' } -> msg
            else -> ERR_GENERIC
        }
    }
}

object DomainLists {
    /** One poison row must not fail the whole Room flow and crash startup. */
    fun <T, R> safeMap(items: List<T>, map: (T) -> R): List<R> =
        items.mapNotNull { item -> runCatching { map(item) }.getOrNull() }
}

object ArgbColors {
    fun toArgbInt(color: Long): Int = (color and 0xFFFFFFFFL).toInt()
}
