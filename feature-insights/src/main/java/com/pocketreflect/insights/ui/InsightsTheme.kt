// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.insights.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val InsightsDark = darkColorScheme(
    primary = Color(0xFFB8A9E8),
    onPrimary = Color(0xFF1A1625),
    primaryContainer = Color(0xFF3D3555),
    onPrimaryContainer = Color(0xFFE8E0F5),
    secondary = Color(0xFF8EC9B0),
    secondaryContainer = Color(0xFF2A4038),
    tertiary = Color(0xFFD4B896),
    tertiaryContainer = Color(0xFF4A3D2E),
    background = Color(0xFF0F1118),
    onBackground = Color(0xFFE8E6ED),
    surface = Color(0xFF161922),
    onSurface = Color(0xFFE8E6ED),
    surfaceVariant = Color(0xFF222633),
    onSurfaceVariant = Color(0xFFB8B5C4),
    outline = Color(0xFF3A3F52),
)

@Composable
fun InsightsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = InsightsDark,
        content = content,
    )
}
