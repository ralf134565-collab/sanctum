// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pocketreflect.app.data.ai.EngineBackend
import com.pocketreflect.app.data.model.ModelVariant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-реализация [ModelSelectionRepository].
 *
 * Использует тот же `DataStore<Preferences>` (файл `pocket_reflect_user_prefs`),
 * что и [DataStoreUserPreferencesRepository] — это требование архитектурного
 * инварианта DataStore: один файл — один экземпляр на процесс. Префикс
 * ключей `model_*` визуально отделяет их от остальных настроек.
 *
 * Запись атомарна на уровне `edit { … }`; чтение тоже атомарно благодаря
 * `dataStore.data.map { … }`. Если в момент апгрейда схема ключей расходится
 * (например, частично записанное состояние из старой версии) — [readAttached]
 * безопасно возвращает `null` вместо краша.
 *
 * Backend ([selectedBackend]) хранится в отдельном ключе и **не** очищается
 * при `clearAttached` — это user-preference, переживает смену файла модели.
 */
@Singleton
class DataStoreModelSelectionRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ModelSelectionRepository {

    override val attached: Flow<AttachedModel?> =
        dataStore.data.map { prefs -> readAttached(prefs) }

    override val selectedBackend: Flow<EngineBackend> =
        dataStore.data.map { prefs -> readBackend(prefs) }

    override suspend fun setAttached(attached: AttachedModel) {
        dataStore.edit { prefs ->
            prefs[KEY_VARIANT] = attached.variant.name
            prefs[KEY_PATH] = attached.absolutePath
            prefs[KEY_SIZE] = attached.sizeBytes
            prefs[KEY_SHA256] = attached.sha256Hex
            prefs[KEY_ATTACHED_AT] = attached.attachedAtEpochMs
        }
    }

    override suspend fun clearAttached() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_VARIANT)
            prefs.remove(KEY_PATH)
            prefs.remove(KEY_SIZE)
            prefs.remove(KEY_SHA256)
            prefs.remove(KEY_ATTACHED_AT)
        }
    }

    override suspend fun setBackend(backend: EngineBackend) {
        dataStore.edit { prefs ->
            prefs[KEY_ENGINE_BACKEND] = backend.name
        }
    }

    private fun readAttached(prefs: Preferences): AttachedModel? {
        val variantName = prefs[KEY_VARIANT] ?: return null
        val variant = ModelVariant.entries.firstOrNull { it.name == variantName } ?: return null
        val path = prefs[KEY_PATH] ?: return null
        val size = prefs[KEY_SIZE] ?: return null
        val sha = prefs[KEY_SHA256] ?: return null
        val attachedAt = prefs[KEY_ATTACHED_AT] ?: return null
        return AttachedModel(
            variant = variant,
            absolutePath = path,
            sizeBytes = size,
            sha256Hex = sha,
            attachedAtEpochMs = attachedAt,
        )
    }

    /**
     * Дефолт — [EngineBackend.GPU]. Неизвестное значение в storage (downgrade
     * приложения, ручная правка) → silently откатываемся к дефолту, чтобы
     * не зависеть от ошибки конфигурации.
     */
    private fun readBackend(prefs: Preferences): EngineBackend {
        val name = prefs[KEY_ENGINE_BACKEND] ?: return EngineBackend.GPU
        return EngineBackend.entries.firstOrNull { it.name == name } ?: EngineBackend.GPU
    }

    private companion object {
        val KEY_VARIANT = stringPreferencesKey("model_variant")
        val KEY_PATH = stringPreferencesKey("model_file_path")
        val KEY_SIZE = longPreferencesKey("model_size_bytes")
        val KEY_SHA256 = stringPreferencesKey("model_sha256_hex")
        val KEY_ATTACHED_AT = longPreferencesKey("model_attached_at_ms")
        val KEY_ENGINE_BACKEND = stringPreferencesKey("engine_backend")
    }
}
