// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatContextPolicyTest {

    @Test
    fun computeUsage_percentFromChars() {
        val messages = listOf(
            ChatMessage(role = ChatRole.USER, content = "a".repeat(6000), timestamp = 1L),
        )
        val usage = ChatContextPolicy.computeUsage(messages, journalSnippet = null)
        assertEquals(50, usage.percent)
        assertFalse(usage.isFull)
    }

    @Test
    fun computeUsage_fullAtMax() {
        val messages = listOf(
            ChatMessage(role = ChatRole.USER, content = "x".repeat(12_000), timestamp = 1L),
        )
        val usage = ChatContextPolicy.computeUsage(messages, null)
        assertTrue(usage.isFull)
        assertEquals(100, usage.percent)
    }

    @Test
    fun trimHistory_keepsTail() {
        val old = ChatMessage(role = ChatRole.USER, content = "old".repeat(500), timestamp = 1L)
        val recent = ChatMessage(role = ChatRole.USER, content = "recent", timestamp = 2L)
        val trimmed = ChatContextPolicy.trimHistoryForInference(
            messages = listOf(old, recent),
            journalSnippetLength = 0,
        )
        assertTrue(trimmed.last().content.contains("recent"))
    }
}
