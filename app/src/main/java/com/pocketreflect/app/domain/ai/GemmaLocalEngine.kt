// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.ai

import com.pocketreflect.app.data.local.entity.JournalEntry
import com.pocketreflect.app.domain.chat.ChatMessage
import com.pocketreflect.app.domain.chat.ChatPersona
import kotlinx.coroutines.flow.Flow

/**
 * Абстракция над локальным LLM-движком (Gemma 4 через LiteRT-LM).
 *
 * Принципиально:
 *  - Интерфейс знает ТОЛЬКО про доменные сущности (JournalEntry).
 *    Никаких намёков на TFLite/LiteRT — это деталь реализации,
 *    которую мы заменим в `data` слое после интеграции рантайма.
 *  - Реализация ОБЯЗАНА быть полностью оффлайн. Любой сетевой вызов в
 *    реализации — нарушение продуктового инварианта (Local-First).
 *  - Все методы `suspend`: инференс модели тяжёлый, его нужно уносить
 *    из main thread на специально выделенный диспатчер
 *    (см. `Dispatchers.Default` или собственный single-thread executor,
 *    чтобы не пересоздавать KV-cache на каждый вызов).
 *
 *  Раздел еженедельной суммаризации специально вынесен в отдельный метод:
 *  у него другой prompt-template и другая длина контекста.
 */
interface GemmaLocalEngine {

    /**
     * Сформировать эмпатичный отклик ИИ-ментора по текущему дню.
     * Должен опираться на теги, микро-победы и рефлексию, но не «вытаскивать»
     * пользователя на оценочные суждения. Стиль — поддерживающий, бережный.
     */
    suspend fun generatePromptResponse(
        entry: JournalEntry,
        personalManifesto: String? = null,
    ): String

    /**
     * Сжать историю N последних дней в компактный профиль ментальных трендов
     * (вызывается из WorkManager раз в неделю — см. AITrendProfile).
     */
    suspend fun summarizeWeek(
        entries: List<JournalEntry>,
        personalManifesto: String? = null,
    ): WeeklySummary

    /**
     * Потоковый ответ для вкладки «Чат». Эмитит нарастающий текст
     * (или целые чанки — зависит от runtime).
     */
    fun streamChat(
        history: List<ChatMessage>,
        persona: ChatPersona,
        journalSnippet: String?,
        manifestoSnippet: String? = null,
    ): Flow<String>

    /** Готова ли модель к инференсу (загружена в память, прогрета). */
    suspend fun isReady(): Boolean

    /**
     * Прогреть движок без производства полезного ответа: загрузить модель в
     * нативную память, чтобы первый реальный `generatePromptResponse` не висел
     * 10–30 секунд под пользовательским вводом.
     *
     * Вызывается из `ModelWarmupWorker` через WorkManager на cold-start
     * после привязки модели. Должен быть **идемпотентным**: повторный вызов
     * на уже прогретом движке — no-op.
     *
     * Подразумевается, что реализация сама решит, нужно ли что-то делать:
     *  - mock — noop;
     *  - real — `ensureEngine()` без последующего `Conversation`;
     *  - coordinator — делегирует real, только если модель привязана.
     *
     * При ошибке инициализации движок **не обязан** бросать — координатор и
     * worker должны воспринимать warmUp как best-effort. Внутри реализации
     * exception всё равно может пробросится — это нормально, worker его поймает.
     */
    suspend fun warmUp()

    /** Снять модель с памяти, освободить ресурсы (вызывается при onTrimMemory). */
    suspend fun release()

    /** Сформировать краткое саммари (2-3 предложения) истории диалога для очистки контекста. */
    suspend fun summarizeChat(history: List<ChatMessage>): String

    data class WeeklySummary(
        /** Краткая человеко-читаемая выжимка (то, что увидит пользователь). */
        val humanReadable: String,
        /** Машинное представление (JSON) — для будущих графиков и контекста модели. */
        val structuredJson: String?,
    )
}
