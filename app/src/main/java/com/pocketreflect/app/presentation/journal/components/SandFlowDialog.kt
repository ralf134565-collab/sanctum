// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.journal.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.pocketreflect.app.R
import com.pocketreflect.app.core.haptic.rememberHapticFeedback
import com.pocketreflect.app.presentation.components.screenAtmosphereGradient
import com.pocketreflect.mandala.sandbox.engine.MandalaEngine
import com.pocketreflect.mandala.sandbox.engine.MandalaPhase
import com.pocketreflect.mandala.sandbox.ui.CalmingQuotesOverlay
import com.pocketreflect.mandala.sandbox.ui.MandalaCanvas
import com.pocketreflect.mandala.sandbox.ui.PhaseOverlay
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun SandFlowDialog(
    difficulty: Int,
    breathingSyncEnabled: Boolean,
    onDismiss: () -> Unit,
) {
    val engine = remember { MandalaEngine() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = rememberHapticFeedback()

    var layoutApplied by remember { mutableStateOf(false) }

    LaunchedEffect(difficulty, breathingSyncEnabled) {
        engine.tuning.coreFillThreshold = difficulty
        engine.tuning.breathingSyncEnabled = breathingSyncEnabled
    }

    val introUiTick = engine.uiSecondTick.longValue

    LaunchedEffect(lifecycleOwner, engine, layoutApplied) {
        if (!layoutApplied) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var lastNanos = withFrameNanos { it }
            while (isActive) {
                if (engine.phase == MandalaPhase.Complete) {
                    delay(200)
                    lastNanos = withFrameNanos { it }
                    continue
                }
                withFrameNanos { frameNanos ->
                    val dtSec = ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.032f)
                    lastNanos = frameNanos

                    when (engine.phase) {
                        MandalaPhase.IntroFocus,
                        MandalaPhase.Playing,
                        MandalaPhase.CoreGlow,
                        MandalaPhase.WindDestroy,
                        -> engine.update(dtSec)

                        MandalaPhase.Complete -> Unit
                    }

                    if (engine.consumeCoreCapturedFlag()) {
                        haptics.sandFlowCoreCapture()
                    }
                    if (engine.consumeRingPassFlags()) {
                        haptics.sandFlowPass()
                    }
                    haptics.sandFlowRotation(engine.consumeRotationHapticIntensity())
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .screenAtmosphereGradient(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                MandalaCanvas(
                    engine = engine,
                    onLayout = { width, height ->
                        engine.onLayout(width, height)
                        layoutApplied = true
                    },
                    onTap = {
                        when (engine.phase) {
                            MandalaPhase.IntroFocus -> engine.skipIntro()
                            MandalaPhase.Complete -> engine.restartSession()
                            else -> Unit
                        }
                    },
                    onDragStart = { touchX, touchY ->
                        engine.startDrag(touchX, touchY)
                    },
                    onDragEnd = {
                        engine.endDrag()
                    },
                    onRotationDrag = { touchX, touchY, dragX, dragY ->
                        engine.applyRotationImpulse(touchX, touchY, dragX, dragY)
                    },
                )

                PhaseOverlay(
                    engine = engine,
                    introUiTick = introUiTick,
                    onIntroSkip = engine::skipIntro,
                    onRestart = engine::restartSession,
                )

                if (engine.phase == MandalaPhase.Playing || engine.phase == MandalaPhase.CoreGlow) {
                    CalmingQuotesOverlay(
                        engine = engine,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 48.dp),
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(top = 16.dp, start = 16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.cd_sand_flow_close),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}
