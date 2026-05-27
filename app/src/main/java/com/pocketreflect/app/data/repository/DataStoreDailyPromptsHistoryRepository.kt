// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import com.pocketreflect.app.data.repository.DailyPromptsHistoryRepository.Companion.HISTORY_LIMIT
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-Preferences реализация [DailyPromptsHistoryRepository].
 *
 * ## Формат хранения
 *
 * Сериализация — один `stringPreferencesKey` с разделителем `\u001F`
 * (ASCII Unit Separator). Причины выбора:
 *
 *  - `stringSetPreferencesKey` **не сохраняет порядок** — критично для FIFO.
 *  - JSON-encoding (Gson/Kotlinx Serialization) — overkill для плоского
 *    списка строк и тянет лишнюю зависимость в data-слой.
 *  - Unit Separator (`\u001F`) гарантированно не встречается в естественных
 *    русских/латинских текстах промптов (это control-символ из ASCII C0),
 *    в отличие от запятой/перевода строки которые валидны в самих промптах.
 *
 * ## Конкурентность
 *
 * `DataStore.edit { ... }` обеспечивает атомарность каждого `push`/`clear`.
 * Гонок между параллельными `push` (теоретически: пользователь два раза
 * быстро тапнул «обновить промпт») не будет — edit-блок сериализует записи.
 */
@Singleton
class DataStoreDailyPromptsHistoryRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : DailyPromptsHistoryRepository {

    override val recent: Flow<List<String>> =
        dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { prefs -> decode(prefs[KEY_HISTORY]) }

    override suspend fun push(prompt: String) {
        val cleaned = prompt.trim()
        if (cleaned.isBlank()) return
        dataStore.edit { prefs ->
            val current = decode(prefs[KEY_HISTORY])
            val deduped = current.filterNot { it == cleaned }
            val updated = (deduped + cleaned).takeLast(HISTORY_LIMIT)
            prefs[KEY_HISTORY] = encode(updated)
        }
    }

    override suspend fun clear() {
        dataStore.edit { prefs -> prefs.remove(KEY_HISTORY) }
    }

    private fun decode(raw: String?): List<String> {
        if (raw.isNullOrEmpty()) return emptyList()
        return raw.split(SEPARATOR).filter { it.isNotBlank() }
    }

    private fun encode(list: List<String>): String =
        list.joinToString(separator = SEPARATOR)

    private companion object {
        val KEY_HISTORY = stringPreferencesKey("daily_prompts_history_v1")

        /** ASCII Unit Separator — control-символ, не встречается в текстах. */
        const val SEPARATOR: String = "\u001F"
    }
}
