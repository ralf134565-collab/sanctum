// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.pocketreflect.app.data.model.ModelStorage
import com.pocketreflect.app.data.repository.DataStoreUserPreferencesRepository
import com.pocketreflect.app.data.repository.UserPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

/**
 * DI-модуль пользовательских настроек и связанной им файловой инфраструктуры.
 *
 * Архитектурный инвариант: `DataStore<Preferences>` поставляется как `@Singleton`,
 * иначе будут падать одновременные `edit { … }` (DataStore запрещает создавать
 * более одного экземпляра для одного файла в процессе).
 *
 * [ModelStorage] провайдится здесь же, потому что это `@ApplicationContext`-
 * зависимая фабрика над `filesDir/models/`. Класс намеренно не несёт
 * Hilt-аннотаций — это упрощает pure-JVM юнит-тестирование на `TemporaryFolder`.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    private const val USER_PREFS_FILE = "pocket_reflect_user_prefs"

    /**
     * Имя поддиректории в `filesDir`, куда сохраняются `.litertlm` файлы
     * подключённых моделей. Выделено в константу, чтобы тесты могли использовать
     * то же имя для проверки путей.
     */
    private const val MODELS_DIR_NAME = "models"

    @Provides
    @Singleton
    fun provideUserPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(USER_PREFS_FILE) },
        )

    @Provides
    @Singleton
    fun provideModelStorage(
        @ApplicationContext context: Context,
    ): ModelStorage = ModelStorage(File(context.filesDir, MODELS_DIR_NAME))
}

/**
 * Биндинг абстракции [UserPreferencesRepository] на DataStore-реализацию.
 * Стиль зеркалит [com.pocketreflect.app.di.RepositoryModule].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class UserPreferencesBindingsModule {

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(
        impl: DataStoreUserPreferencesRepository,
    ): UserPreferencesRepository
}
