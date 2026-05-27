// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.ai

import com.pocketreflect.app.domain.ai.AiEngineStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class AiEngineStatusResolverTest {

    @Test
    fun `no attached model is offline`() {
        WarmupState.entries.forEach { warmup ->
            assertEquals(
                AiEngineStatus.MODEL_OFFLINE,
                resolveAiEngineStatus(
                    hasAttachedModel = false,
                    warmupState = warmup,
                    realEngineReady = true,
                ),
            )
        }
    }

    @Test
    fun `real engine ready wins over failed warmup`() {
        assertEquals(
            AiEngineStatus.REAL_READY,
            resolveAiEngineStatus(
                hasAttachedModel = true,
                warmupState = WarmupState.Failed,
                realEngineReady = true,
            ),
        )
    }

    @Test
    fun `attached and ready warmup is real ready`() {
        assertEquals(
            AiEngineStatus.REAL_READY,
            resolveAiEngineStatus(
                hasAttachedModel = true,
                warmupState = WarmupState.Ready,
                realEngineReady = false,
            ),
        )
    }

    @Test
    fun `attached while warming shows warming`() {
        assertEquals(
            AiEngineStatus.WARMING,
            resolveAiEngineStatus(
                hasAttachedModel = true,
                warmupState = WarmupState.Warming,
                realEngineReady = false,
            ),
        )
    }

    @Test
    fun `attached failed warmup and engine not ready is fallback`() {
        assertEquals(
            AiEngineStatus.FALLBACK,
            resolveAiEngineStatus(
                hasAttachedModel = true,
                warmupState = WarmupState.Failed,
                realEngineReady = false,
            ),
        )
    }
}
