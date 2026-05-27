// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.pocketreflect.app.data.repository.DailyPromptsHistoryRepository.Companion.HISTORY_LIMIT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Юнит-тесты на DataStore-реализацию history-репозитория «промптов дня».
 *
 * Те же принципы, что и в [DataStoreModelSelectionRepositoryTest]:
 * pure JVM путь через `PreferenceDataStoreFactory.create(...)` поверх
 * временного файла. Без Robolectric и instrumentation.
 *
 * Ключевые инварианты:
 *  - FIFO усечение до [HISTORY_LIMIT].
 *  - LRU-deduplication: повторный push того же текста поднимает его в конец.
 *  - Blank-строки игнорируются.
 *  - clear() возвращает к пустому состоянию.
 *  - Persistence: новый инстанс репозитория видит ту же history (round-trip).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreDailyPromptsHistoryRepositoryTest {

    @get:Rule
    val tempFolder: TemporaryFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var datastoreScope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: DataStoreDailyPromptsHistoryRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        datastoreScope = CoroutineScope(testDispatcher + SupervisorJob())
        dataStore = PreferenceDataStoreFactory.create(
            scope = datastoreScope,
            produceFile = { tempFolder.newFile("prompts_history.preferences_pb") },
        )
        repository = DataStoreDailyPromptsHistoryRepository(dataStore)
    }

    @After
    fun tearDown() {
        datastoreScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `recent is empty by default`() = runTest(testDispatcher) {
        assertEquals(emptyList<String>(), repository.recent.first())
    }

    @Test
    fun `push single prompt then recent emits singleton list`() = runTest(testDispatcher) {
        repository.push("Что сегодня дало энергии?")
        assertEquals(listOf("Что сегодня дало энергии?"), repository.recent.first())
    }

    @Test
    fun `pushing 8 prompts retains last 7 (FIFO)`() = runTest(testDispatcher) {
        // HISTORY_LIMIT = 7. Восьмой push должен вытеснить первый.
        val prompts = (1..8).map { "Промпт #$it" }
        prompts.forEach { repository.push(it) }

        val recent = repository.recent.first()
        assertEquals(HISTORY_LIMIT, recent.size)
        assertEquals("Первый промпт должен быть вытеснен FIFO", "Промпт #2", recent.first())
        assertEquals("Последний push — в хвосте списка", "Промпт #8", recent.last())
    }

    @Test
    fun `pushing duplicate moves it to the tail (LRU semantics)`() = runTest(testDispatcher) {
        repository.push("A")
        repository.push("B")
        repository.push("C")
        repository.push("A") // дубль: A должен переехать в хвост, B/C остаться

        val recent = repository.recent.first()
        assertEquals(listOf("B", "C", "A"), recent)
    }

    @Test
    fun `blank prompts are silently ignored`() = runTest(testDispatcher) {
        repository.push("")
        repository.push("   ")
        repository.push("\n\t")
        assertEquals(
            "Бланки не должны попадать в storage — это защита от вызовов до readiness",
            emptyList<String>(),
            repository.recent.first(),
        )
    }

    @Test
    fun `clear empties the history`() = runTest(testDispatcher) {
        repository.push("A")
        repository.push("B")
        assertTrue(repository.recent.first().isNotEmpty())

        repository.clear()

        assertEquals(emptyList<String>(), repository.recent.first())
    }

    @Test
    fun `history is trimmed whitespace on push`() = runTest(testDispatcher) {
        repository.push("  Какой момент дня вы захотите вспомнить через год?  ")
        assertEquals(
            listOf("Какой момент дня вы захотите вспомнить через год?"),
            repository.recent.first(),
        )
    }

    @Test
    fun `new repository instance sees previously persisted history`() = runTest(testDispatcher) {
        // Round-trip: один экземпляр пишет, другой (поверх того же DataStore) читает.
        // Гарантирует, что мы не зависим от in-memory cache внутри Repository.
        repository.push("A")
        repository.push("B")

        val secondInstance = DataStoreDailyPromptsHistoryRepository(dataStore)
        assertEquals(listOf("A", "B"), secondInstance.recent.first())
    }
}
