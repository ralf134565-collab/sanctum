// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.pocketreflect.app.data.repository.AppThemeMode

/**
 * Главная тема приложения.
 *
 * По умолчанию тёмная («вечерний дневник»). Светлая — опционально в настройках.
 * Dynamic Color (Material You) не используем: палитра подобрана под спокойный UX.
 */
private val DarkColorScheme = darkColorScheme(
    primary = LavenderPrimary,
    onPrimary = LavenderOnPrimary,
    primaryContainer = LavenderContainer,
    onPrimaryContainer = LavenderOnContainer,

    secondary = MintSecondary,
    onSecondary = MintOnSecondary,
    secondaryContainer = MintContainer,
    onSecondaryContainer = MintOnContainer,

    tertiary = AmberWarning,
    onTertiary = AmberOnWarning,

    error = CoralCritical,
    onError = CoralOnCritical,

    background = MidnightBackground,
    onBackground = TextPrimary,

    surface = MidnightSurface,
    onSurface = TextPrimary,
    surfaceVariant = MidnightSurfaceVar,
    onSurfaceVariant = TextSecondary,

    outline = MidnightOutline,
    outlineVariant = MidnightOutline,
)

private val LightColorScheme = lightColorScheme(
    primary = DawnLavenderPrimary,
    onPrimary = DawnLavenderOnPrimary,
    primaryContainer = DawnLavenderContainer,
    onPrimaryContainer = DawnLavenderOnContainer,

    secondary = DawnMintSecondary,
    onSecondary = DawnMintOnSecondary,
    secondaryContainer = DawnMintContainer,
    onSecondaryContainer = DawnMintOnContainer,

    tertiary = DawnAmberWarning,
    onTertiary = DawnAmberOnWarning,

    error = DawnCoralCritical,
    onError = DawnCoralOnCritical,

    background = DawnBackground,
    onBackground = DawnTextPrimary,

    surface = DawnSurface,
    onSurface = DawnTextPrimary,
    surfaceVariant = DawnSurfaceVar,
    onSurfaceVariant = DawnTextSecondary,

    outline = DawnOutline,
    outlineVariant = DawnOutline,
)

@Composable
fun PocketReflectTheme(
    themeMode: AppThemeMode = AppThemeMode.DEFAULT,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (themeMode) {
        AppThemeMode.DARK -> DarkColorScheme
        AppThemeMode.LIGHT -> LightColorScheme
    }
    val useLightSystemBars = themeMode == AppThemeMode.LIGHT

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = useLightSystemBars
                isAppearanceLightNavigationBars = useLightSystemBars
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PocketReflectTypography,
        content = content,
    )
}
