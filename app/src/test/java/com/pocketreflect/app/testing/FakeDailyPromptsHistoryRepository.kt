// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.testing

import com.pocketreflect.app.data.repository.DailyPromptsHistoryRepository
import com.pocketreflect.app.data.repository.DailyPromptsHistoryRepository.Companion.HISTORY_LIMIT
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory реализация [DailyPromptsHistoryRepository] для plain JUnit-тестов.
 *
 * Поведение должно зеркалить production-имплементацию:
 *  - LRU-deduplication: повторный push того же промпта поднимает его в конец.
 *  - FIFO усечение до [HISTORY_LIMIT].
 *  - Blank-строки игнорируются.
 *
 * `pushInvocations` / `clearInvocations` — для assert'ов в тестах.
 */
class FakeDailyPromptsHistoryRepository(
    initial: List<String> = emptyList(),
) : DailyPromptsHistoryRepository {

    private val state = MutableStateFlow(initial.takeLast(HISTORY_LIMIT))

    override val recent: Flow<List<String>> = state.asStateFlow()

    var pushInvocations: Int = 0
        private set
    var clearInvocations: Int = 0
        private set

    /** Снимок текущей истории — удобно в тестах без collect'a. */
    val snapshot: List<String>
        get() = state.value

    override suspend fun push(prompt: String) {
        val cleaned = prompt.trim()
        if (cleaned.isBlank()) return
        pushInvocations++
        val current = state.value
        val deduped = current.filterNot { it == cleaned }
        state.value = (deduped + cleaned).takeLast(HISTORY_LIMIT)
    }

    override suspend fun clear() {
        clearInvocations++
        state.value = emptyList()
    }
}
