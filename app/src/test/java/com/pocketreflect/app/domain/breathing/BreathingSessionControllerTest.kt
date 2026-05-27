// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.breathing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BreathingSessionControllerTest {

    @Test
    fun resonant_atStart_isInhale() {
        val snap = BreathingSessionController.snapshot(
            pattern = BreathingPattern.RESONANT,
            cycleCount = 6,
            startedAtMs = 0L,
            nowMs = 0L,
        )
        assertEquals(BreathingSessionController.Phase.INHALE, snap.phase)
        assertEquals(0, snap.phaseIndexInCycle)
        assertFalse(snap.completed)
        assertEquals(60, snap.secondsRemaining)
    }

    @Test
    fun resonant_afterFiveSeconds_isExhale() {
        val snap = BreathingSessionController.snapshot(
            pattern = BreathingPattern.RESONANT,
            cycleCount = 6,
            startedAtMs = 0L,
            nowMs = 5_000L,
        )
        assertEquals(BreathingSessionController.Phase.EXHALE, snap.phase)
        assertEquals(1, snap.phaseIndexInCycle)
    }

    @Test
    fun resonant_afterFullSession_isCompleted() {
        val snap = BreathingSessionController.snapshot(
            pattern = BreathingPattern.RESONANT,
            cycleCount = 6,
            startedAtMs = 0L,
            nowMs = 60_000L,
        )
        assertTrue(snap.completed)
        assertEquals(0, snap.secondsRemaining)
    }

    @Test
    fun box_followsFourPhaseCycle() {
        assertEquals(
            BreathingSessionController.Phase.INHALE,
            phaseAt(0L),
        )
        assertEquals(
            BreathingSessionController.Phase.HOLD_AFTER_INHALE,
            phaseAt(4_000L),
        )
        assertEquals(
            BreathingSessionController.Phase.EXHALE,
            phaseAt(8_000L),
        )
        assertEquals(
            BreathingSessionController.Phase.HOLD_AFTER_EXHALE,
            phaseAt(12_000L),
        )
    }

    @Test
    fun box_defaultSessionDuration_is64Seconds() {
        val snap = BreathingSessionController.snapshot(
            pattern = BreathingPattern.BOX,
            cycleCount = BreathingSessionController.defaultCycleCount(BreathingPattern.BOX),
            startedAtMs = 0L,
            nowMs = 0L,
        )
        assertEquals(64, snap.secondsRemaining)
    }

    private fun phaseAt(nowMs: Long): BreathingSessionController.Phase =
        BreathingSessionController.snapshot(
            pattern = BreathingPattern.BOX,
            cycleCount = 4,
            startedAtMs = 0L,
            nowMs = nowMs,
        ).phase
}
