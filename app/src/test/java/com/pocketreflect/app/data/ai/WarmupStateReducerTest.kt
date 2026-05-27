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
        // Семантика: DataStore ещё не прочитан. Любое значение WorkManager
        // тут ничего не значит, потому что мы даже не знаем, нужен ли warmup.
        WorkInfo.State.entries.forEach { workState ->
            assertEquals(
                "expected Unknown for null attached + $workState",
                WarmupState.Unknown,
                reduceWarmupState(hasAttachedModel = null, workInfoState = workState),
            )
        }
        assertEquals(
            WarmupState.Unknown,
            reduceWarmupState(hasAttachedModel = null, workInfoState = null),
        )
    }

    // --- No model attached --------------------------------------------------

    @Test
    fun `not attached emits NoModel and ignores any work state`() {
        // Если модель удалили, прошлый worker мог остаться в SUCCEEDED — это
        // не должно сбивать gate: показать «модели нет» важнее, чем «warmup ok».
        WorkInfo.State.entries.forEach { workState ->
            assertEquals(
                "expected NoModel for hasAttached=false + $workState",
                WarmupState.NoModel,
                reduceWarmupState(hasAttachedModel = false, workInfoState = workState),
            )
        }
        assertEquals(
            WarmupState.NoModel,
            reduceWarmupState(hasAttachedModel = false, workInfoState = null),
        )
    }

    // --- Attached + various work states ------------------------------------

    @Test
    fun `attached and work not yet enqueued emits Warming`() {
        // Mаленький временной зазор между «увидели attached» и «enqueue работы»
        // не должен пропускать пользователя в Today (иначе он успеет нажать
        // «Завершить день» до того, как engine начнёт грузиться).
        assertEquals(
            WarmupState.Warming,
            reduceWarmupState(hasAttachedModel = true, workInfoState = null),
        )
    }

    @Test
    fun `attached and ENQUEUED RUNNING BLOCKED all map to Warming`() {
        // Эти три состояния различает только WorkManager-внутрь, для UI они
        // эквивалентны: «модель ещё греется».
        listOf(
            WorkInfo.State.ENQUEUED,
            WorkInfo.State.RUNNING,
            WorkInfo.State.BLOCKED,
        ).forEach { workState ->
            assertEquals(
                "expected Warming for $workState",
                WarmupState.Warming,
                reduceWarmupState(hasAttachedModel = true, workInfoState = workState),
            )
        }
    }

    @Test
    fun `attached and SUCCEEDED emits Ready`() {
        assertEquals(
            WarmupState.Ready,
            reduceWarmupState(
                hasAttachedModel = true,
                workInfoState = WorkInfo.State.SUCCEEDED,
            ),
        )
    }

    @Test
    fun `attached and FAILED emits Failed`() {
        assertEquals(
            WarmupState.Failed,
            reduceWarmupState(
                hasAttachedModel = true,
                workInfoState = WorkInfo.State.FAILED,
            ),
        )
    }

    @Test
    fun `attached and CANCELLED emits Failed - we do not loop forever`() {
        // CANCELLED для пользователя неотличимо от FAILED: модель не прогрета,
        // движок не готов. Если показывать «отмена» как «всё ещё идёт» —
        // bootstrap залипнет навсегда после, например, отмены вручную из
        // настроек разработчика. Считаем терминальным.
        assertEquals(
            WarmupState.Failed,
            reduceWarmupState(
                hasAttachedModel = true,
                workInfoState = WorkInfo.State.CANCELLED,
            ),
        )
    }
}
