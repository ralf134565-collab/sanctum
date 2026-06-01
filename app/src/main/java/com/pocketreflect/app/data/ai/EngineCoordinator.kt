// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.ai

import android.util.Log
import com.pocketreflect.app.data.local.entity.JournalEntry
import com.pocketreflect.app.data.repository.ModelSelectionRepository
import com.pocketreflect.app.domain.ai.AiEngineStatusSource
import com.pocketreflect.app.domain.ai.GemmaLocalEngine
import com.pocketreflect.app.domain.chat.ChatMessage
import com.pocketreflect.app.domain.chat.ChatPersona
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first

/**
 * Decorator над [GemmaLocalEngine], который выбирает между реальной
 * LiteRT-LM реализацией и мок-заглушкой в зависимости от состояния
 * подключённой модели и от runtime-успешности real-движка.
 *
 * ### Решения, делегируемые coordinator'у
 *  1. **Нет модели** → запрос идёт сразу в [mock]. Это позволяет VM и
 *     Worker'ам быть «слепыми» к тому, есть ли модель: они всегда зовут
 *     `GemmaLocalEngine`, а UX «модель не подключена» решается экраном
 *     `ModelSettingsScreen`, не отсутствием ответа.
 *  2. **Real падает** (init failed / native crash / OOM в `initialize` /
 *     `LiteRtLmJniException` в `sendMessage`) → fallback в [mock], в логах
 *     остаётся причина. Пользователь не видит «пустой ответ» — приложение
 *     отвечает голосом mock'а, который заведомо приемлем.
 *  3. **CancellationException пробрасывается явно**: координатор НЕ
 *     перехватывает отмену корутины как ошибку real-движка, иначе мы
 *     случайно «оживляем» отменённый запрос на mock'е и нарушаем
 *     contract structured concurrency.
 *
 * Биндинг устроен так: публичный `GemmaLocalEngine @Binds → EngineCoordinator`
 * (см. `di/AIModule.kt`), а сам coordinator получает quantifier-биндинги
 * `@Named("real")` → `LiteRtGemmaEngine` и `@Named("mock")` → `MockGemmaLocalEngine`.
 * Этим всё дерево VM остаётся неизменным после Sub-PR #2.
 */
@Singleton
class EngineCoordinator @Inject constructor(
    @Named("real") private val real: GemmaLocalEngine,
    @Named("mock") private val mock: GemmaLocalEngine,
    private val modelSelectionRepo: ModelSelectionRepository,
    private val aiEngineStatusSource: AiEngineStatusSource,
) : GemmaLocalEngine {

    override suspend fun generatePromptResponse(
        entry: JournalEntry,
        personalManifesto: String?,
    ): String =
        runWithFallback(
            realBlock = { real.generatePromptResponse(entry, personalManifesto) },
            mockBlock = { mock.generatePromptResponse(entry, personalManifesto) },
        )

    override suspend fun summarizeWeek(
        entries: List<JournalEntry>,
        personalManifesto: String?,
    ): GemmaLocalEngine.WeeklySummary =
        runWithFallback(
            realBlock = { real.summarizeWeek(entries, personalManifesto) },
            mockBlock = { mock.summarizeWeek(entries, personalManifesto) },
        )

    override fun streamChat(
        history: List<ChatMessage>,
        persona: ChatPersona,
        journalSnippet: String?,
        manifestoSnippet: String?,
        customPersonaPrompt: String?,
    ): Flow<String> = flow {
        if (modelSelectionRepo.attached.first() == null) {
            Log.i(TAG, "streamChat → MOCK (no model attached)")
            mock.streamChat(history, persona, journalSnippet, manifestoSnippet, customPersonaPrompt)
                .collect { emit(it) }
            return@flow
        }
        Log.i(TAG, "streamChat → REAL")
        try {
            real.streamChat(history, persona, journalSnippet, manifestoSnippet, customPersonaPrompt)
                .collect { emit(it) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (t: Throwable) {
            Log.w(TAG, "streamChat real failed, falling back to mock", t)
            aiEngineStatusSource.notifyRuntimeFailure()
            mock.streamChat(history, persona, journalSnippet, manifestoSnippet, customPersonaPrompt)
                .collect { emit(it) }
        }
    }

    /** Готов хоть один из движков. */
    override suspend fun isReady(): Boolean = real.isReady() || mock.isReady()

    /**
     * Прогрев имеет смысл только для real-движка, и только когда модель
     * привязана. Иначе real гарантированно бросит `IllegalStateException`
     * («model is not attached»), и мы зря потратим WorkManager-цикл и шум
     * в логах. Mock warmUp — noop, его не зовём.
     *
     * `CancellationException` пробрасывается явно (как и в `runWithFallback`):
     * structured concurrency требует, чтобы отмена корутины не маскировалась.
     */
    override suspend fun warmUp() {
        val hasModel = modelSelectionRepo.attached.first() != null
        if (!hasModel) {
            Log.i(TAG, "warmUp() skipped: no model attached")
            return
        }
        Log.i(TAG, "warmUp() delegating to real engine")
        try {
            real.warmUp()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (t: Throwable) {
            Log.w(TAG, "Real engine warmUp failed; production will fall back to mock at first inference", t)
        }
    }

    /**
     * Освобождаем только real. У mock release — noop, и нет смысла его дёргать;
     * к тому же это вызывается из `onTrimMemory`, где мы хотим освободить
     * именно тяжёлый native-engine.
     */
    override suspend fun release() {
        real.release()
    }

    override suspend fun summarizeChat(history: List<ChatMessage>): String =
        runWithFallback(
            realBlock = { real.summarizeChat(history) },
            mockBlock = { mock.summarizeChat(history) },
        )

    /**
     * Шаблонный метод вокруг real → mock fallback'а.
     *
     * inline + crossinline нужны, чтобы вызовы `real.foo()` / `mock.foo()`
     * не оборачивались в лямбды и `suspend` правильно прокидывался.
     */
    private suspend inline fun <T> runWithFallback(
        crossinline realBlock: suspend () -> T,
        crossinline mockBlock: suspend () -> T,
    ): T {
        if (modelSelectionRepo.attached.first() == null) {
            Log.i(TAG, "Routing to MOCK engine (no model attached)")
            return mockBlock()
        }
        Log.i(TAG, "Routing to REAL engine")
        return try {
            realBlock()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (t: Throwable) {
            Log.w(TAG, "Real engine failed, falling back to mock", t)
            aiEngineStatusSource.notifyRuntimeFailure()
            mockBlock()
        }
    }

    private companion object {
        const val TAG = "EngineCoordinator"
    }
}
