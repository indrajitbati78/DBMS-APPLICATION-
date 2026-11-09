package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = WarmPrimaryDark,
    onPrimary = WarmOnPrimaryDark,
    primaryContainer = WarmPrimaryContainerDark,
    onPrimaryContainer = WarmOnPrimaryContainerDark,
    secondary = WarmSecondaryDark,
    onSecondary = WarmOnSecondaryDark,
    secondaryContainer = WarmSecondaryContainerDark,
    onSecondaryContainer = WarmOnSecondaryContainerDark,
    tertiary = WarmTertiaryDark,
    onTertiary = WarmOnTertiaryDark,
    tertiaryContainer = WarmTertiaryContainerDark,
    onTertiaryContainer = WarmOnTertiaryContainerDark,
    background = WarmBackgroundDark,
    onBackground = WarmOnBackgroundDark,
    surface = WarmSurfaceDark,
    onSurface = WarmOnSurfaceDark,
    surfaceVariant = WarmSurfaceVariantDark,
    onSurfaceVariant = WarmOnSurfaceVariantDark,
    outline = WarmOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = WarmPrimaryLight,
    onPrimary = WarmOnPrimaryLight,
    primaryContainer = WarmPrimaryContainerLight,
    onPrimaryContainer = WarmOnPrimaryContainerLight,
    secondary = WarmSecondaryLight,
    onSecondary = WarmOnSecondaryLight,
    secondaryContainer = WarmSecondaryContainerLight,
    onSecondaryContainer = WarmOnSecondaryContainerLight,
    tertiary = WarmTertiaryLight,
    onTertiary = WarmOnTertiaryLight,
    tertiaryContainer = WarmTertiaryContainerLight,
    onTertiaryContainer = WarmOnTertiaryContainerLight,
    background = WarmBackgroundLight,
    onBackground = WarmOnBackgroundLight,
    surface = WarmSurfaceLight,
    onSurface = WarmOnSurfaceLight,
    surfaceVariant = WarmSurfaceVariantLight,
    onSurfaceVariant = WarmOnSurfaceVariantLight,
    outline = WarmOutlineLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep warm signature palette consistent
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
