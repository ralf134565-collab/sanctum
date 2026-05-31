// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.mandala.sandbox.engine

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MandalaPhaseTest {

    private lateinit var engine: MandalaEngine

    @Before
    fun setUp() {
        engine = MandalaEngine(maxParticles = 16)
        engine.onLayout(width = 800f, height = 1200f)
    }

    @Test
    fun intro_transitionsToPlayingAfterTenSeconds() {
        assertEquals(MandalaPhase.IntroFocus, engine.phase)
        engine.update(dtSec = 10.1f)
        assertEquals(MandalaPhase.Playing, engine.phase)
    }

    @Test
    fun skipIntro_movesDirectlyToPlaying() {
        engine.skipIntro()
        assertEquals(MandalaPhase.Playing, engine.phase)
    }

    @Test
    fun playing_transitionsToCoreGlowWhenThresholdReached() {
        engine.skipIntro()
        engine.tuning.coreFillThreshold = 2
        engine.metrics.coreFill = 2
        engine.update(dtSec = 0.016f)
        assertEquals(MandalaPhase.CoreGlow, engine.phase)
    }

    @Test
    fun coreGlow_transitionsToWindAfterTwoSeconds() {
        engine.skipIntro()
        engine.tuning.coreFillThreshold = 1
        engine.metrics.coreFill = 1
        engine.update(dtSec = 0.016f)
        assertEquals(MandalaPhase.CoreGlow, engine.phase)

        engine.update(dtSec = 2.1f)
        assertEquals(MandalaPhase.WindDestroy, engine.phase)
    }

    @Test
    fun wind_transitionsToCompleteWhenNoParticlesRemain() {
        engine.skipIntro()
        engine.tuning.coreFillThreshold = 1
        engine.metrics.coreFill = 1
        engine.update(dtSec = 0.016f)
        engine.update(dtSec = 2.1f)
        assertEquals(MandalaPhase.WindDestroy, engine.phase)

        engine.update(dtSec = 0.5f)
        assertEquals(MandalaPhase.Complete, engine.phase)
    }

    @Test
    fun restartSession_returnsToIntroFocus() {
        engine.skipIntro()
        engine.restartSession()
        assertEquals(MandalaPhase.IntroFocus, engine.phase)
        assertEquals(0, engine.metrics.coreFill)
    }
}
