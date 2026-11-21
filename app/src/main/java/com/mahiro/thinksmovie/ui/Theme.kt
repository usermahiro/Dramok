package com.mahiro.thinksmovie.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val BrandDark = darkColorScheme(
    primary = Color(0xFFFFB1C8),
    onPrimary = Color(0xFF650033),
    primaryContainer = Color(0xFF8E0D4B),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = Color(0xFFE2BDC7),
    onSecondary = Color(0xFF422E37),
    secondaryContainer = Color(0xFF5A3F48),
    onSecondaryContainer = Color(0xFFFFD9E2),
    tertiary = Color(0xFFFFD2A6),
    onTertiary = Color(0xFF5B3A00),
    tertiaryContainer = Color(0xFF7A5200),
    onTertiaryContainer = Color(0xFFFFE3C2),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF191114),
    onBackground = Color(0xFFF5DEE7),
    surface = Color(0xFF191114),
    onSurface = Color(0xFFF5DEE7),
    surfaceVariant = Color(0xFF33262B),
    onSurfaceVariant = Color(0xFFD8C2CA),
    outline = Color(0xFF9F8B93),
    outlineVariant = Color(0xFF4F3B44),
)

/** Material 3 Expressive: warna dinamis (Android 12+), motion expressive, bentuk MaterialShapes. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThinksMovieTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> BrandDark
        else -> expressiveLightColorScheme()
    }
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
