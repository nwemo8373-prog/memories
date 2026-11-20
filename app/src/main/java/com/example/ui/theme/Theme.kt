package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val NegmDarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = ObsidianBlack,
    primaryContainer = Color(0xFF0A2A4A),
    onPrimaryContainer = ElectricBlue,
    secondary = CyberViolet,
    onSecondary = TextPrimary,
    secondaryContainer = Color(0xFF28194E),
    onSecondaryContainer = NeonViolet,
    tertiary = NeonMint,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = TextPrimary,
    surface = MidnightSurface,
    onSurface = TextPrimary,
    surfaceVariant = DeepCardSurface,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorderLight,
    outlineVariant = GlassDivider,
    error = CrimsonAlert,
    onError = TextPrimary
)

private val NegmShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp)
)

val LocalAppLanguage = staticCompositionLocalOf { "ar" }

@Composable
fun NegmGestureTheme(
    language: String = "ar",
    content: @Composable () -> Unit
) {
    val layoutDirection = if (language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
    val strings = if (language == "ar") ArabicStrings else EnglishStrings

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalAppLanguage provides language,
        LocalAppStrings provides strings
    ) {
        MaterialTheme(
            colorScheme = NegmDarkColorScheme,
            typography = Typography,
            shapes = NegmShapes,
            content = content
        )
    }
}
