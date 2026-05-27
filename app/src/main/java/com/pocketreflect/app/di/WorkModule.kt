// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DI-модуль для WorkManager-инфраструктуры (Sub-PR #4).
 *
 * `WorkManager.getInstance(context)` — это singleton на процесс, но
 * чтобы код в `WarmupCoordinator` оставался DI-friendly (тестируемым через
 * подмену в Hilt-test модуле), оборачиваем его в `@Provides`. Сами Worker'ы
 * получают зависимости через [androidx.hilt.work.HiltWorkerFactory] —
 * фабрика подключается в `PocketReflectApp.workManagerConfiguration` ещё с
 * Sub-PR foundation-polish.
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkModule {

    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context,
    ): WorkManager = WorkManager.getInstance(context)
}
