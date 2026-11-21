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
    secondaryContainer = Color(0xFF5A3F48),
    background = Color(0xFF191114),
    surface = Color(0xFF191114),
)

/** Material 3 Expressive: skema warna dinamis (Android 12+), motion expressive, bentuk MaterialShapes. */
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
