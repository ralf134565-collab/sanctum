// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Базовые юнит-тесты доменной модели — гарантируют, что
 * правила «adaptive UX» (полярность тегов) не сломаются при добавлении новых.
 */
class MoodTagTest {

    @Test
    fun `fromStorageKeyOrNull is case-insensitive`() {
        assertEquals(MoodTag.ANXIETY, MoodTag.fromStorageKeyOrNull("ANXIETY"))
        assertEquals(MoodTag.CALM, MoodTag.fromStorageKeyOrNull("calm"))
    }

    @Test
    fun `unknown key returns null`() {
        assertNull(MoodTag.fromStorageKeyOrNull("__broken__"))
    }

    @Test
    fun `set with anxiety detects negative polarity`() {
        val tags = setOf(MoodTag.ANXIETY, MoodTag.CALM)
        assertTrue(tags.hasNegative)
        assertTrue(tags.hasPositive)
    }

    @Test
    fun `set with only joy has no negative`() {
        val tags = setOf(MoodTag.JOY, MoodTag.GRATITUDE)
        assertFalse(tags.hasNegative)
        assertTrue(tags.hasPositive)
    }
}
