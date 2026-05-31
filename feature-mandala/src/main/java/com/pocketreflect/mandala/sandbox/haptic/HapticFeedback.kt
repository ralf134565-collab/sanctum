// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.mandala.sandbox.haptic

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

class HapticFeedback(
    private val context: Context,
    private val isEnabled: Boolean = true
) {
    private val vibrator: Vibrator? = resolveVibrator(context)

    fun sandFlowRotation(intensity: Float) {
        if (!isEnabled || intensity <= 0.05f) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            v.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_LOW_TICK)
        ) {
            val composition = VibrationEffect.startComposition()
            val scale = (0.08f + intensity * 0.42f).coerceIn(0.08f, 0.5f)
            composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, scale)
            vibrateEffect(v, composition.compose())
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val amplitude = (40f + intensity * 110f).toInt().coerceIn(30, 150)
            vibrateEffect(v, VibrationEffect.createOneShot(10, amplitude))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(10)
        }
    }

    fun sandFlowPass() {
        if (!isEnabled) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            v.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_LOW_TICK)
        ) {
            val composition = VibrationEffect.startComposition()
            composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.12f)
            vibrateEffect(v, composition.compose())
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrateEffect(v, VibrationEffect.createOneShot(8, 35))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(8)
        }
    }

    fun sandFlowCoreCapture() {
        if (!isEnabled) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrateEffect(
                v,
                VibrationEffect.createWaveform(
                    longArrayOf(0, 15, 30, 15),
                    intArrayOf(0, 45, 0, 25),
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(30)
        }
    }

    private fun vibrateEffect(vibrator: Vibrator, effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VIBRATION_ATTRIBUTES)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    vibrator.vibrate(effect, VIBRATION_ATTRIBUTES)
                } catch (e: Exception) {
                    vibrator.vibrate(effect)
                }
            } else {
                vibrator.vibrate(effect)
            }
        }
    }

    private companion object {
        val VIBRATION_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        fun resolveVibrator(context: Context): Vibrator? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
    }
}

@Composable
fun rememberHapticFeedback(isEnabled: Boolean = true): HapticFeedback {
    val context = LocalContext.current.applicationContext
    return remember(context, isEnabled) { HapticFeedback(context, isEnabled) }
}
