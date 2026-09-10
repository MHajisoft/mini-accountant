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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ir.mhajisoft.hesabres.R

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

val BrandBlue = Color(0xFF2563EB)
val BrandDeepBlue = Color(0xFF1D4ED8)
val BrandTeal = Color(0xFF10B981)
val BrandGreen = Color(0xFF84CC16)
val BrandSoftBackground = Color(0xFFE0F2FE)
val BrandOffWhite = Color(0xFFF1F5F9)

val LocalLedgerTones = staticCompositionLocalOf {
    LedgerTones(
        income = BrandTeal,
        onIncome = Color.White,
        expense = Color(0xFFB91C1C),
        onExpense = Color.White,
        debtor = Color(0xFFC2410C),
        creditor = BrandDeepBlue,
    )
}

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = BrandTeal,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = BrandGreen,
    onTertiary = Color(0xFF1A2E05),
    tertiaryContainer = Color(0xFFECFCCB),
    onTertiaryContainer = Color(0xFF3F6212),
    background = BrandSoftBackground,
    onBackground = Color(0xFF0F172A),
    surface = BrandOffWhite,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFD5E8F6),
    onSurfaceVariant = Color(0xFF334155),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFE8F4FC),
    surfaceContainerHigh = Color(0xFFD7EAF8),
    surfaceContainerHighest = Color(0xFFC7E0F4),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFFB8D4EA),
    error = Color(0xFFB42318),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE4E2),
    onErrorContainer = Color(0xFF7A271A),
    inverseSurface = Color(0xFF1E293B),
    inverseOnSurface = BrandOffWhite,
    inversePrimary = Color(0xFF93C5FD),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF93C5FD),
    onPrimary = Color(0xFF0B1F4A),
    primaryContainer = BrandDeepBlue,
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
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFF334155),
    error = Color(0xFFF97066),
    onError = Color(0xFF7A271A),
    errorContainer = Color(0xFF7A271A),
    onErrorContainer = Color(0xFFFEE4E2),
    inverseSurface = Color(0xFFE2E8F0),
    inverseOnSurface = Color(0xFF111827),
    inversePrimary = BrandBlue,
)

private val LightTones = LedgerTones(
    income = BrandTeal,
    onIncome = Color.White,
    expense = Color(0xFFB91C1C),
    onExpense = Color.White,
    debtor = Color(0xFFC2410C),
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
            displayLarge = displayLarge.copy(fontFamily = Vazirmatn),
            displayMedium = displayMedium.copy(fontFamily = Vazirmatn),
            displaySmall = displaySmall.copy(fontFamily = Vazirmatn),
            headlineLarge = headlineLarge.copy(fontFamily = Vazirmatn),
            headlineMedium = headlineMedium.copy(fontFamily = Vazirmatn),
            headlineSmall = headlineSmall.copy(fontFamily = Vazirmatn),
            titleLarge = titleLarge.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.SemiBold),
            titleMedium = titleMedium.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.SemiBold),
            titleSmall = titleSmall.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Medium),
            bodyLarge = bodyLarge.copy(fontFamily = Vazirmatn),
            bodyMedium = bodyMedium.copy(fontFamily = Vazirmatn),
            bodySmall = bodySmall.copy(fontFamily = Vazirmatn),
            labelLarge = labelLarge.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Medium),
            labelMedium = labelMedium.copy(fontFamily = Vazirmatn),
            labelSmall = labelSmall.copy(fontFamily = Vazirmatn),
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
