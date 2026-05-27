// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.repository

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Единая точка для необратимого сброса пользовательского контента.
 *
 * «Стереть всю историю» в настройках должно вызывать только [wipeAllUserContent],
 * а не цепочку репозиториев из ViewModel.
 *
 * ## Что стирается
 *  - Room: записи дневника, AI-профили недель, сообщения чата (одна транзакция).
 *  - DataStore: настройки чата (включая disclaimer — показывается снова) и
 *    история показанных промптов дня.
 *
 * ## Что не трогаем
 *  - Биометрический lock и auto-lock ([UserPreferencesRepository]).
 *  - Подключённая модель Gemma ([ModelSelectionRepository] и файл на диске).
 */
interface UserDataRepository {
    suspend fun wipeAllUserContent()
}

@Singleton
class DefaultUserDataRepository @Inject constructor(
    private val journalRepository: JournalRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val promptsHistory: DailyPromptsHistoryRepository,
) : UserDataRepository {

    override suspend fun wipeAllUserContent() {
        // Сначала Room — главный объём личных данных. DataStore нельзя
        // включить в SQL-транзакцию; при сбое на prefs журнал уже пуст.
        journalRepository.wipeAll()
        userPreferencesRepository.clearChatPreferences()
        promptsHistory.clear()
    }
}
