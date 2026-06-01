// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.ambient

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmbientAudioFormatTest {

    @Test
    fun extensionFromMime_oggAndMp3() {
        assertEquals(AmbientAudioFormat.EXT_OGG, AmbientAudioFormat.extensionFromMime("audio/ogg"))
        assertEquals(AmbientAudioFormat.EXT_OGG, AmbientAudioFormat.extensionFromMime("application/ogg"))
        assertEquals(AmbientAudioFormat.EXT_MP3, AmbientAudioFormat.extensionFromMime("audio/mpeg"))
        assertEquals(AmbientAudioFormat.EXT_MP3, AmbientAudioFormat.extensionFromMime("audio/mp3"))
        assertNull(AmbientAudioFormat.extensionFromMime("audio/wav"))
    }
}
