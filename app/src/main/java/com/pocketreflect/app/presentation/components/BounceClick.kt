// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Модификатор упругого утаптывания (Spring Click Bounce) для кастомных интерактивных элементов.
 * 
 * Мягко уменьшает элемент при зажатии и упруго выталкивает обратно при отпускании,
 * симулируя соматическое нажатие на мягкую подушечку.
 */
fun Modifier.bounceClick(
    damping: Float = Spring.DampingRatioMediumBouncy,
    stiffness: Float = Spring.StiffnessLow,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.95f else 1.0f,
        animationSpec = spring(
            dampingRatio = damping,
            stiffness = stiffness
        ),
        label = "bounce-click-scale"
    )

    this.then(
        graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null, // Полностью убираем дефолтную резкую вспышку (ripple) ради премиальной ламповой тактильности
            enabled = enabled,
            onClick = onClick
        )
    )
}

/**
 * Модификатор масштабирования для интеграции со стандартными компонентами Compose (Button, Chip),
 * у которых уже есть собственный MutableInteractionSource.
 */
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    damping: Float = Spring.DampingRatioMediumBouncy,
    stiffness: Float = Spring.StiffnessLow,
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(
            dampingRatio = damping,
            stiffness = stiffness
        ),
        label = "press-scale"
    )
    
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
