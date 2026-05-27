// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Премиальный атмосферный фон «Санктума».
 * 
 * В тёмной теме («вечерний сейф») рисует глубокий космический радиальный градиент
 * с эффектом тонкого созвездия из мерцающих звёзд, предотвращая бандинг на OLED-экранах.
 * В светлой теме рисует мягкий пастельный градиент рассвета.
 */
fun Modifier.screenAtmosphereGradient(): Modifier = composed {
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0B1020)

    if (isDark) {
        val transition = rememberInfiniteTransition(label = "star-twinkle")
        val twinkleAlpha by transition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3500, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "twinkle"
        )

        drawWithCache {
            val radialBrush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF141C38), // Мягкий космический индиго в центре
                    Color(0xFF070A14)  // Абсолютная глубина ночи по краям
                ),
                center = Offset(size.width / 2f, size.height * 0.35f),
                radius = size.minDimension * 0.85f
            )

            onDrawBehind {
                drawRect(brush = radialBrush)

                // Созвездие (4 мерцающие звезды для создания вечерней глубины)
                val starCoordinates = listOf(
                    Offset(size.width * 0.18f, size.height * 0.12f),
                    Offset(size.width * 0.82f, size.height * 0.22f),
                    Offset(size.width * 0.52f, size.height * 0.42f),
                    Offset(size.width * 0.12f, size.height * 0.68f),
                )

                starCoordinates.forEachIndexed { index, center ->
                    val alpha = if (index % 2 == 0) twinkleAlpha else (1f - twinkleAlpha)
                    drawCircle(
                        color = Color(0xFFDDD3FF),
                        radius = if (index == 2) 1.5.dp.toPx() else 1.dp.toPx(),
                        center = center,
                        alpha = alpha * 0.5f
                    )
                }
            }
        }
    } else {
        background(
            Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.background,
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                )
            )
        )
    }
}
