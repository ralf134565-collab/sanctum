// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.di

import com.pocketreflect.app.data.transfer.BackupRepository
import com.pocketreflect.app.data.transfer.DefaultBackupRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * DI-модуль зашифрованного бэкапа.
 *
 * Json-инстанс — Singleton. Конфигурация:
 *  - `ignoreUnknownKeys = true` — обратно совместимое чтение payload'ов
 *    из будущих версий (мы тихо отбросим неизвестные поля и продолжим);
 *  - `encodeDefaults = true` — гарантия, что поля с дефолтными значениями
 *    физически попадают в файл (важно для криптоустойчивости: чем больше
 *    предсказуемость, тем хуже для атакующего, а consistent ciphertext
 *    структуру не выдаёт благодаря AES-GCM);
 *  - `prettyPrint = false` — экономия размера и никакого визуального hint'а
 *    в plaintext (он всё равно зашифрован, но привычка).
 */
@Module
@InstallIn(SingletonComponent::class)
object BackupModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class BackupBindingsModule {

    @Binds
    @Singleton
    internal abstract fun bindBackupRepository(impl: DefaultBackupRepository): BackupRepository
}
