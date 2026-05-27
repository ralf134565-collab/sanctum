// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.core.haptic

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.getSystemService
import com.pocketreflect.app.domain.breathing.BreathingHapticIntensity
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Тактильное ведение резонансного дыхания (5+5 с).
 *
 * Принципы (Android Haptics UX + wellness-приложения вроде Haptic Calm / Breezy):
 * - **Дискретные импульсы**, не непрерывный buzz — иначе энергия накапливается и раздражает.
 * - **Ease-in / ease-out** по синусоиде — плавный вход и выход с нулевой амплитуды.
 * - **Низкий потолок амплитуды** — «подсказка», а не сигнализация.
 * - Классический квадрат — короткий [pulsePhaseChange], без изменений.
 */
class ResonantHapticGuide(context: Context) {

    private val appContext = context.applicationContext
    private val vibrator: Vibrator? = resolveVibrator(appContext)
    private val guideScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var activeJob: Job? = null

    fun playPhase(
        isInhale: Boolean,
        durationMs: Long,
        intensity: BreathingHapticIntensity = BreathingHapticIntensity.DEFAULT,
    ) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        cancelInternal()

        val profile = profileFor(intensity)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            supportsLowTick(v)
        ) {
            vibrateEffect(v, buildCompositionPhase(isInhale, durationMs, profile))
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrateEffect(
                v,
                buildPulseWaveform(isInhale, durationMs, profile, v.hasAmplitudeControl()),
            )
            return
        }

        @Suppress("DEPRECATION")
        activeJob = guideScope.launch {
            playLegacyPulses(v, isInhale, durationMs, profile)
        }
    }

    /** Короткий импульс при смене фазы (классический квадрат). */
    fun pulsePhaseChange() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrateEffect(v, VibrationEffect.createOneShot(PHASE_PULSE_MS, BOX_PULSE_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(PHASE_PULSE_MS)
        }
    }

    fun cancel() {
        cancelInternal()
    }

    fun dispose() {
        cancelInternal()
        guideScope.cancel()
    }

    private fun cancelInternal() {
        activeJob?.cancel()
        activeJob = null
        vibrator?.cancel()
    }

    private fun vibrateEffect(vibrator: Vibrator, effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VIBRATION_ATTRIBUTES)
        } else {
            vibrator.vibrate(effect)
        }
    }

    @Suppress("NewApi")
    private fun supportsLowTick(vibrator: Vibrator): Boolean =
        vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_LOW_TICK)

    /**
     * Синусоидальная огибающая 0..1: мягкий вход, пик в середине фазы, мягкий выход.
     * Соответствует рекомендации Android «ease-in at the beginning» для длинных вибраций.
     */
    private fun breathEnvelope(progress: Float, isInhale: Boolean): Float {
        val t = progress.coerceIn(0f, 1f)
        return if (isInhale) {
            sin(t * PI.toFloat() / 2f)
        } else {
            sin((1f - t) * PI.toFloat() / 2f)
        }
    }

    @Suppress("NewApi")
    private fun buildCompositionPhase(
        isInhale: Boolean,
        durationMs: Long,
        profile: PulseProfile,
    ): VibrationEffect {
        val pulses = profile.pulsesPerPhase
        val intervalMs = (durationMs / pulses).toInt().coerceAtLeast(120)
        val composition = VibrationEffect.startComposition()
        for (i in 0 until pulses) {
            val progress = i.toFloat() / (pulses - 1).coerceAtLeast(1)
            val env = breathEnvelope(progress, isInhale)
            val scale = profile.minScale + (profile.peakScale - profile.minScale) * env
            composition.addPrimitive(
                VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
                scale.coerceIn(0.08f, 0.7f),
                if (i == 0) 0 else intervalMs,
            )
        }
        return composition.compose()
    }

    private fun buildPulseWaveform(
        isInhale: Boolean,
        durationMs: Long,
        profile: PulseProfile,
        hasAmplitudeControl: Boolean,
    ): VibrationEffect {
        val pulses = profile.pulsesPerPhase
        val intervalMs = (durationMs / pulses).coerceAtLeast(120L)
        val offMs = (intervalMs - profile.pulseOnMs).coerceAtLeast(60L)
        val timings = LongArray(pulses * 2)
        val amplitudes = if (hasAmplitudeControl) IntArray(pulses * 2) else null

        for (i in 0 until pulses) {
            timings[i * 2] = if (i == 0) 0L else offMs
            timings[i * 2 + 1] = profile.pulseOnMs
            amplitudes?.let { amps ->
                amps[i * 2] = 0
                val progress = i.toFloat() / (pulses - 1).coerceAtLeast(1)
                val env = breathEnvelope(progress, isInhale)
                amps[i * 2 + 1] = (
                    profile.minAmplitude +
                        (profile.peakAmplitude - profile.minAmplitude) * env
                    ).toInt().coerceIn(1, profile.peakAmplitude)
            }
        }

        return if (amplitudes != null) {
            VibrationEffect.createWaveform(timings, amplitudes, -1)
        } else {
            VibrationEffect.createWaveform(timings, -1)
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun playLegacyPulses(
        vibrator: Vibrator,
        isInhale: Boolean,
        durationMs: Long,
        profile: PulseProfile,
    ) {
        val pulses = profile.pulsesPerPhase
        val intervalMs = (durationMs / pulses).coerceAtLeast(120L)
        for (i in 0 until pulses) {
            val progress = i.toFloat() / (pulses - 1).coerceAtLeast(1)
            val env = breathEnvelope(progress, isInhale)
            val onMs = (profile.pulseOnMs * (0.6f + 0.4f * env)).toLong().coerceAtLeast(25L)
            vibrator.vibrate(onMs)
            delay(intervalMs)
        }
    }

    private data class PulseProfile(
        val peakAmplitude: Int,
        val minAmplitude: Int,
        val peakScale: Float,
        val minScale: Float,
        val pulsesPerPhase: Int,
        val pulseOnMs: Long,
    )

    private companion object {
        const val PHASE_PULSE_MS = 120L
        const val BOX_PULSE_AMPLITUDE = 180

        val VIBRATION_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        fun profileFor(intensity: BreathingHapticIntensity): PulseProfile = when (intensity) {
            BreathingHapticIntensity.GENTLE -> PulseProfile(
                peakAmplitude = 82,
                minAmplitude = 18,
                peakScale = 0.38f,
                minScale = 0.10f,
                pulsesPerPhase = 7,
                pulseOnMs = 42L,
            )
            BreathingHapticIntensity.MODERATE -> PulseProfile(
                peakAmplitude = 128,
                minAmplitude = 32,
                peakScale = 0.55f,
                minScale = 0.16f,
                pulsesPerPhase = 9,
                pulseOnMs = 52L,
            )
        }

        fun resolveVibrator(context: Context): Vibrator? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService<VibratorManager>()?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
    }
}
