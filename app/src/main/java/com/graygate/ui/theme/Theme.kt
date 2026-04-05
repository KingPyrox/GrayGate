package com.graygate.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

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
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF212121),
    surfaceVariant = Color(0xFFECEFF1),
    onSurfaceVariant = Color(0xFF455A64),
    outline = Color(0xFF90A4AE),
    background = Color(0xFFFAFAFA),
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
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF263238),
    onSurfaceVariant = Color(0xFFB0BEC5),
    outline = Color(0xFF546E7A),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
)

@Composable
fun GrayGateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
