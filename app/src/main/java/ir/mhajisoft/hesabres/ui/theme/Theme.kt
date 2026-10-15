package ir.mhajisoft.hesabres.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import ir.mhajisoft.hesabres.domain.crash.ArgbColors
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.mhajisoft.hesabres.R

/** ARGB longs are not Compose ColorLongs; the Long constructor can throw on the color-space bits. */
fun argbColor(color: Long): Color = Color(ArgbColors.toArgbInt(color))

val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
)

val FinanceShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

data class LedgerTones(
    val income: Color,
    val onIncome: Color,
    val expense: Color,
    val onExpense: Color,
    val debtor: Color,
    val creditor: Color,
)

val PoolakiIndigo = Color(0xFF5E63B6)
val PoolakiIndigoDeep = Color(0xFF4A4F9E)
val PoolakiKeypad = Color(0xFF7B80D6)
val PoolakiBackground = Color(0xFFEFEFF4)
val PoolakiPeach = Color(0xFFFBE3C4)
val PoolakiPeachInk = Color(0xFF8A5A2B)
val PoolakiNegative = Color(0xFFD9534F)
val PoolakiMuted = Color(0xFF8A8A8A)
val PoolakiChip = Color(0xFFECEDFB)
val PoolakiBarMuted = Color(0xFFE4E4E4)
val PoolakiInk = Color(0xFF222222)

val BrandBlue = Color(0xFF2563EB)
val BrandDeepBlue = Color(0xFF1D4ED8)
val BrandTeal = Color(0xFF10B981)
val BrandGreen = Color(0xFF84CC16)
val BrandSoftBackground = Color(0xFFE0F2FE)
val BrandOffWhite = Color(0xFFF1F5F9)

val LocalLedgerTones = staticCompositionLocalOf {
    LedgerTones(
        income = Color(0xFF047857),
        onIncome = Color.White,
        expense = Color(0xFFB91C1C),
        onExpense = Color.White,
        debtor = Color(0xFF9A3412),
        creditor = BrandDeepBlue,
    )
}

private val LightColors = lightColorScheme(
    primary = PoolakiIndigo,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = PoolakiChip,
    onPrimaryContainer = PoolakiIndigoDeep,
    secondary = BrandTeal,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = BrandGreen,
    onTertiary = Color(0xFF1A2E05),
    tertiaryContainer = Color(0xFFECFCCB),
    onTertiaryContainer = Color(0xFF3F6212),
    background = PoolakiBackground,
    onBackground = PoolakiInk,
    surface = Color(0xFFFFFFFF),
    onSurface = PoolakiInk,
    surfaceVariant = Color(0xFFD6E6F2),
    onSurfaceVariant = PoolakiMuted,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFE8F4FC),
    surfaceContainerHigh = Color(0xFFD7EAF8),
    surfaceContainerHighest = Color(0xFFC7E0F4),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF64748B),
    error = PoolakiNegative,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE4E2),
    onErrorContainer = Color(0xFF7A271A),
    inverseSurface = Color(0xFF1E293B),
    inverseOnSurface = BrandOffWhite,
    inversePrimary = Color(0xFF93C5FD),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB7BAEA),
    onPrimary = Color(0xFF1C2048),
    primaryContainer = Color(0xFF3A3F86),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF6EE7B7),
    onSecondary = Color(0xFF064E3B),
    secondaryContainer = Color(0xFF047857),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = Color(0xFFD9F99D),
    onTertiary = Color(0xFF1A2E05),
    tertiaryContainer = Color(0xFF4D7C0F),
    onTertiaryContainer = Color(0xFFECFCCB),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1E3A5F),
    onSurfaceVariant = Color(0xFFCBD5E1),
    surfaceContainerLowest = Color(0xFF070B14),
    surfaceContainerLow = Color(0xFF111827),
    surfaceContainer = Color(0xFF172033),
    surfaceContainerHigh = Color(0xFF1E293B),
    surfaceContainerHighest = Color(0xFF273549),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFF64748B),
    error = Color(0xFFF97066),
    onError = Color(0xFF7A271A),
    errorContainer = Color(0xFF7A271A),
    onErrorContainer = Color(0xFFFEE4E2),
    inverseSurface = Color(0xFFE2E8F0),
    inverseOnSurface = Color(0xFF111827),
    inversePrimary = BrandBlue,
)

private val LightTones = LedgerTones(
    income = Color(0xFF047857),
    onIncome = Color.White,
    expense = PoolakiNegative,
    onExpense = Color.White,
    debtor = Color(0xFF9A3412),
    creditor = BrandDeepBlue,
)

private val DarkTones = LedgerTones(
    income = Color(0xFF34D399),
    onIncome = Color(0xFF064E3B),
    expense = Color(0xFFFCA5A5),
    onExpense = Color(0xFF7F1D1D),
    debtor = Color(0xFFFDBA74),
    creditor = Color(0xFF93C5FD),
)

val ColorScheme.ledgerTones: LedgerTones
    @Composable get() = LocalLedgerTones.current

@Composable
fun HesabresTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val typography = MaterialTheme.typography.run {
        copy(
            displayLarge = displayLarge.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, lineHeight = 64.sp),
            displayMedium = displayMedium.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, lineHeight = 52.sp),
            displaySmall = displaySmall.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, lineHeight = 44.sp),
            headlineLarge = headlineLarge.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, lineHeight = 40.sp),
            headlineMedium = headlineMedium.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, lineHeight = 36.sp),
            headlineSmall = headlineSmall.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, lineHeight = 32.sp),
            titleLarge = titleLarge.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
            titleMedium = titleMedium.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp),
            titleSmall = titleSmall.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Medium, lineHeight = 22.sp),
            bodyLarge = bodyLarge.copy(fontFamily = Vazirmatn, lineHeight = 26.sp),
            bodyMedium = bodyMedium.copy(fontFamily = Vazirmatn, lineHeight = 22.sp),
            bodySmall = bodySmall.copy(fontFamily = Vazirmatn, lineHeight = 18.sp),
            labelLarge = labelLarge.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Medium, lineHeight = 20.sp),
            labelMedium = labelMedium.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Medium, lineHeight = 18.sp),
            labelSmall = labelSmall.copy(fontFamily = Vazirmatn, lineHeight = 16.sp),
        )
    }
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalLedgerTones provides if (darkTheme) DarkTones else LightTones,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = typography,
            shapes = FinanceShapes,
            content = content,
        )
    }
}
