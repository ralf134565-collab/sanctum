// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.ai

import androidx.work.WorkInfo
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Юнит-тесты на [reduceWarmupState] — pure JVM, без WorkManager и без
 * Robolectric. Reducer вынесен из [WarmupCoordinator] именно ради таких
 * быстрых детерминированных тестов всех ветвей конечного автомата.
 */
class WarmupStateReducerTest {

    // --- Pre-attachment phase ----------------------------------------------

    @Test
    fun `null attached emits Unknown regardless of work state`() {
        WorkInfo.State.entries.forEach { workState ->
            assertEquals(
                "expected Unknown for null attached + $workState",
                WarmupState.Unknown,
                reduceWarmupState(
                    hasAttachedModel = null,
                    workInfoState = workState,
                    launchWarmupEnabled = true,
                ),
            )
        }
        assertEquals(
            WarmupState.Unknown,
            reduceWarmupState(
                hasAttachedModel = null,
                workInfoState = null,
                launchWarmupEnabled = false,
            ),
        )
    }

    // --- No model attached --------------------------------------------------

    @Test
    fun `not attached emits NoModel and ignores any work state`() {
        WorkInfo.State.entries.forEach { workState ->
            assertEquals(
                "expected NoModel for hasAttached=false + $workState",
                WarmupState.NoModel,
                reduceWarmupState(
                    hasAttachedModel = false,
                    workInfoState = workState,
                    launchWarmupEnabled = true,
                ),
            )
        }
    }

    // --- On-demand mode (launch warmup disabled) ----------------------------

    @Test
    fun `attached with launch warmup off and no work emits Idle`() {
        assertEquals(
            WarmupState.Idle,
            reduceWarmupState(
                hasAttachedModel = true,
                workInfoState = null,
                launchWarmupEnabled = false,
            ),
        )
    }

    @Test
    fun `attached with launch warmup off and stale SUCCEEDED emits Idle`() {
        assertEquals(
            WarmupState.Idle,
            reduceWarmupState(
                hasAttachedModel = true,
                workInfoState = WorkInfo.State.SUCCEEDED,
                launchWarmupEnabled = false,
            ),
        )
    }

    @Test
    fun `attached with launch warmup off and active work emits Warming`() {
        listOf(
            WorkInfo.State.ENQUEUED,
            WorkInfo.State.RUNNING,
            WorkInfo.State.BLOCKED,
        ).forEach { workState ->
            assertEquals(
                "expected Warming for on-demand + $workState",
                WarmupState.Warming,
                reduceWarmupState(
                    hasAttachedModel = true,
                    workInfoState = workState,
                    launchWarmupEnabled = false,
                ),
            )
        }
    }

    // --- Launch warmup enabled ----------------------------------------------

    @Test
    fun `attached and work not yet enqueued emits Warming when launch warmup on`() {
        assertEquals(
            WarmupState.Warming,
            reduceWarmupState(
                hasAttachedModel = true,
                workInfoState = null,
                launchWarmupEnabled = true,
            ),
        )
    }

    @Test
    fun `attached and ENQUEUED RUNNING BLOCKED all map to Warming when launch warmup on`() {
        listOf(
            WorkInfo.State.ENQUEUED,
            WorkInfo.State.RUNNING,
            WorkInfo.State.BLOCKED,
        ).forEach { workState ->
            assertEquals(
                "expected Warming for $workState",
                WarmupState.Warming,
                reduceWarmupState(
                    hasAttachedModel = true,
                    workInfoState = workState,
                    launchWarmupEnabled = true,
                ),
            )
        }
    }

    @Test
    fun `attached and SUCCEEDED emits Ready when launch warmup on`() {
        assertEquals(
            WarmupState.Ready,
            reduceWarmupState(
                hasAttachedModel = true,
                workInfoState = WorkInfo.State.SUCCEEDED,
                launchWarmupEnabled = true,
            ),
        )
    }

    @Test
    fun `attached and FAILED emits Failed when launch warmup on`() {
        assertEquals(
            WarmupState.Failed,
            reduceWarmupState(
                hasAttachedModel = true,
                workInfoState = WorkInfo.State.FAILED,
                launchWarmupEnabled = true,
            ),
        )
    }

    @Test
    fun `attached and CANCELLED emits Failed - we do not loop forever`() {
        assertEquals(
            WarmupState.Failed,
            reduceWarmupState(
                hasAttachedModel = true,
                workInfoState = WorkInfo.State.CANCELLED,
                launchWarmupEnabled = true,
            ),
        )
    }
}
