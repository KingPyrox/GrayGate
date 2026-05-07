package com.graygate.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Brand accent colors for the gate metaphor
val GateColorSide = Color(0xFF4FC3F7)   // vibrant light blue -- "color" side
val GateGraySide = Color(0xFF78909C)    // blue-grey -- "grayscale" side
val BrandDarkSlate = Color(0xFF37474F)  // primary brand dark

private val LightColors = lightColorScheme(
    primary = Color(0xFF37474F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFD8DC),
    onPrimaryContainer = Color(0xFF1C313A),
    secondary = Color(0xFF546E7A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB0BEC5),
    onSecondaryContainer = Color(0xFF263238),
    tertiary = Color(0xFF78909C),
    surface = Color(0xFFF5F7F8),
    onSurface = Color(0xFF212121),
    surfaceVariant = Color(0xFFECEFF1),
    onSurfaceVariant = Color(0xFF455A64),
    outline = Color(0xFF90A4AE),
    background = Color(0xFFF0F4F5),
    onBackground = Color(0xFF212121),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF90A4AE),
    onPrimary = Color(0xFF1C313A),
    primaryContainer = Color(0xFF37474F),
    onPrimaryContainer = Color(0xFFCFD8DC),
    secondary = Color(0xFF78909C),
    onSecondary = Color(0xFF263238),
    secondaryContainer = Color(0xFF455A64),
    onSecondaryContainer = Color(0xFFB0BEC5),
    tertiary = Color(0xFFB0BEC5),
    surface = Color(0xFF1C262B),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF263238),
    onSurfaceVariant = Color(0xFFB0BEC5),
    outline = Color(0xFF546E7A),
    background = Color(0xFF151E23),
    onBackground = Color(0xFFE0E0E0),
)

private val GrayGateTypography = Typography(
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = (-0.5).sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 0.5.sp
    ),
)

@Composable
fun GrayGateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GrayGateTypography,
        content = content
    )
}
