package com.example.genesis.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = MihonBlue,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = MihonBackground,
    surface = MihonDark,
    onPrimary = MihonBackground,
    onSecondary = MihonBackground,
    onTertiary = MihonBackground,
    onBackground = androidx.compose.ui.graphics.Color.White,
    onSurface = androidx.compose.ui.graphics.Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun Theme(
    selectedTheme: String = "Original",
    isPureBlack: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when (selectedTheme) {
        "Dinámico" -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    dynamicDarkColorScheme(context)
                } catch (e: Exception) {
                    // Fallback if dynamic colors system service is missing on the container OS
                    DarkColorScheme.copy(
                        background = if (isPureBlack) androidx.compose.ui.graphics.Color.Black else MihonSurface
                    )
                }
            } else {
                DarkColorScheme.copy(
                    background = if (isPureBlack) androidx.compose.ui.graphics.Color.Black else MihonSurface
                )
            }
        }
        "Miaupuchino" -> {
            darkColorScheme(
                primary = androidx.compose.ui.graphics.Color(0xFFD7CCC8), // Light café au lait
                secondary = androidx.compose.ui.graphics.Color(0xFFB0BEC5),
                tertiary = androidx.compose.ui.graphics.Color(0xFFFFCC80),
                background = if (isPureBlack) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color(0xFF1F1816),
                surface = androidx.compose.ui.graphics.Color(0xFF2D2421),
                onPrimary = androidx.compose.ui.graphics.Color(0xFF1F1816),
                onSecondary = androidx.compose.ui.graphics.Color(0xFF1F1816),
                onTertiary = androidx.compose.ui.graphics.Color(0xFF1F1816),
                onBackground = androidx.compose.ui.graphics.Color(0xFFFFF3E0),
                onSurface = androidx.compose.ui.graphics.Color(0xFFFFF3E0),
            )
        }
        else -> { // "Original"
            DarkColorScheme.copy(
                background = if (isPureBlack) androidx.compose.ui.graphics.Color.Black else MihonSurface
            )
        }
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            try {
                var currentContext = view.context
                var activity: Activity? = null
                while (currentContext is android.content.ContextWrapper) {
                    if (currentContext is Activity) {
                        activity = currentContext
                        break
                    }
                    currentContext = currentContext.baseContext
                }
                val window = activity?.window
                if (window != null) {
                    window.statusBarColor = colorScheme.background.toArgb()
                    window.navigationBarColor = colorScheme.background.toArgb()
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                }
            } catch (e: Throwable) {
                android.util.Log.e("ThemeInit", "Failed to set window status bar color safely", e)
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
