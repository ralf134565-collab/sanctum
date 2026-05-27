// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.breathing

/**
 * Чистая логика фаз дыхательной сессии — без Android/Compose.
 */
object BreathingSessionController {

    enum class Phase {
        INHALE,
        HOLD_AFTER_INHALE,
        EXHALE,
        HOLD_AFTER_EXHALE,
    }

    data class Snapshot(
        val phase: Phase,
        /** Индекс фазы внутри одного цикла (0..1 resonant, 0..3 box). */
        val phaseIndexInCycle: Int,
        /** Прогресс текущей фазы 0..1. */
        val phaseProgress: Float,
        val secondsRemaining: Int,
        val completed: Boolean,
        val totalDurationMs: Long,
    )

    fun snapshot(
        pattern: BreathingPattern,
        cycleCount: Int,
        startedAtMs: Long,
        nowMs: Long,
    ): Snapshot {
        val normalizedCycles = normalizeCycleCount(cycleCount)
        val cycleDurationMs = cycleDurationMs(pattern)
        val totalDurationMs = cycleDurationMs * normalizedCycles
        val elapsedMs = (nowMs - startedAtMs).coerceAtLeast(0L)
        val remainingMs = totalDurationMs - elapsedMs

        if (remainingMs <= 0L) {
            return Snapshot(
                phase = Phase.HOLD_AFTER_EXHALE,
                phaseIndexInCycle = lastPhaseIndex(pattern),
                phaseProgress = 1f,
                secondsRemaining = 0,
                completed = true,
                totalDurationMs = totalDurationMs,
            )
        }

        val elapsedInCycleMs = elapsedMs % cycleDurationMs
        val (phase, phaseIndex, phaseDurationMs) = resolvePhase(pattern, elapsedInCycleMs)
        val phaseProgress = (elapsedInCycleMs - phaseOffsetMs(pattern, phaseIndex))
            .coerceAtLeast(0L)
            .toFloat() / phaseDurationMs.toFloat()

        return Snapshot(
            phase = phase,
            phaseIndexInCycle = phaseIndex,
            phaseProgress = phaseProgress.coerceIn(0f, 1f),
            secondsRemaining = ((remainingMs + 999L) / 1000L).toInt(),
            completed = false,
            totalDurationMs = totalDurationMs,
        )
    }

    fun normalizeCycleCount(raw: Int): Int = when {
        raw < 3 -> 3
        raw > 8 -> 8
        else -> raw
    }

    fun defaultCycleCount(pattern: BreathingPattern): Int = when (pattern) {
        BreathingPattern.RESONANT -> 6
        BreathingPattern.BOX -> 4
    }

    fun cycleDurationMs(pattern: BreathingPattern): Long = when (pattern) {
        BreathingPattern.RESONANT -> 10_000L
        BreathingPattern.BOX -> 16_000L
    }

    fun phaseDurationMs(pattern: BreathingPattern, phase: Phase): Long = when (pattern) {
        BreathingPattern.RESONANT -> when (phase) {
            Phase.INHALE, Phase.EXHALE -> 5_000L
            else -> 0L
        }
        BreathingPattern.BOX -> 4_000L
    }

    private fun lastPhaseIndex(pattern: BreathingPattern): Int = when (pattern) {
        BreathingPattern.RESONANT -> 1
        BreathingPattern.BOX -> 3
    }

    private fun resolvePhase(
        pattern: BreathingPattern,
        elapsedInCycleMs: Long,
    ): Triple<Phase, Int, Long> = when (pattern) {
        BreathingPattern.RESONANT -> when {
            elapsedInCycleMs < 5_000L -> Triple(Phase.INHALE, 0, 5_000L)
            else -> Triple(Phase.EXHALE, 1, 5_000L)
        }
        BreathingPattern.BOX -> when {
            elapsedInCycleMs < 4_000L -> Triple(Phase.INHALE, 0, 4_000L)
            elapsedInCycleMs < 8_000L -> Triple(Phase.HOLD_AFTER_INHALE, 1, 4_000L)
            elapsedInCycleMs < 12_000L -> Triple(Phase.EXHALE, 2, 4_000L)
            else -> Triple(Phase.HOLD_AFTER_EXHALE, 3, 4_000L)
        }
    }

    private fun phaseOffsetMs(pattern: BreathingPattern, phaseIndex: Int): Long = when (pattern) {
        BreathingPattern.RESONANT -> if (phaseIndex == 0) 0L else 5_000L
        BreathingPattern.BOX -> phaseIndex * 4_000L
    }
}
