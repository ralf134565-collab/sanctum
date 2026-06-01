// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.ai

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.pocketreflect.app.core.locale.AppLanguageResolver
import com.pocketreflect.app.data.local.entity.JournalEntry
import com.pocketreflect.app.data.repository.ModelSelectionRepository
import com.pocketreflect.app.domain.ai.GemmaLocalEngine
import com.pocketreflect.app.domain.ai.prompts.JournalPrompts
import com.pocketreflect.app.domain.chat.ChatMessage
import com.pocketreflect.app.domain.chat.ChatPersona
import com.pocketreflect.app.domain.chat.prompts.ChatPrompts
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Реализация [GemmaLocalEngine] поверх LiteRT-LM 0.11 (`com.google.ai.edge.litertlm`).
 *
 *  ### Жизненный цикл
 *  - `Engine` — **singleton на процесс**, ленивая инициализация при первом
 *    обращении. `initialize()` грузит модель из `filesDir/models/...litertlm`
 *    (до ~10 с на E2B), поэтому держим её живой между вызовами.
 *  - `Conversation` — **per-request**. Каждая запись дневника = независимый
 *    контекст; persistent Conversation между записями был бы privacy-протечкой
 *    и source of confusion (история из вчерашнего разговора влияет на
 *    сегодняшний). KV-cache reuse — отдельная будущая оптимизация.
 *
 *  ### Backend selection (#3c — user-controlled + GPU fallback)
 *  В [ensureEngine] читаем `selectedBackend` из [ModelSelectionRepository]
 *  (дефолт `GPU`). Если выбран GPU и init упал — молча откатываемся на CPU
 *  (graceful degradation на эмуляторе и устройствах без OpenCL). Если выбран
 *  CPU и упал — пробрасываем ошибку (это аномалия, маскировать GPU-фоллбэком
 *  было бы непредсказуемо). При смене preference между запросами
 *  [activeBackend] перестаёт соответствовать желаемому → engine `close()` +
 *  пересоздаётся.
 *
 *  ### Конкурентность
 *  - [initMutex] защищает init / release / переход состояний движка.
 *  - [engine] помечен `@Volatile` чтобы [isReady] читал актуальное значение
 *    без блокировки.
 *  - `Conversation` создаётся ВНУТРИ [generate], не разделяется между корутинами.
 *
 *  ### Что НЕ делаем здесь
 *  - Не обрабатываем «модель не подключена» — это работа [EngineCoordinator].
 *    Мы просто бросаем `IllegalStateException` и доверяем, что декоратор
 *    переключится на mock.
 *  - Не парсим JSON-структуру в [summarizeWeek] — `structuredJson` остаётся
 *    `null` до отдельного PR.
 */
