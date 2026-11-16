package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class AppThemeMode(val code: String, val label: String) {
    SYSTEM("SYSTEM", "System"),
    LIGHT("LIGHT", "Light"),
    DARK("DARK", "Dark");

    companion object {
        fun fromCode(code: String?): AppThemeMode =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: SYSTEM
    }
}

@Immutable
data class LinkUpExtendedColors(
    val isDark: Boolean,
    val accentText: Color,
    val accentBlue: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    val successBg: Color,
    val successFg: Color,
    val warningBg: Color,
    val warningFg: Color,
    val errorBg: Color,
    val errorFg: Color,
    val featureCardBg: Color,
    val featureCardBorder: Color
)

private val LightExtendedColors = LinkUpExtendedColors(
    isDark = false,
    accentText = NavyPrimary,
    accentBlue = ElectricBlue,
    accentContainer = ElectricBlueContainer,
    onAccentContainer = ElectricBlueDark,
    successBg = VerifiedEmeraldBg,
    successFg = VerifiedEmerald,
    warningBg = WarningAmberBg,
    warningFg = WarningAmber,
    errorBg = ErrorCrimsonBg,
    errorFg = ErrorCrimson,
    featureCardBg = NavyPrimary,
    featureCardBorder = Color.Transparent
)

private val DarkExtendedColors = LinkUpExtendedColors(
    isDark = true,
    accentText = TextPrimaryDark,
    accentBlue = ElectricBlueBright,
    accentContainer = ElectricBlueContainerDark,
    onAccentContainer = ElectricBlueLightText,
    successBg = VerifiedEmeraldBgDark,
    successFg = VerifiedEmeraldLight,
    warningBg = WarningAmberBgDark,
    warningFg = WarningAmberLight,
    errorBg = ErrorCrimsonBgDark,
    errorFg = ErrorCrimsonLight,
    featureCardBg = SurfaceVariantDark,
    featureCardBorder = OutlineDark
)

val LocalLinkUpColors = staticCompositionLocalOf { LightExtendedColors }

val MaterialTheme.linkUpColors: LinkUpExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalLinkUpColors.current

private val LinkUpLightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = Color.White,
    primaryContainer = ElectricBlueContainer,
    onPrimaryContainer = NavyPrimary,
    secondary = ElectricBlue,
    onSecondary = Color.White,
    secondaryContainer = CyanContainer,
    onSecondaryContainer = ElectricBlueDark,
    tertiary = CyanAccent,
    onTertiary = Color.White,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceWhite,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight,
    outlineVariant = OutlineSubtle,
    error = ErrorCrimson,
    onError = Color.White,
    errorContainer = ErrorCrimsonBg,
    onErrorContainer = ErrorCrimson
)

private val LinkUpDarkColorScheme = darkColorScheme(
    primary = ElectricBlueBright,
    onPrimary = NavyPrimaryDark,
    primaryContainer = ElectricBlueContainerDark,
    onPrimaryContainer = Color.White,
    secondary = ElectricBlue,
    onSecondary = Color.White,
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = ElectricBlueBright,
    tertiary = CyanAccent,
    onTertiary = Color.White,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark,
    outlineVariant = OutlineSubtleDark,
    error = ErrorCrimsonLight,
    onError = NavyPrimaryDark,
    errorContainer = ErrorCrimsonBgDark,
    onErrorContainer = Color(0xFFFECACA)
)

val LinkUpShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun LinkUpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) LinkUpDarkColorScheme else LinkUpLightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    CompositionLocalProvider(LocalLinkUpColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LinkUpTypography,
            shapes = LinkUpShapes,
            content = content
        )
    }
}
