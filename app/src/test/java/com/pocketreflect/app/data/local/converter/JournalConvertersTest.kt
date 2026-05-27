// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.local.converter

import com.pocketreflect.app.domain.model.MoodTag
import org.junit.Assert.assertEquals
import org.junit.Test

class JournalConvertersTest {

    private val converters = JournalConverters()

    @Test
    fun `round trip preserves order-independent set semantics`() {
        val original = listOf(MoodTag.CALM, MoodTag.GRATITUDE, MoodTag.JOY)
        val raw = converters.fromMoodTagList(original)
        val restored = converters.toMoodTagList(raw)
        assertEquals(original.toSet(), restored.toSet())
    }

    @Test
    fun `unknown tokens are silently dropped`() {
        val raw = "calm|__broken__|gratitude"
        assertEquals(listOf(MoodTag.CALM, MoodTag.GRATITUDE), converters.toMoodTagList(raw))
    }

    @Test
    fun `empty or null input returns empty list`() {
        assertEquals(emptyList<MoodTag>(), converters.toMoodTagList(null))
        assertEquals(emptyList<MoodTag>(), converters.toMoodTagList(""))
        assertEquals(emptyList<MoodTag>(), converters.toMoodTagList("   "))
    }

    @Test
    fun `duplicates are normalized`() {
        val raw = converters.fromMoodTagList(listOf(MoodTag.JOY, MoodTag.JOY, MoodTag.CALM))
        assertEquals("joy|calm", raw)
    }
}