@Singleton
class LiteRtGemmaEngine @Inject constructor(
    private val modelSelectionRepo: ModelSelectionRepository,
    private val languageResolver: AppLanguageResolver,
    @ApplicationContext private val context: Context,
) : GemmaLocalEngine {

    /**
     * Защищает создание/освобождение/реинициализацию [engine].
     * Отделён от [inferenceMutex], чтобы держать ortogonal: пока идёт
     * инференс, другой запрос не должен ждать init-mutex впустую
     * (он уже знает, что engine готов).
     */
    private val initMutex = Mutex()

    /**
     * Защищает САМ `Conversation.sendMessage()` от параллельных вызовов.
     *
     * LiteRT-LM 0.11 имеет жёсткое ограничение нативной стороны:
     *   "FAILED_PRECONDITION: A session already exists.
     *    Only one session is supported at a time."
     * (см. `Engine.createConversation` → JNI).
     *
     * Без сериализации второй inference, запущенный пока первый ещё
     * крутит `sendMessage` (а Kotlin-cancel НЕ останавливает блокирующий
     * native call), мгновенно падает с `LiteRtLmJniException`. В UI это
     * было видно как «отклик появился, потом сменился» — на самом деле
     * первый ответ был mock-fallback от EngineCoordinator, второй —
     * запоздалый real-ответ от первого corutine'а, который никто не отменил.
     */
    private val inferenceMutex = Mutex()

    @Volatile
    private var engine: Engine? = null

    @Volatile
    private var activeBackend: EngineBackend? = null

    override suspend fun generatePromptResponse(
        entry: JournalEntry,
        personalManifesto: String?,
    ): String =
        withContext(Dispatchers.Default) {
            val language = languageResolver.resolvedNow()
            val eng = ensureEngine()
            inferenceMutex.withLock {
                generate(
                    eng,
                    userPrompt = JournalPrompts.buildPromptInput(entry, language, personalManifesto),
                    systemInstruction = JournalPrompts.systemInstruction(language),
                    kind = "prompt",
                )
            }
        }

    override suspend fun summarizeWeek(
        entries: List<JournalEntry>,
        personalManifesto: String?,
    ): GemmaLocalEngine.WeeklySummary = withContext(Dispatchers.Default) {
        val language = languageResolver.resolvedNow()
        val eng = ensureEngine()
        val text = inferenceMutex.withLock {
            generate(
                eng,
                userPrompt = JournalPrompts.buildSummaryInput(entries, language, personalManifesto),
                systemInstruction = JournalPrompts.weeklySystemInstruction(language),
                kind = "summary",
            )
        }

        val moodTagCounts = entries.flatMap { it.moodTags }
            .groupBy { it.storageKey }
            .mapValues { it.value.size }
        val startDate = entries.minOfOrNull { it.dayBucket }
        val endDate = entries.maxOfOrNull { it.dayBucket }

        val report = WeeklyTrendReport(
            entryCount = entries.size,
            startDate = startDate,
            endDate = endDate,
            tagFrequencies = moodTagCounts
        )
        val jsonStr = try {
            Json.encodeToString(WeeklyTrendReport.serializer(), report)
        } catch (e: Exception) {
            null
        }

        GemmaLocalEngine.WeeklySummary(
            humanReadable = text,
            structuredJson = jsonStr,
        )
    }

    override suspend fun isReady(): Boolean = engine != null

    override fun streamChat(
        history: List<ChatMessage>,
        persona: ChatPersona,
        journalSnippet: String?,
        manifestoSnippet: String?,
        customPersonaPrompt: String?,
    ): Flow<String> = flow {
        val language = languageResolver.resolvedNow()
        val eng = ensureEngine()
        val prompt = ChatPrompts.buildChatUserPrompt(
            history,
            journalSnippet,
            persona,
            language,
            manifestoSnippet,
        )
        val systemInstruction = ChatPrompts.systemInstructionFor(
            persona = persona,
            language = language,
            customPersonaPrompt = customPersonaPrompt,
        )
        try {
            inferenceMutex.withLock {
                eng.createConversation(
                    ConversationConfig(
                        systemInstruction = Contents.of(systemInstruction),
                        samplerConfig = DEFAULT_SAMPLER,
                    ),
                ).use { conversation ->
                    // LiteRT шлёт в Flow отдельные токены (Message = один фрагмент),
                    // а не нарастающий полный текст. Старая логика substring от
                    // «полного» ответа отбрасывала почти все чанки → в UI мелькали
                    // отдельные бессмысленные куски.
                    conversation.sendMessageAsync(prompt).collect { message ->
                        val piece = message.toString()
                        if (piece.isEmpty() || isLikelyControlToken(piece)) return@collect
                        emit(piece)
                    }
                }
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            Log.i(TAG, "streamChat cancelled, releasing engine to prevent JNI lockups")
            release()
            throw cancelled
        }
    }.flowOn(Dispatchers.Default)

    /**
     * Прогревочный путь: просто триггерим `ensureEngine`, не открывая
     * `Conversation`. Тяжёлая часть — `Engine.initialize()` под мьютексом,
     * именно её мы и хотим унести в WorkManager до первого пользовательского
     * запроса. Идемпотентность даёт сама `ensureEngine`: если engine уже
     * поднят и backend совпадает с preference — `withLock { return existing }`.
     */
    override suspend fun warmUp() = withContext(Dispatchers.Default) {
        Log.i(TAG, "warmUp() invoked")
        ensureEngine()
        Unit
    }

    override suspend fun release() {
        initMutex.withLock {
            val current = engine
            if (current != null) {
                runCatching { current.close() }
                    .onFailure { Log.w(TAG, "Engine close failed, ignored", it) }
                engine = null
                activeBackend = null
            }
        }
    }

    /**
     * Один синхронный запрос → один полный ответ. Streaming отложен в
     * отдельный PR — для UI «итогов дня» one-shot достаточен и проще в VM.
     *
     * `Message.toString()` делегирует на `Contents.toString()`, который
     * склеивает все `Content.Text` через `joinToString("")` — это
     * текстовое представление ответа, безопасное для тех моделей, что
     * иногда возвращают несколько `Content.Text` чанков подряд.
     */
    private fun generate(eng: Engine, userPrompt: String, systemInstruction: String, kind: String): String {
        val startNanos = System.nanoTime()
        Log.i(TAG, "Inference started: kind=$kind, prompt-length=${userPrompt.length} chars")
        val result = eng.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of(systemInstruction),
                samplerConfig = DEFAULT_SAMPLER,
            ),
        ).use { conversation ->
            conversation.sendMessage(userPrompt).toString().trim()
        }
        val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000
        Log.i(
            TAG,
            "Inference completed: kind=$kind, elapsed=${elapsedMs}ms, response-length=${result.length} chars",
        )
        return result
    }

    override suspend fun summarizeChat(history: List<ChatMessage>): String = withContext(Dispatchers.Default) {
        val language = languageResolver.resolvedNow()
        val eng = ensureEngine()
        val userPrompt = if (language == com.pocketreflect.app.core.locale.AppLanguage.RU) {
            "Напиши краткое содержание (2-3 предложения) следующего диалога на русском языке. Напиши только само содержание, без вводных слов:\n\n" +
                    history.joinToString("\n") { "${it.role}: ${it.content}" }
        } else {
            "Write a brief summary (2-3 sentences) of the following dialogue. Write only the summary itself, without intro phrases:\n\n" +
                    history.joinToString("\n") { "${it.role}: ${it.content}" }
        }
        val systemInstruction = if (language == com.pocketreflect.app.core.locale.AppLanguage.RU) {
            "Ты — ассистент, который делает краткие и содержательные выводы диалога."
        } else {
            "You are an assistant that writes brief and informative summaries of dialogues."
        }
        inferenceMutex.withLock {
            generate(
                eng,
                userPrompt = userPrompt,
                systemInstruction = systemInstruction,
                kind = "summary",
            )
        }
    }

    /**
     * Лениво создаёт и инициализирует [Engine]. Под [initMutex], потому что:
     *  - ViewModel может параллельно вызвать
     *    `generatePromptResponse` / `summarizeWeek` на одном singleton.
     *  - Двойной `Engine.initialize()` на одном файле модели — undefined
     *    поведение нативной стороны.
     *
     * Дополнительно проверяет рассогласование `activeBackend` с актуальным
     * `selectedBackend` — если пользователь переключил GPU↔CPU в Settings,
     * закрываем существующий engine и пересоздаём под новым бэкендом.
     * Затратно (10–15 с на E2B), но переключение редкое.
     */
    private suspend fun ensureEngine(): Engine = initMutex.withLock {
        val desiredBackend = modelSelectionRepo.selectedBackend.first()

        engine?.let { existing ->
            if (activeBackend == desiredBackend) return@withLock existing
            Log.i(TAG, "Backend preference changed: $activeBackend → $desiredBackend, reinitialising")
            runCatching { existing.close() }
                .onFailure { Log.w(TAG, "Engine close failed during backend switch, ignored", it) }
            engine = null
            activeBackend = null
        }

        val attached = modelSelectionRepo.attached.first()
            ?: throw IllegalStateException(
                "LiteRtGemmaEngine: model is not attached. EngineCoordinator " +
                    "должен был отфильтровать этот путь и отдать запрос на mock.",
            )
        val modelPath = attached.absolutePath

        val newEngine = runCatching { createAndInit(modelPath, desiredBackend) }
            .recoverCatching { primaryFailure ->
                if (desiredBackend == EngineBackend.GPU) {
                    Log.w(
                        TAG,
                        "GPU backend init failed; falling back to CPU. Reason: ${primaryFailure.message}",
                        primaryFailure,
                    )
                    createAndInit(modelPath, EngineBackend.CPU)
                } else {
                    // Пользователь явно выбрал CPU и оно не поднялось — это аномалия.
                    // Не маскируем GPU-фоллбэком: пробрасываем, EngineCoordinator уйдёт на mock.
                    throw primaryFailure
                }
            }
            .getOrThrow()

        engine = newEngine
        newEngine
    }

    private fun createAndInit(modelPath: String, backend: EngineBackend): Engine {
        val backendImpl: Backend = when (backend) {
            EngineBackend.GPU -> Backend.GPU()
            EngineBackend.CPU -> Backend.CPU()
        }
        val cfg = EngineConfig(
            modelPath = modelPath,
            backend = backendImpl,
            cacheDir = context.cacheDir.absolutePath,
        )
        Log.i(TAG, "Engine.initialize() starting: backend=$backend, modelPath=$modelPath")
        val initStartNanos = System.nanoTime()
        val instance = Engine(cfg)
        instance.initialize()
        val initElapsedMs = (System.nanoTime() - initStartNanos) / 1_000_000
        activeBackend = backend
        Log.i(TAG, "Engine.initialize() succeeded: backend=$backend, elapsed=${initElapsedMs}ms")
        return instance
    }

    /** Служебные токены шаблона Gemma — не показываем в UI. */
    private fun isLikelyControlToken(piece: String): Boolean =
        piece.contains("<|") || piece.contains("|>")

    private companion object {
        const val TAG = "LiteRtGemmaEngine"

        /**
         * Семплер калиброван под «эмпатичный отклик».
         *
         * Калибровка (эмпирически на Gemma 4 E2B int4, CPU и GPU):
         *  - `temperature = 0.55` — для эмпатичного письма низкая temperature даёт
         *     **более точные** отклики. Высокая temperature (0.7+) для Gemma instruct
         *     вытаскивает «попсовые» токены, что = клише («ты молодец», «отдохни»).
         *     Диапазон 0.5–0.6 — sweet spot для inst-моделей этого семейства.
         *  - `topP = 0.92` — оставляем чуть больше пространства для редких удачных
         *     формулировок, чем стандартные 0.9.
         *  - `topK = 40` — шире лексический выбор: при topK=20 модель часто
         *     выбирала одно и то же слово на одинаковую структуру входа.
         *     Удвоение topK даёт ощутимое разнообразие без потери связности.
         *
         * NB: если в будущем добавим выбор «характера» (D4 backlog), будет
         * отдельный sampler-preset на персону.
         */
        val DEFAULT_SAMPLER = SamplerConfig(
            topK = 40,
            topP = 0.92,
            temperature = 0.55,
        )
    }
}

@Serializable
internal data class WeeklyTrendReport(
    val entryCount: Int,
    val startDate: String?,
    val endDate: String?,
    val tagFrequencies: Map<String, Int>
)
