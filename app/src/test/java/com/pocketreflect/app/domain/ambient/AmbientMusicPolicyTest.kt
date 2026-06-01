// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.ambient

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientMusicPolicyTest {

    @Test
    fun shouldAutoPlay_whenEnabledNotPausedAndHasTracks() {
        assertTrue(AmbientMusicPolicy.shouldAutoPlay(true, false, true))
        assertFalse(AmbientMusicPolicy.shouldAutoPlay(false, false, true))
        assertFalse(AmbientMusicPolicy.shouldAutoPlay(true, true, true))
        assertFalse(AmbientMusicPolicy.shouldAutoPlay(true, false, false))
    }
}
