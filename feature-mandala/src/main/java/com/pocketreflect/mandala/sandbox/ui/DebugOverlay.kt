// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.mandala.sandbox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketreflect.feature.mandala.BuildConfig
import com.pocketreflect.feature.mandala.R
import com.pocketreflect.mandala.sandbox.engine.MandalaEngine
import com.pocketreflect.mandala.sandbox.engine.MandalaPhase

@Composable
fun DebugOverlay(
    fps: Float,
    engine: MandalaEngine,
    modifier: Modifier = Modifier,
) {
    if (!BuildConfig.DEBUG) return

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = "FPS: ${"%.1f".format(fps)}", style = MaterialTheme.typography.labelMedium)
        Text(text = "Alive: ${engine.particlePool.aliveCount()}", style = MaterialTheme.typography.labelMedium)
        Text(
            text = "Core: ${engine.metrics.coreFill} / ${engine.tuning.coreFillThreshold}",
            style = MaterialTheme.typography.labelMedium,
        )
        Text(text = "Passes: ${engine.metrics.channelPasses}", style = MaterialTheme.typography.labelMedium)
        Text(
            text = "Breathing: ${if (engine.tuning.breathingSyncEnabled) "on" else "off"}",
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
fun DebugTuningPanel(
    visible: Boolean,
    engine: MandalaEngine,
    onCycleGravity: () -> Unit,
    onCycleSpawn: () -> Unit,
    onCycleFriction: () -> Unit,
    onToggleBreathing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!BuildConfig.DEBUG || !visible) return

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
            .padding(12.dp)
            .pointerInput(Unit) {
                detectTapGestures {
                    onCycleGravity()
                    onCycleSpawn()
                    onCycleFriction()
                    onToggleBreathing()
                }
            },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = "Debug tuning (tap to cycle)", style = MaterialTheme.typography.titleSmall)
        Text(text = "Gravity: ${engine.tuning.gravity.toInt()}", style = MaterialTheme.typography.labelMedium)
        Text(text = "Spawn ms: ${engine.tuning.spawnIntervalMs}", style = MaterialTheme.typography.labelMedium)
        Text(text = "Friction: ${"%.2f".format(engine.tuning.friction)}", style = MaterialTheme.typography.labelMedium)
        Text(
            text = "Breathing sync: ${if (engine.tuning.breathingSyncEnabled) "ON" else "OFF"}",
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
fun PhaseOverlay(
    engine: MandalaEngine,
    introUiTick: Long,
    onIntroSkip: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    @Suppress("UNUSED_VARIABLE")
    val introTick = introUiTick

    Box(modifier = modifier.fillMaxSize()) {
        val bottomOverlayModifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 32.dp)
            .padding(bottom = 48.dp)

        when (engine.phase) {
            MandalaPhase.IntroFocus -> {
                Column(
                    modifier = bottomOverlayModifier
                        .pointerInput(Unit) {
                            detectTapGestures { onIntroSkip() }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.intro_focus),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            lineHeight = 22.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    CircularProgressIndicator(
                        progress = { engine.introElapsedSec / 10f },
                        modifier = Modifier.size(40.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        strokeWidth = 2.dp,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    )

                    Text(
                        text = if (engine.introElapsedSec >= 3f) {
                            stringResource(R.string.intro_tap_to_begin)
                        } else {
                            "${(10f - engine.introElapsedSec).toInt().coerceAtLeast(0)}s"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(
                            letterSpacing = 0.5.sp,
                        ),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            MandalaPhase.Complete -> {
                Column(
                    modifier = bottomOverlayModifier
                        .pointerInput(Unit) {
                            detectTapGestures { onRestart() }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.session_complete),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            lineHeight = 22.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(R.string.session_restart),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            else -> Unit
        }
    }
}
