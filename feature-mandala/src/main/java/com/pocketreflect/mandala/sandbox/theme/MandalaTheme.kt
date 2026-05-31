// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.mandala.sandbox.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MandalaDarkColors = darkColorScheme(
    primary = Color(0xFF7DD3FC),
    onPrimary = Color(0xFF003544),
    tertiary = Color(0xFFFCD34D),
    onTertiary = Color(0xFF3D2E00),
    background = Color(0xFF0B1020),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFE2E8F0),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    outline = Color(0xFF64748B),
)

object MandalaPalette {
    val particleColors = intArrayOf(
        0xFF7DD3FC.toInt(),
        0xFF67E8F9.toInt(),
        0xFFA5F3FC.toInt(),
        0xFFFCD34D.toInt(),
        0xFFFDE68A.toInt(),
        0xFFFBBF24.toInt(),
        0xFFA7F3D0.toInt(),
        0xFF6EE7B7.toInt(),
    )
}

@Composable
fun MandalaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MandalaDarkColors,
        content = content,
    )
}
