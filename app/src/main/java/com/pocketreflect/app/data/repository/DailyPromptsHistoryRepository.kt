// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.repository

import kotlinx.coroutines.flow.Flow

/**
 * Хранилище «последних показанных промптов дня» для анти-дубль ротации.
 *
 * ## Зачем
 *
 * `DailyPrompts.POOL` содержит 40 формулировок. Если выбирать случайно
 * без памяти, пользователь будет видеть один и тот же промпт каждые
 * несколько дней — что разрушает иллюзию «свежести» и обесценивает сам
 * ритуал. Этот репозиторий хранит последние [HISTORY_LIMIT] показанных
 * промптов в FIFO-очереди; `DailyPrompts.random(history)` исключает их
 * из выбора, гарантируя ~неделю без повторов.
 *
 * ## Семантика
 *
 *  - [recent] — порядок от **старого к новому**. Самый последний показанный — last.
 *  - [push] — добавляет промпт в конец; если он уже есть в списке —
 *    удаляет старое вхождение и кладёт новое в конец (LRU-стиль),
 *    чтобы пользователь не мог «забить» history одинаковыми reshuffle'ами.
 *  - При переполнении список усекается **с головы** до [HISTORY_LIMIT].
 *
 * ## Lifecycle
 *
 * Хранится в том же `pocket_reflect_user_prefs` DataStore, что и остальные
 * preferences — переживает rotate, process death, обновление APK.
 * Очищается при [UserDataRepository.wipeAllUserContent] («Удалить все данные»
 * в Settings → Danger zone).
 */
interface DailyPromptsHistoryRepository {
    val recent: Flow<List<String>>

    /**
     * Регистрирует факт показа [prompt] пользователю.
     *
     * Игнорирует blank-строки (defensive: исключаем мусор в storage,
     * если зовут до того как DailyPrompts.random отработал).
     */
    suspend fun push(prompt: String)

    /** Сбрасывает историю — для Danger zone и для тестов. */
    suspend fun clear()

    companion object {
        /**
         * FIFO-лимит. 7 = неделя ежедневного использования без повторов
         * (главный продуктовый use-case). При POOL=40 это оставляет 33
         * кандидата на каждый день — более чем достаточно для разнообразия.
         */
        const val HISTORY_LIMIT: Int = 7
    }
}
