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
    primary = Color(0xFF0B6E4F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8F3E7),
    onPrimaryContainer = Color(0xFF083D2C),
    secondary = Color(0xFF1F3A5F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E3F5),
    onSecondaryContainer = Color(0xFF13243C),
    tertiary = Color(0xFF9A3412),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE4D6),
    onTertiaryContainer = Color(0xFF5C1A08),
    background = Color(0xFFF6F4EF),
    onBackground = Color(0xFF1A1814),
    surface = Color(0xFFFFFCF7),
    onSurface = Color(0xFF1A1814),
    surfaceVariant = Color(0xFFE8E2D6),
    onSurfaceVariant = Color(0xFF4A453C),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3EFE7),
    surfaceContainer = Color(0xFFEDE8DE),
    surfaceContainerHigh = Color(0xFFE4DED2),
    surfaceContainerHighest = Color(0xFFD9D2C4),
    outline = Color(0xFF7A7366),
    outlineVariant = Color(0xFFCDC6B8),
    error = Color(0xFFB42318),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE4E2),
    onErrorContainer = Color(0xFF7A271A),
    inverseSurface = Color(0xFF2A2722),
    inverseOnSurface = Color(0xFFF6F4EF),
    inversePrimary = Color(0xFF7DCEA0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7DCEA0),
    onPrimary = Color(0xFF083D2C),
    primaryContainer = Color(0xFF0B6E4F),
    onPrimaryContainer = Color(0xFFD8F3E7),
    secondary = Color(0xFFA8C4EA),
    onSecondary = Color(0xFF13243C),
    secondaryContainer = Color(0xFF1F3A5F),
    onSecondaryContainer = Color(0xFFD7E3F5),
    tertiary = Color(0xFFFDBA74),
    onTertiary = Color(0xFF5C1A08),
    tertiaryContainer = Color(0xFF9A3412),
    onTertiaryContainer = Color(0xFFFFE4D6),
    background = Color(0xFF14120E),
    onBackground = Color(0xFFEDE8DE),
    surface = Color(0xFF1C1A16),
    onSurface = Color(0xFFEDE8DE),
    surfaceVariant = Color(0xFF3A362F),
    onSurfaceVariant = Color(0xFFCBC4B6),
    surfaceContainerLowest = Color(0xFF0E0C0A),
    surfaceContainerLow = Color(0xFF1C1A16),
    surfaceContainer = Color(0xFF26231E),
    surfaceContainerHigh = Color(0xFF312D27),
    surfaceContainerHighest = Color(0xFF3D3831),
    outline = Color(0xFFA39C8F),
    outlineVariant = Color(0xFF4A453C),
    error = Color(0xFFF97066),
    onError = Color(0xFF7A271A),
    errorContainer = Color(0xFF7A271A),
    onErrorContainer = Color(0xFFFEE4E2),
    inverseSurface = Color(0xFFEDE8DE),
    inverseOnSurface = Color(0xFF1C1A16),
    inversePrimary = Color(0xFF0B6E4F),
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
