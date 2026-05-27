// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.core.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pocketreflect.app.domain.ai.GemmaLocalEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

/**
 * Однократный worker, который греет нативный LiteRT-LM движок до первого
 * пользовательского `generatePromptResponse` (Sub-PR #4).
 *
 * Почему именно WorkManager, а не корутина в `Application.onCreate`:
 *  - **Application.onCreate** должен оставаться лёгким (< 50 мс), иначе
 *    cold start превращается в ANR-ловушку. `Engine.initialize()` для E2B
 *    держит main thread до 10 с.
 *  - **WorkManager** даёт resilience: если процесс убьют во время загрузки
 *    (например, system pressure от других приложений), задача перезапустится
 *    при следующем старте — пользователь не теряет прогрев навсегда.
 *  - **OneTime + ExistingWorkPolicy.KEEP** — гарантия, что два параллельных
 *    enqueue (например, из VM и из push-notification flow в будущем) не
 *    запустят две инициализации одного нативного движка одновременно.
 *
 * Контракт:
 *  - Делегирует на `GemmaLocalEngine.warmUp()` (это `EngineCoordinator`,
 *    который сам решит, есть ли смысл будить native real).
 *  - Жёсткий таймаут 60 секунд: модели E2B/E4B на средних устройствах
 *    греются за 10–30 с; 60 с — это «уже точно что-то пошло не так».
 *    `withTimeout` отменяет underlying coroutine, что прокидывается в
 *    нативную сторону через cancel-flag в `Engine.initialize()`.
 *  - При таймауте/исключении возвращаем `Result.failure()` — `WarmupCoordinator`
 *    свернёт UI в состояние [com.pocketreflect.app.data.ai.WarmupState.Failed],
 *    и пользователь увидит «работаем в режиме поддержки» (mock-fallback в
 *    `EngineCoordinator` уже подменит реальный движок при первом инференсе).
 *  - **Не используем `Result.retry()`** — если LiteRT-LM не поднялся с
 *    первого раза на этом устройстве, второй заход с тем же файлом и тем же
 *    backend'ом ничего не изменит. Лучше честно показать fallback-баннер.
 */
@HiltWorker
class ModelWarmupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val engine: GemmaLocalEngine,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = try {
        Log.i(TAG, "doWork() starting")
        val startNanos = System.nanoTime()
        withTimeout(WARMUP_TIMEOUT) {
            engine.warmUp()
        }
        val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000
        Log.i(TAG, "warmUp completed successfully: elapsed=${elapsedMs}ms")
        Result.success()
    } catch (t: TimeoutCancellationException) {
        Log.w(TAG, "warmUp timed out after ${WARMUP_TIMEOUT.inWholeSeconds}s", t)
        Result.failure()
    } catch (t: Throwable) {
        Log.w(TAG, "warmUp failed", t)
        Result.failure()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "model_warmup"
        private const val TAG = "ModelWarmupWorker"
        private val WARMUP_TIMEOUT = 60.seconds
    }
}
