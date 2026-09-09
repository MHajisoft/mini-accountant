package ir.mhajisoft.miniaccountant.ui.theme

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
import ir.mhajisoft.miniaccountant.R

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

val LocalLedgerTones = staticCompositionLocalOf {
    LedgerTones(
        income = Color(0xFF047857),
        onIncome = Color.White,
        expense = Color(0xFFB91C1C),
        onExpense = Color.White,
        debtor = Color(0xFFB45309),
        creditor = Color(0xFF1D4ED8),
    )
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF134E4A),
    secondary = Color(0xFFB45309),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFED7AA),
    onSecondaryContainer = Color(0xFF7C2D12),
    tertiary = Color(0xFF047857),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF064E3B),
    background = Color(0xFFF0FDFA),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF334155),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = Color(0xFFE2E8F0),
    surfaceContainerHighest = Color(0xFFCBD5E1),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFFCBD5E1),
    error = Color(0xFFB91C1C),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    inverseSurface = Color(0xFF1E293B),
    inverseOnSurface = Color(0xFFF8FAFC),
    inversePrimary = Color(0xFF5EEAD4),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5EEAD4),
    onPrimary = Color(0xFF134E4A),
    primaryContainer = Color(0xFF115E59),
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Color(0xFFFBBF24),
    onSecondary = Color(0xFF78350F),
    secondaryContainer = Color(0xFF92400E),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = Color(0xFF6EE7B7),
    onTertiary = Color(0xFF064E3B),
    tertiaryContainer = Color(0xFF065F46),
    onTertiaryContainer = Color(0xFFD1FAE5),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFFCBD5E1),
    surfaceContainerLowest = Color(0xFF020617),
    surfaceContainerLow = Color(0xFF0F172A),
    surfaceContainer = Color(0xFF1E293B),
    surfaceContainerHigh = Color(0xFF334155),
    surfaceContainerHighest = Color(0xFF475569),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFF334155),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF7F1D1D),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    inverseSurface = Color(0xFFE2E8F0),
    inverseOnSurface = Color(0xFF1E293B),
    inversePrimary = Color(0xFF0F766E),
)

private val LightTones = LedgerTones(
    income = Color(0xFF047857),
    onIncome = Color.White,
    expense = Color(0xFFB91C1C),
    onExpense = Color.White,
    debtor = Color(0xFFC2410C),
    creditor = Color(0xFF1D4ED8),
)

private val DarkTones = LedgerTones(
    income = Color(0xFF6EE7B7),
    onIncome = Color(0xFF064E3B),
    expense = Color(0xFFFCA5A5),
    onExpense = Color(0xFF7F1D1D),
    debtor = Color(0xFFFDBA74),
    creditor = Color(0xFF93C5FD),
)

val ColorScheme.ledgerTones: LedgerTones
    @Composable get() = LocalLedgerTones.current

@Composable
fun MiniAccountantTheme(
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
