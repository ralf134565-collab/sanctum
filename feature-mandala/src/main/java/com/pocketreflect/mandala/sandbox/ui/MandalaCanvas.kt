// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.mandala.sandbox.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.pocketreflect.mandala.sandbox.engine.MandalaEngine
import com.pocketreflect.mandala.sandbox.engine.MandalaPhase
import com.pocketreflect.mandala.sandbox.theme.MandalaPalette

@Composable
fun MandalaCanvas(
    engine: MandalaEngine,
    onLayout: (Float, Float) -> Unit,
    onTap: () -> Unit,
    onDragStart: (touchX: Float, touchY: Float) -> Unit,
    onDragEnd: () -> Unit,
    onRotationDrag: (touchX: Float, touchY: Float, dragX: Float, dragY: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val outlineColor = Color(0xFF64748B)
    val gapColor = Color(0xFF7DD3FC)
    val coreColor = Color(0xFF7DD3FC)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        LaunchedEffect(widthPx, heightPx) {
            if (widthPx > 0f && heightPx > 0f) {
                onLayout(widthPx, heightPx)
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(engine) {
                    detectTapGestures { onTap() }
                }
                .pointerInput(engine) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            onDragStart(offset.x, offset.y)
                        },
                        onDragEnd = {
                            onDragEnd()
                        },
                        onDragCancel = {
                            onDragEnd()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onRotationDrag(
                                change.position.x,
                                change.position.y,
                                dragAmount.x,
                                dragAmount.y,
                            )
                        },
                    )
                },
        ) {
            @Suppress("UNUSED_VARIABLE")
            val frameTick = engine.frameNonce.longValue

            val center = Offset(engine.centerX, engine.centerY)

            var ringIndex = 0
            while (ringIndex < engine.rings.size) {
                val ring = engine.rings[ringIndex]
                val ringAlpha = ring.alpha
                if (ringAlpha > 0.01f) {
                    // Эффект свечения (Bloom): рисуем размытый дубликат линии под ней
                    drawCircle(
                        color = outlineColor.copy(alpha = 0.15f * ringAlpha),
                        radius = ring.radius,
                        center = center,
                        style = Stroke(width = 12f),
                    )
                    drawArc(
                        color = gapColor.copy(alpha = 0.25f * ringAlpha),
                        startAngle = ring.gapStartAngle + ring.currentAngle,
                        sweepAngle = ring.gapWidth,
                        useCenter = false,
                        topLeft = Offset(center.x - ring.radius, center.y - ring.radius),
                        size = Size(ring.radius * 2f, ring.radius * 2f),
                        style = Stroke(width = 18f),
                    )

                    // Основная линия
                    drawCircle(
                        color = outlineColor.copy(alpha = 0.55f * ringAlpha),
                        radius = ring.radius,
                        center = center,
                        style = Stroke(width = 3f),
                    )
                    drawArc(
                        color = gapColor.copy(alpha = 0.9f * ringAlpha),
                        startAngle = ring.gapStartAngle + ring.currentAngle,
                        sweepAngle = ring.gapWidth,
                        useCenter = false,
                        topLeft = Offset(center.x - ring.radius, center.y - ring.radius),
                        size = Size(ring.radius * 2f, ring.radius * 2f),
                        style = Stroke(width = 6f),
                    )
                }
                ringIndex++
            }

            val glowAlpha = when (engine.phase) {
                MandalaPhase.CoreGlow -> 0.35f + engine.coreGlowIntensity * 0.45f
                MandalaPhase.WindDestroy -> 0.25f
                else -> 0.15f
            }
            
            // 1. ОТРИСОВКА ЦЕНТРАЛЬНОГО ЯДРА (Стабильная звезда в центре)
            val coreCenter = Offset(engine.centerX, engine.centerY)
            val currentCoreRadius = engine.coreRadius * engine.corePulseScale
            
            // Внешнее размытое неоновое свечение центрального ядра (Bloom), вспыхивающее при поглощении частиц
            drawCircle(
                color = coreColor.copy(alpha = (glowAlpha * 0.35f) * engine.corePulseScale),
                radius = currentCoreRadius * 1.5f,
                center = coreCenter,
            )
            
            // Средний мягкий слой свечения
            drawCircle(
                color = coreColor.copy(alpha = glowAlpha * engine.corePulseScale),
                radius = currentCoreRadius * (1f + engine.coreGlowIntensity * 0.25f),
                center = coreCenter,
            )
            
            // Четкий контур центрального ядра
            drawCircle(
                color = outlineColor.copy(alpha = 0.8f),
                radius = currentCoreRadius,
                center = coreCenter,
                style = Stroke(width = 2f),
            )

            // 2. ОТРИСОВКА БЛУЖДАЮЩЕГО ПРОВОДНИКА (Orb / Guide)
            val orbCenter = Offset(engine.orbX, engine.orbY)
            val orbColor = Color(0xFFFCD34D) // Теплый янтарно-золотой цвет для Проводника
            
            // Мягкий неоновый ореол вокруг Проводника
            drawCircle(
                color = orbColor.copy(alpha = 0.25f),
                radius = engine.orbRadius * 1.8f,
                center = orbCenter,
            )
            
            // Тело Проводника
            drawCircle(
                color = orbColor.copy(alpha = 0.85f),
                radius = engine.orbRadius,
                center = orbCenter,
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = engine.orbRadius * 0.6f,
                center = orbCenter,
            )

            val pool = engine.particlePool.items
            var particleIndex = 0
            while (particleIndex < pool.size) {
                val particle = pool[particleIndex]
                if (particle.isAlive) {
                    val color = Color(MandalaPalette.particleColors[particle.colorIndex])
                    drawCircle(
                        color = color.copy(alpha = 0.85f * particle.alpha),
                        radius = particle.radius,
                        center = Offset(particle.x, particle.y),
                    )
                }
                particleIndex++
            }
        }
    }
}
