// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.ai

import com.pocketreflect.app.data.local.entity.JournalEntry
import com.pocketreflect.app.data.model.ModelVariant
import com.pocketreflect.app.data.repository.AttachedModel
import com.pocketreflect.app.data.repository.ModelSelectionRepository
import com.pocketreflect.app.domain.ai.GemmaLocalEngine
import com.pocketreflect.app.domain.chat.ChatMessage
import com.pocketreflect.app.domain.chat.ChatPersona
import com.pocketreflect.app.domain.model.MoodTag
import com.pocketreflect.app.testing.FakeAiEngineStatusSource
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Юнит-тесты на [EngineCoordinator] — pure JVM, без LiteRT, без Robolectric.
 *
 * Стратегия: подменяем real/mock реализации [GemmaLocalEngine] на fake'и,
 * `ModelSelectionRepository` — на MutableStateFlow-обёртку. Проверяем три
 * ключевых сценария + cancellation contract.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EngineCoordinatorTest {

    @Test
    fun `no model attached - uses mock without touching real`() = runTest {
        val real = FakeEngine(promptResponse = "real")
        val mock = FakeEngine(promptResponse = "mock-answer")
        val coordinator = coordinator(real, mock, attached = null)

        val result = coordinator.generatePromptResponse(sampleEntry())

        assertEquals("mock-answer", result)
        assertEquals(0, real.promptCalls)
    }

    @Test
    fun `model attached and real succeeds - returns real answer`() = runTest {
        val real = FakeEngine(promptResponse = "real-answer")
        val mock = FakeEngine(promptResponse = "mock")
        val coordinator = coordinator(real, mock, attached = sampleAttached())

        val result = coordinator.generatePromptResponse(sampleEntry())

        assertEquals("real-answer", result)
        assertEquals(1, real.promptCalls)
        assertEquals(0, mock.promptCalls)
    }

    @Test
    fun `model attached but real throws - falls back to mock`() = runTest {
        val real = FakeEngine(promptThrows = RuntimeException("simulated jni crash"))
        val mock = FakeEngine(promptResponse = "fallback-mock")
        val coordinator = coordinator(real, mock, attached = sampleAttached())

        val result = coordinator.generatePromptResponse(sampleEntry())

        assertEquals("fallback-mock", result)
        assertEquals(1, real.promptCalls)
        assertEquals(1, mock.promptCalls)
    }

    @Test
    fun `cancellation propagates and does not trigger mock fallback`() = runTest {
        val real = FakeEngine(promptThrows = CancellationException("cancelled"))
        val mock = FakeEngine(promptResponse = "should-not-be-called")
        val coordinator = coordinator(real, mock, attached = sampleAttached())

        try {
            coordinator.generatePromptResponse(sampleEntry())
            fail("CancellationException должен был пробросится, а не быть проглочен в mock-fallback")
        } catch (e: CancellationException) {
            assertEquals("cancelled", e.message)
        }
        assertEquals("mock не должен вызываться при отмене корутины", 0, mock.promptCalls)
    }

    @Test
    fun `summarizeWeek follows the same fallback rule as generatePromptResponse`() = runTest {
        val realSummary = GemmaLocalEngine.WeeklySummary(humanReadable = "real-week", structuredJson = null)
        val mockSummary = GemmaLocalEngine.WeeklySummary(humanReadable = "mock-week", structuredJson = null)
        val real = FakeEngine(summary = realSummary, summaryThrows = IllegalStateException("oom"))
        val mock = FakeEngine(summary = mockSummary)
        val coordinator = coordinator(real, mock, attached = sampleAttached())

        val result = coordinator.summarizeWeek(entries = listOf(sampleEntry()))

        assertSame(mockSummary, result)
    }

    @Test
    fun `warmUp skips real when no model attached`() = runTest {
        val real = FakeEngine()
        val mock = FakeEngine()
        val coordinator = coordinator(real, mock, attached = null)

        coordinator.warmUp()

        assertEquals("real.warmUp не должен вызываться без модели", 0, real.warmUpCalls)
        assertEquals("mock.warmUp не должен вызываться никогда", 0, mock.warmUpCalls)
    }

    @Test
    fun `warmUp delegates to real when model attached`() = runTest {
        val real = FakeEngine()
        val mock = FakeEngine()
        val coordinator = coordinator(real, mock, attached = sampleAttached())

        coordinator.warmUp()

        assertEquals(1, real.warmUpCalls)
        assertEquals(0, mock.warmUpCalls)
    }

    @Test
    fun `warmUp swallows real failure - production path will mock-fallback later`() = runTest {
        // Семантика: warmUp — best-effort. Если он упал, это не блокер для UX:
        // bootstrap-экран отпустит пользователя в Today, а первый реальный
        // инференс уже потом сам уйдёт на mock через runWithFallback.
        val real = FakeEngine(warmUpThrows = RuntimeException("simulated jni crash on init"))
        val mock = FakeEngine()
        val coordinator = coordinator(real, mock, attached = sampleAttached())

        coordinator.warmUp()

        assertEquals(1, real.warmUpCalls)
    }

    @Test
    fun `warmUp propagates CancellationException`() = runTest {
        val real = FakeEngine(warmUpThrows = CancellationException("cancelled during warmup"))
        val mock = FakeEngine()
        val coordinator = coordinator(real, mock, attached = sampleAttached())

        try {
            coordinator.warmUp()
            fail("CancellationException должен пробрасываться, а не глушиться")
        } catch (e: CancellationException) {
            assertEquals("cancelled during warmup", e.message)
        }
    }

    @Test
    fun `release delegates to real engine only`() = runTest {
        val real = FakeEngine()
        val mock = FakeEngine()
        val coordinator = coordinator(real, mock, attached = sampleAttached())

        coordinator.release()

        assertTrue("real.release должен вызываться из координатора", real.releaseCalled)
        assertFalse("mock.release не должен вызываться", mock.releaseCalled)
    }

    @Test
    fun `isReady is OR of real and mock`() = runTest {
        val realReady = FakeEngine(isReady = true)
        val mockReady = FakeEngine(isReady = true)
        val notReady = FakeEngine(isReady = false)

        assertTrue(coordinator(realReady, notReady, attached = sampleAttached()).isReady())
        assertTrue(coordinator(notReady, mockReady, attached = sampleAttached()).isReady())
        assertFalse(coordinator(notReady, notReady, attached = sampleAttached()).isReady())
    }

    // --- helpers ----------------------------------------------------------

    private fun coordinator(
        real: GemmaLocalEngine,
        mock: GemmaLocalEngine,
        attached: AttachedModel?,
    ): EngineCoordinator = EngineCoordinator(
        real = real,
        mock = mock,
        modelSelectionRepo = FakeModelSelectionRepo(attached),
        aiEngineStatusSource = FakeAiEngineStatusSource(),
    )

    private fun sampleEntry(): JournalEntry = JournalEntry(
        id = 1L,
        timestamp = 1_780_000_000_000L,
        dayBucket = "2026-05-19",
        moodTags = listOf(MoodTag.CALM),
        microWins = "успел отдохнуть",
        tomorrowTasks = "позвонить маме",
        reflection = "сегодня хороший день",
        promptShown = "Что вас сегодня порадовало?",
        aiReflection = null,
    )

    private fun sampleAttached(): AttachedModel = AttachedModel(
        variant = ModelVariant.E2B,
        absolutePath = "/data/data/com.pocketreflect.app/files/models/gemma-4-E2B-it.litertlm",
        sizeBytes = 2_588_147_712L,
        sha256Hex = "a".repeat(64),
        attachedAtEpochMs = 1_780_000_000_000L,
    )

    /**
     * In-memory fake [GemmaLocalEngine]. Считает количество вызовов
     * (для проверки «не вызвали ли мы лишний раз»), умеет имитировать
     * исключения для каждого suspend-метода независимо.
     */
    private class FakeEngine(
        private val promptResponse: String = "",
        private val promptThrows: Throwable? = null,
        private val summary: GemmaLocalEngine.WeeklySummary =
            GemmaLocalEngine.WeeklySummary("", null),
        private val summaryThrows: Throwable? = null,
        private val isReady: Boolean = true,
        private val warmUpThrows: Throwable? = null,
    ) : GemmaLocalEngine {
        var promptCalls: Int = 0
            private set
        var summaryCalls: Int = 0
            private set
        var releaseCalled: Boolean = false
            private set
        var warmUpCalls: Int = 0
            private set

        override suspend fun generatePromptResponse(
            entry: JournalEntry,
            personalManifesto: String?,
        ): String {
            promptCalls += 1
            promptThrows?.let { throw it }
            return promptResponse
        }

        override suspend fun summarizeWeek(
            entries: List<JournalEntry>,
            personalManifesto: String?,
        ): GemmaLocalEngine.WeeklySummary {
            summaryCalls += 1
            summaryThrows?.let { throw it }
            return summary
        }

        override suspend fun isReady(): Boolean = isReady

        override suspend fun warmUp() {
            warmUpCalls += 1
            warmUpThrows?.let { throw it }
        }

        override fun streamChat(
            history: List<ChatMessage>,
            persona: ChatPersona,
            journalSnippet: String?,
            manifestoSnippet: String?,
            customPersonaPrompt: String?,
        ): Flow<String> = flowOf(promptResponse)

        override suspend fun summarizeChat(history: List<ChatMessage>): String = promptResponse

        override suspend fun release() {
            releaseCalled = true
        }
    }

    /**
     * Минимальная реализация [ModelSelectionRepository]: только
     * read-side `attached` через [MutableStateFlow]. write-side
     * (`setAttached`/`clearAttached`) намеренно не реализован — координатор
     * их не зовёт, и любая такая попытка будет «громким» FAIL'ом теста.
     *
     * `selectedBackend` отдаёт безопасный дефолт `GPU` — координатор тоже
     * его не читает (это работа `LiteRtGemmaEngine`), но enum-fallback по
     * контракту обязан существовать.
     */
    private class FakeModelSelectionRepo(initial: AttachedModel?) : ModelSelectionRepository {
        override val attached = MutableStateFlow(initial)
        override val selectedBackend = MutableStateFlow(EngineBackend.GPU)

        override suspend fun setAttached(attached: AttachedModel): Unit =
            error("FakeModelSelectionRepo.setAttached не должен вызываться из координатора")

        override suspend fun clearAttached(): Unit =
            error("FakeModelSelectionRepo.clearAttached не должен вызываться из координатора")

        override suspend fun setBackend(backend: EngineBackend): Unit =
            error("FakeModelSelectionRepo.setBackend не должен вызываться из координатора")
    }
}
