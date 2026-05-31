// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.journal

import androidx.test.core.app.ApplicationProvider
import com.pocketreflect.app.core.locale.AppLanguageResolver
import com.pocketreflect.app.core.work.WeeklySummaryPolicy
import com.pocketreflect.app.data.local.entity.AITrendProfile
import com.pocketreflect.app.data.local.entity.JournalEntry
import com.pocketreflect.app.domain.model.MoodTag
import com.pocketreflect.app.testing.FakeDatabaseAccess
import com.pocketreflect.app.testing.FakeUserPreferencesRepository
import com.pocketreflect.app.testing.FakeAiEngineStatusSource
import com.pocketreflect.app.testing.FakeClock
import com.pocketreflect.app.testing.FakeDailyPromptsHistoryRepository
import com.pocketreflect.app.testing.FakeGemmaLocalEngine
import com.pocketreflect.app.testing.FakeJournalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Тесты «empathic UX» инвариантов JournalViewModel.
 * Все тесты используют [StandardTestDispatcher] — он не выполняет корутины
 * автоматически, мы прокручиваем их через `advanceUntilIdle()`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class JournalViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeJournalRepository
    private lateinit var engine: FakeGemmaLocalEngine
    private lateinit var clock: FakeClock
    private lateinit var promptsHistory: FakeDailyPromptsHistoryRepository
    private lateinit var aiEngineStatus: FakeAiEngineStatusSource
    private lateinit var userPreferences: FakeUserPreferencesRepository
    private lateinit var appLanguageResolver: AppLanguageResolver
    private lateinit var databaseAccess: FakeDatabaseAccess

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeJournalRepository()
        engine = FakeGemmaLocalEngine()
        clock = FakeClock(fixedNowMillis = 1_715_000_000_000L, fixedToday = "2026-05-19")
        promptsHistory = FakeDailyPromptsHistoryRepository()
        aiEngineStatus = FakeAiEngineStatusSource()
        userPreferences = FakeUserPreferencesRepository()
        appLanguageResolver = AppLanguageResolver(userPreferences)
        databaseAccess = FakeDatabaseAccess()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel() = JournalViewModel(
        appContext = ApplicationProvider.getApplicationContext(),
        journalRepository = repository,
        gemmaEngine = engine,
        clock = clock,
        promptsHistory = promptsHistory,
        aiEngineStatusSource = aiEngineStatus,
        appLanguageResolver = appLanguageResolver,
        userPreferencesRepository = userPreferences,
        databaseAccess = databaseAccess,
    )

    @Test
    fun `toggling tag does not invoke gemma engine`() = runTest(testDispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(0, engine.generateInvocations)

        vm.onIntent(JournalContract.Intent.ToggleTag(MoodTag.CALM))
        advanceUntilIdle()

        assertEquals("Инференс только по явной кнопке", 0, engine.generateInvocations)
        assertNull(vm.state.value.aiResponse)
    }

    @Test
    fun `request ai reflection invokes engine when tags selected`() = runTest(testDispatcher) {
        engine.responseProvider = { "mentor-reply" }
        val vm = viewModel()
        advanceUntilIdle()

        vm.onIntent(JournalContract.Intent.ToggleTag(MoodTag.CALM))
        vm.onIntent(JournalContract.Intent.RequestAiReflection)
        advanceUntilIdle()

        assertEquals(1, engine.generateInvocations)
        assertEquals("mentor-reply", vm.state.value.aiResponse)
        assertFalse(vm.state.value.isAiThinking)
    }

    @Test
    fun `toggling tag clears stale ai response`() = runTest(testDispatcher) {
        engine.responseProvider = { "first" }
        val vm = viewModel()
        advanceUntilIdle()

        vm.onIntent(JournalContract.Intent.ToggleTag(MoodTag.CALM))
        vm.onIntent(JournalContract.Intent.RequestAiReflection)
        advanceUntilIdle()
        assertEquals("first", vm.state.value.aiResponse)

        vm.onIntent(JournalContract.Intent.ToggleTag(MoodTag.ANXIETY))
        advanceUntilIdle()

        assertNull(vm.state.value.aiResponse)
        assertEquals(1, engine.generateInvocations)
    }

    @Test
    fun `toggling negative tag hides micro-wins and clears existing text`() = runTest(testDispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        // Сначала пишем «победы», потом ставим тег «Тревога».
        vm.onIntent(JournalContract.Intent.UpdateMicroWins("дошёл до 22:00 без новостей"))
        vm.onIntent(JournalContract.Intent.ToggleTag(MoodTag.ANXIETY))
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue("Микро-победы должны быть скрыты при негативном теге", state.isMicroWinsHidden)
        assertEquals("Текст микро-побед должен быть очищен", "", state.microWins)
        assertTrue("Должен включиться supportive mode", state.isSupportiveModeActive)
    }

    @Test
    fun `tomorrow tasks are limited to MAX lines on input`() = runTest(testDispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        val tooMany = (1..7).joinToString("\n") { "задача $it" }
        vm.onIntent(JournalContract.Intent.UpdateTomorrowTasks(tooMany))

        val saved = vm.state.value.tomorrowTasks
        val lineCount = saved.lineSequence().count()
        assertEquals(
            "Должно быть ровно MAX_TOMORROW_TASK_LINES строк",
            JournalContract.MAX_TOMORROW_TASK_LINES,
            lineCount,
        )
    }

    @Test
    fun `cannot save without any tag selected`() = runTest(testDispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        assertFalse("Без тегов canSave должен быть false", vm.state.value.canSave)
        assertTrue("Должен быть hint", vm.state.value.showSaveHintNoTags)

        vm.onIntent(JournalContract.Intent.SaveDay)
        advanceUntilIdle()
        assertEquals("repository.saveEntry не должен был вызваться", 0, repository.saveInvocations)
    }

    @Test
    fun `saveDay persists aiReflection after mentor request`() = runTest(testDispatcher) {
        engine.responseProvider = { "saved-mentor-text" }
        val vm = viewModel()
        advanceUntilIdle()

        vm.onIntent(JournalContract.Intent.ToggleTag(MoodTag.CALM))
        vm.onIntent(JournalContract.Intent.RequestAiReflection)
        advanceUntilIdle()

        vm.onIntent(JournalContract.Intent.SaveDay)
        advanceUntilIdle()

        assertEquals("saved-mentor-text", repository.lastSaved?.aiReflection)
    }

    @Test
    fun `saveDay persists entry and flips wasSavedForDay flag`() = runTest(testDispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onIntent(JournalContract.Intent.ToggleTag(MoodTag.CALM))
        vm.onIntent(JournalContract.Intent.UpdateMicroWins("вышел погулять"))
        vm.onIntent(JournalContract.Intent.SaveDay)
        advanceUntilIdle()

        assertEquals(1, repository.saveInvocations)
        val saved = repository.lastSaved!!
        assertEquals(clock.fixedToday, saved.dayBucket)
        assertEquals(clock.fixedNowMillis, saved.timestamp)
        assertTrue(saved.moodTags.contains(MoodTag.CALM))

        val state = vm.state.value
        assertTrue(state.wasSavedForDay)
        assertFalse(state.isSaving)
    }

    @Test
    fun `bootstrap with existing entry sets wasSavedForDay to true`() = runTest(testDispatcher) {
        repository.todayOverride = com.pocketreflect.app.data.local.entity.JournalEntry(
            id = 7L,
            timestamp = clock.nowMillis(),
            dayBucket = clock.today(),
            moodTags = listOf(MoodTag.GRATITUDE),
            microWins = "найден лучший кофе на районе",
            tomorrowTasks = "",
            reflection = "",
            promptShown = "Что вы сегодня заметили впервые?",
            aiReflection = "fake-cached",
        )

        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state.wasSavedForDay)
        assertEquals(setOf(MoodTag.GRATITUDE), state.selectedTags)
        assertEquals("найден лучший кофе на районе", state.microWins)
        assertEquals("fake-cached", state.aiResponse)
    }

    @Test
    fun `bootstrap pushes selected daily prompt into history when no entry exists`() = runTest(testDispatcher) {
        // today=null → должны выбрать новый промпт и зафиксировать сам факт показа.
        val vm = viewModel()
        advanceUntilIdle()

        val shown = vm.state.value.dailyPrompt
        assertTrue("Промпт должен быть выбран при bootstrap", shown.isNotBlank())
        assertEquals(
            "history должна содержать ровно один push — выбранный промпт",
            listOf(shown),
            promptsHistory.snapshot,
        )
    }

    @Test
    fun `bootstrap with existing entry does NOT push promptShown into history`() = runTest(testDispatcher) {
        // today!=null → промпт уже был показан в прошлой сессии и записан в БД.
        // Повторно пушить его в history не нужно — иначе FIFO забьётся
        // одной и той же записью при каждом перезапуске.
        repository.todayOverride = com.pocketreflect.app.data.local.entity.JournalEntry(
            id = 1L,
            timestamp = clock.nowMillis(),
            dayBucket = clock.today(),
            moodTags = listOf(MoodTag.CALM),
            microWins = "",
            tomorrowTasks = "",
            reflection = "",
            promptShown = "Какой момент дня вы захотите вспомнить через год?",
            aiReflection = null,
        )

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(
            "Промпт из существующей записи в history НЕ пушится",
            0,
            promptsHistory.pushInvocations,
        )
        assertEquals(
            "На экране должен быть промпт из БД, не новый",
            "Какой момент дня вы захотите вспомнить через год?",
            vm.state.value.dailyPrompt,
        )
    }

    @Test
    fun `reshuffle never returns a prompt that is in history`() = runTest(testDispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        // Симулируем 7 reshuffle подряд — каждый раз новый промпт.
        val seen = mutableListOf(vm.state.value.dailyPrompt)
        repeat(7) {
            vm.onIntent(JournalContract.Intent.ReshufflePrompt)
            advanceUntilIdle()
            seen += vm.state.value.dailyPrompt
        }
        // Все промпты в seen должны быть разными — анти-дубль работает.
        assertEquals(seen.toSet().size, seen.size)
    }

    @Test
    fun `request weekly trend builds and displays summary`() = runTest(testDispatcher) {
        repository.seedEntries(
            listOf(
                sampleEntry("2026-05-17"),
                sampleEntry("2026-05-18"),
                sampleEntry("2026-05-19"),
            ),
        )
        val vm = viewModel()
        advanceUntilIdle()
        assertTrue(vm.state.value.canRequestWeeklyTrend)

        vm.onIntent(JournalContract.Intent.RequestWeeklyTrend)
        advanceUntilIdle()

        assertEquals(1, engine.summarizeWeekInvocations)
        assertEquals("fake-week", vm.state.value.weeklyTrendSummary)
        assertFalse(vm.state.value.isWeeklyTrendBuilding)
    }

    @Test
    fun `weekly trend ignores stale blank profile when newer summary exists`() = runTest(testDispatcher) {
        repository.saveTrendProfile(
            AITrendProfile(
                periodStart = 1L,
                periodEnd = 9_999L,
                generatedAt = clock.nowMillis() - 1_000L,
                entryCount = 3,
                summary = "",
                structuredJson = null,
            ),
        )
        repository.seedEntries(
            listOf(
                sampleEntry("2026-05-17"),
                sampleEntry("2026-05-18"),
                sampleEntry("2026-05-19"),
            ),
        )
        val vm = viewModel()
        advanceUntilIdle()

        vm.onIntent(JournalContract.Intent.RequestWeeklyTrend)
        advanceUntilIdle()

        assertEquals("fake-week", vm.state.value.weeklyTrendSummary)
    }

    @Test
    fun `uiHapticEnabled defaults and observation`() = runTest(testDispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        assertTrue(vm.state.value.uiHapticEnabled)

        userPreferences.setUiHapticEnabled(false)
        advanceUntilIdle()
        assertFalse(vm.state.value.uiHapticEnabled)
    }

    private fun sampleEntry(dayBucket: String) = JournalEntry(
        timestamp = clock.nowMillis(),
        dayBucket = dayBucket,
        moodTags = listOf(MoodTag.CALM),
        microWins = "",
        tomorrowTasks = "",
        reflection = "",
        promptShown = "",
        aiReflection = null,
    )
}
