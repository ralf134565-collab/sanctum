// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Премиальный «дышащий» круг с физиологической кривой дыхания и объемным свечением (Glow Aura).
 * 
 * Симулирует реальный вдох и выдох человека: легкие наполняются воздухом медленно
 * с задержкой у пика (CubicBezierEasing), создавая глубокий соматический эффект заземления.
 */
@Composable
fun CalmPulseCircle(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    color: Color = MaterialTheme.colorScheme.secondary,
    durationMillis: Int = 2500, // Половина цикла дыхания (2.5с на вдох, 2.5с на выдох)
) {
    val transition = rememberInfiniteTransition(label = "premium-breath")
    
    // Физиологически выверенная синусоидальная кривая вдоха-выдоха
    val breathingEase = CubicBezierEasing(0.35f, 0.0f, 0.25f, 1.0f)
    
    val scale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = breathingEase),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breath-scale",
    )
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = breathingEase),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breath-alpha",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size * 1.4f)
    ) {
        // Мягкий внешний сияющий ореол (Ambient Glow)
        Box(
            modifier = Modifier
                .size(size)
                .scale(scale * 1.25f)
                .alpha((1.35f - scale) * 0.2f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.5f), Color.Transparent)
                    )
                )
        )

        // Плотное упругое ядро
        Box(
            modifier = Modifier
                .size(size)
                .scale(scale)
                .alpha(alpha)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            color,
                            color.copy(alpha = 0.8f)
                        )
                    )
                ),
        )
    }
}

/**
 * Премиальный волнообразный парящий индикатор набора текста для чата наставника.
 * 
 * Каждая точка парит вверх-вниз по плавной синусоиде с фазовым сдвигом,
 * создавая физическую имитацию волны.
 */
@Composable
fun CalmTypingIndicator(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            val transition = rememberInfiniteTransition(label = "bubble-wave-$index")
            
            val dy by transition.animateFloat(
                initialValue = 0f,
                targetValue = -5f, // Высота парения в dp
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 600,
                        delayMillis = index * 120, // Волновой сдвиг
                        easing = EaseInOutSine,
                    ),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "bubble-offset-$index",
            )
            
            val alpha by transition.animateFloat(
                initialValue = 0.35f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 600,
                        delayMillis = index * 120,
                        easing = EaseInOutSine,
                    ),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "bubble-alpha-$index",
            )

            Box(
                modifier = Modifier
                    .size(6.dp)
                    .graphicsLayer { translationY = dy.dp.toPx() }
                    .alpha(alpha)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
            )
        }
    }
}
