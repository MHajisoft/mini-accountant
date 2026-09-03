package ir.mhajisoft.miniaccountant.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import ir.mhajisoft.miniaccountant.R

val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    secondary = Color(0xFFB45309),
    tertiary = Color(0xFF1D4ED8),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    error = Color(0xFFB91C1C),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5EEAD4),
    onPrimary = Color(0xFF134E4A),
    primaryContainer = Color(0xFF115E59),
    secondary = Color(0xFFFBBF24),
    tertiary = Color(0xFF93C5FD),
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    error = Color(0xFFFCA5A5),
)

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
            titleLarge = titleLarge.copy(fontFamily = Vazirmatn),
            titleMedium = titleMedium.copy(fontFamily = Vazirmatn),
            titleSmall = titleSmall.copy(fontFamily = Vazirmatn),
            bodyLarge = bodyLarge.copy(fontFamily = Vazirmatn),
            bodyMedium = bodyMedium.copy(fontFamily = Vazirmatn),
            bodySmall = bodySmall.copy(fontFamily = Vazirmatn),
            labelLarge = labelLarge.copy(fontFamily = Vazirmatn),
            labelMedium = labelMedium.copy(fontFamily = Vazirmatn),
            labelSmall = labelSmall.copy(fontFamily = Vazirmatn),
        )
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = typography,
            content = content,
        )
    }
}
