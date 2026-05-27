// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.di

import com.pocketreflect.app.data.repository.ChatRepository
import com.pocketreflect.app.data.repository.DailyPromptsHistoryRepository
import com.pocketreflect.app.data.repository.RoomChatRepository
import com.pocketreflect.app.data.repository.DataStoreDailyPromptsHistoryRepository
import com.pocketreflect.app.data.repository.DataStoreModelSelectionRepository
import com.pocketreflect.app.data.repository.JournalRepository
import com.pocketreflect.app.data.repository.DefaultUserDataRepository
import com.pocketreflect.app.data.repository.ModelSelectionRepository
import com.pocketreflect.app.data.repository.RoomJournalRepository
import com.pocketreflect.app.data.repository.UserDataRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Биндинг доменных репозиториев на их инфраструктурные реализации.
 *
 *  - [JournalRepository] → Room.
 *  - [ModelSelectionRepository] → DataStore Preferences (поверх того же
 *    `pocket_reflect_user_prefs` файла, что и пользовательские настройки;
 *    DataStore запрещает несколько экземпляров на один файл).
 *  - [DailyPromptsHistoryRepository] → тот же DataStore-файл, отдельный ключ.
 *
 * Тесты подменяют биндинги через `@TestInstallIn`, либо инжектят Fake'и
 * напрямую в plain JUnit-тесты.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindJournalRepository(impl: RoomJournalRepository): JournalRepository

    @Binds
    @Singleton
    abstract fun bindModelSelectionRepository(
        impl: DataStoreModelSelectionRepository,
    ): ModelSelectionRepository

    @Binds
    @Singleton
    abstract fun bindDailyPromptsHistoryRepository(
        impl: DataStoreDailyPromptsHistoryRepository,
    ): DailyPromptsHistoryRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: RoomChatRepository): ChatRepository

    @Binds
    @Singleton
    abstract fun bindUserDataRepository(impl: DefaultUserDataRepository): UserDataRepository
}
