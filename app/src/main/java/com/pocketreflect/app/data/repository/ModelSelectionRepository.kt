// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.repository

import com.pocketreflect.app.data.ai.EngineBackend
import com.pocketreflect.app.data.model.ModelVariant
import kotlinx.coroutines.flow.Flow

/**
 * Абстракция над хранилищем «подключённой модели».
 *
 * Хранит **факт** подключения и метаданные, но не саму модель — байты лежат
 * в `filesDir/models/` под управлением [com.pocketreflect.app.data.model.ModelStorage].
 *
 * Поток [attached] переживает rotate и process death, поэтому `SettingsViewModel`
 * и `ModelSettingsViewModel` подписываются на один и тот же source of truth.
 *
 * Так же здесь живёт пользовательский выбор [selectedBackend] (GPU/CPU). Это
 * не часть «подключения файла», но привязка одного и того же DataStore-файла
 * даёт нам атомарность и единую точку очистки в `clearAttached` (важно: backend
 * мы оставляем неизменным при отключении модели — это пользовательская
 * настройка, она переживает смену файла).
 *
 * В тестах подменяется на in-memory реализацию (см.
 * `DataStoreModelSelectionRepositoryTest` для round-trip против реального DataStore).
 */
interface ModelSelectionRepository {
    val attached: Flow<AttachedModel?>

    /**
     * Выбранный пользователем аппаратный бэкенд LiteRT-LM.
     *
     * Дефолт — [EngineBackend.GPU]: на топ-устройствах он 2–3× быстрее CPU,
     * а `LiteRtGemmaEngine` уже умеет молча откатываться на CPU, если OpenCL
     * не инициализируется. Это даёт «zero-config» опыт первому пользователю.
     */
    val selectedBackend: Flow<EngineBackend>

    suspend fun setAttached(attached: AttachedModel)
    suspend fun clearAttached()
    suspend fun setBackend(backend: EngineBackend)
}

/**
 * Метаданные подключённой модели.
 *
 * @property absolutePath абсолютный путь к файлу в `filesDir/models/`.
 *   Хранится в DataStore только для удобства чтения логов и быстрого
 *   `File(absolutePath).exists()`-проверки. Production-код предпочтительно
 *   ходит через `ModelStorage.resolvedFile(variant)`.
 * @property sha256Hex hex SHA-256 файла на момент подключения. Не используется
 *   для повторной верификации (это сделано один раз во время copy), но удобен
 *   для отладки и для будущей фичи «обнаружить, что файл изменили вне приложения».
 */
data class AttachedModel(
    val variant: ModelVariant,
    val absolutePath: String,
    val sizeBytes: Long,
    val sha256Hex: String,
    val attachedAtEpochMs: Long,
)
