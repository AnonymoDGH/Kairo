package io.github.anonymodgh.kairo.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Geist = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold),
)

val GeistMono = FontFamily(
    Font(R.font.geist_mono_regular, FontWeight.Normal),
    Font(R.font.geist_mono_medium, FontWeight.Medium),
)

private val KairoTypography = Typography(
    displaySmall = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 17.sp),
    bodyLarge = TextStyle(fontFamily = Geist, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Geist, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = GeistMono, fontWeight = FontWeight.Medium, fontSize = 12.sp),
)

private val KairoDarkScheme = darkColorScheme(
    primary = KairoColors.Cyan,
    secondary = KairoColors.Violet,
    tertiary = KairoColors.Magenta,
    background = KairoColors.Background,
    surface = KairoColors.Background,
    onBackground = KairoColors.TextPrimary,
    onSurface = KairoColors.TextPrimary,
    onSurfaceVariant = KairoColors.TextSecondary,
    error = KairoColors.Error,
)

@Composable
fun KairoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = KairoDarkScheme, typography = KairoTypography, content = content)
}
