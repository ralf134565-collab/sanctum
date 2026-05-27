// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pocketreflect.app.ui.theme.PocketReflectShapes

/**
 * Возвращает кисть градиентной обводки (Beveled Stroke) для создания иллюзии объема и падения света.
 * Верхняя левая часть обводки подсвечивается, а правая нижняя — уходит в тень.
 */
@Composable
fun rememberCalmCardBorderBrush(): Brush {
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0B1020)
    
    val startColor = if (isDark) {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.45f) // Нежное лавандовое свечение сверху-слева
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)  // Мягкая прорисовка для светлой темы
    }
    
    val endColor = if (isDark) {
        Color(0xFF070A14).copy(alpha = 0.15f) // Растворяется в тени на темном фоне
    } else {
        Color.Transparent
    }

    return remember(startColor, endColor) {
        Brush.linearGradient(
            colors = listOf(startColor, endColor),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }
}

/**
 * Премиальный модификатор объемной рамки карточки.
 */
fun Modifier.calmCardBorder(): Modifier = composed {
    border(
        width = 1.dp,
        brush = rememberCalmCardBorderBrush(),
        shape = PocketReflectShapes.Card,
    )
}

/**
 * Премиальный BorderStroke для использования внутри стандартных Card(border = ...).
 */
@Composable
fun calmCardBorderStroke(): BorderStroke {
    return BorderStroke(
        width = 1.dp,
        brush = rememberCalmCardBorderBrush()
    )
}
