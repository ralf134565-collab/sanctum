// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.journal.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketreflect.app.R
import com.pocketreflect.app.core.haptic.ResonantHapticGuide
import com.pocketreflect.app.domain.breathing.BreathingHapticIntensity
import com.pocketreflect.app.domain.breathing.BreathingPattern
import com.pocketreflect.app.domain.breathing.BreathingSessionController
import kotlinx.coroutines.delay

/**
 * Полноэкранная дыхательная сессия «Дыхательный мост».
 *
 * Режимы задаются в настройках: резонансное 5+5 (6 bpm) или классический квадрат 4-4-4-4.
 */
@Composable
fun BreathingDialog(
    pattern: BreathingPattern,
    hapticEnabled: Boolean,
    hapticIntensity: BreathingHapticIntensity,
    cycleCount: Int,
    onDismiss: () -> Unit,
) {
    val startedAtMs = rememberSaveable { System.currentTimeMillis() }
    val normalizedCycles = BreathingSessionController.normalizeCycleCount(cycleCount)
    val context = LocalContext.current
    val hapticGuide = remember { ResonantHapticGuide(context) }

    var snapshot by remember(startedAtMs, pattern, normalizedCycles) {
        mutableStateOf(
            BreathingSessionController.snapshot(
                pattern = pattern,
                cycleCount = normalizedCycles,
                startedAtMs = startedAtMs,
                nowMs = startedAtMs,
            ),
        )
    }
    var lastHapticToken by remember(startedAtMs, pattern) { mutableLongStateOf(-1L) }

    DisposableEffect(Unit) {
        onDispose { hapticGuide.dispose() }
    }

    LaunchedEffect(startedAtMs, pattern, normalizedCycles) {
        while (true) {
            val now = System.currentTimeMillis()
            val next = BreathingSessionController.snapshot(
                pattern = pattern,
                cycleCount = normalizedCycles,
                startedAtMs = startedAtMs,
                nowMs = now,
            )
            snapshot = next
            if (next.completed) break
            delay(100L)
        }
    }

    val cycleDurationMs = BreathingSessionController.cycleDurationMs(pattern)
    val hapticToken = if (snapshot.completed) {
        -1L
    } else {
        val elapsedMs = (System.currentTimeMillis() - startedAtMs).coerceAtLeast(0L)
        val cycleIndex = elapsedMs / cycleDurationMs
        cycleIndex * 10L + snapshot.phaseIndexInCycle
    }

    LaunchedEffect(hapticToken, hapticEnabled, hapticIntensity, pattern, snapshot.completed) {
        if (snapshot.completed || !hapticEnabled || hapticToken < 0L) {
            if (snapshot.completed) hapticGuide.cancel()
            return@LaunchedEffect
        }
        if (hapticToken == lastHapticToken) return@LaunchedEffect
        lastHapticToken = hapticToken

        when (pattern) {
            BreathingPattern.RESONANT -> {
                val isInhale = snapshot.phase == BreathingSessionController.Phase.INHALE
                hapticGuide.playPhase(
                    isInhale = isInhale,
                    durationMs = BreathingSessionController.phaseDurationMs(
                        pattern,
                        snapshot.phase,
                    ),
                    intensity = hapticIntensity,
                )
            }
            BreathingPattern.BOX -> hapticGuide.pulsePhaseChange()
        }
    }

    val isResonant = pattern == BreathingPattern.RESONANT
    val completed = snapshot.completed

    val scaleTarget = when (pattern) {
        BreathingPattern.RESONANT -> when (snapshot.phase) {
            BreathingSessionController.Phase.INHALE -> 2.2f
            BreathingSessionController.Phase.EXHALE -> 1.0f
            else -> 1.0f
        }
        BreathingPattern.BOX -> when (snapshot.phase) {
            BreathingSessionController.Phase.INHALE -> 2.2f
            BreathingSessionController.Phase.HOLD_AFTER_INHALE -> 2.2f
            BreathingSessionController.Phase.EXHALE -> 1.0f
            BreathingSessionController.Phase.HOLD_AFTER_EXHALE -> 1.0f
        }
    }

    val animationDuration = when (pattern) {
        BreathingPattern.RESONANT -> 5_000
        BreathingPattern.BOX -> when (snapshot.phase) {
            BreathingSessionController.Phase.INHALE,
            BreathingSessionController.Phase.EXHALE,
            -> 4_000
            else -> 0
        }
    }

    val easing = if (isResonant) FastOutSlowInEasing else LinearEasing

    val sphereScale by animateFloatAsState(
        targetValue = if (completed) 1.6f else scaleTarget,
        animationSpec = if (animationDuration > 0 && !completed) {
            tween(durationMillis = animationDuration, easing = easing)
        } else {
            snap()
        },
        label = "BreathingSphereScale",
    )

    val dismiss = {
        hapticGuide.cancel()
        onDismiss()
    }

    Dialog(
        onDismissRequest = dismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isResonant && !completed) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.72f)),
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                ) {
                    IconButton(
                        onClick = dismiss,
                        modifier = Modifier.align(Alignment.TopStart),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.action_cancel),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (isResonant && !completed) 0.85f else 0.6f,
                            ),
                        )
                    }

                    if (!completed) {
                        Text(
                            text = formatTimer(snapshot.secondsRemaining),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 18.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (isResonant) 0.55f else 0.4f,
                            ),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 10.dp, end = 10.dp),
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (!completed) {
                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(168.dp)
                                        .scale(sphereScale * 1.05f)
                                        .background(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                                    Color.Transparent,
                                                ),
                                            ),
                                            shape = CircleShape,
                                        ),
                                )
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .scale(sphereScale)
                                        .background(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary.copy(
                                                        alpha = if (isResonant) 0.85f else 0.7f,
                                                    ),
                                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                                                    Color.Transparent,
                                                ),
                                            ),
                                            shape = CircleShape,
                                        ),
                                )
                            }

                            Spacer(modifier = Modifier.height(110.dp))

                            Text(
                                text = phaseLabel(snapshot.phase),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 28.sp,
                                ),
                                color = if (isResonant) {
                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.92f)
                                } else {
                                    MaterialTheme.colorScheme.onBackground
                                },
                                textAlign = TextAlign.Center,
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (isResonant) {
                                    stringResource(R.string.breathing_resonant_hint)
                                } else {
                                    stringResource(R.string.breathing_banner_hint)
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 12.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = if (isResonant) 0.75f else 0.65f,
                                ),
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(horizontal = 32.dp),
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                                Color.Transparent,
                                            ),
                                        ),
                                        shape = CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(MaterialTheme.colorScheme.secondary, CircleShape),
                                )
                            }

                            Spacer(modifier = Modifier.height(48.dp))

                            Text(
                                text = stringResource(R.string.breathing_completion_ready),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 32.sp,
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = stringResource(R.string.breathing_completion_subtitle),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp),
                                lineHeight = 24.sp,
                            )

                            Spacer(modifier = Modifier.height(40.dp))

                            Button(
                                onClick = dismiss,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.action_understood),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun phaseLabel(phase: BreathingSessionController.Phase): String = when (phase) {
    BreathingSessionController.Phase.INHALE -> stringResource(R.string.breathing_phase_inhale)
    BreathingSessionController.Phase.HOLD_AFTER_INHALE,
    BreathingSessionController.Phase.HOLD_AFTER_EXHALE,
    -> stringResource(R.string.breathing_phase_hold)
    BreathingSessionController.Phase.EXHALE -> stringResource(R.string.breathing_phase_exhale)
}

private fun formatTimer(secondsRemaining: Int): String {
    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
