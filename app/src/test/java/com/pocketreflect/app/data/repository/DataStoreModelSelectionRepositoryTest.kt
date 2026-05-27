// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pocketreflect.app.data.ai.EngineBackend
import com.pocketreflect.app.data.model.ModelVariant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Юнит-тесты на DataStore-реализацию подключения локальной модели.
 *
 * Используем `PreferenceDataStoreFactory.create(...)` с файлом из `TemporaryFolder`
 * — pure JVM путь, без Robolectric и без instrumentation. Реальный продакшен
 * DataStore поверх `pocket_reflect_user_prefs` ведёт себя идентично.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreModelSelectionRepositoryTest {

    @get:Rule
    val tempFolder: TemporaryFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var datastoreScope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: DataStoreModelSelectionRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        datastoreScope = CoroutineScope(testDispatcher + SupervisorJob())
        dataStore = PreferenceDataStoreFactory.create(
            scope = datastoreScope,
            produceFile = { tempFolder.newFile("model_prefs.preferences_pb") },
        )
        repository = DataStoreModelSelectionRepository(dataStore)
    }

    @After
    fun tearDown() {
        datastoreScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `attached is null by default`() = runTest(testDispatcher) {
        assertNull(repository.attached.first())
    }

    @Test
    fun `setAttached then attached emits same payload`() = runTest(testDispatcher) {
        val attached = AttachedModel(
            variant = ModelVariant.E2B,
            absolutePath = "/data/data/com.pocketreflect.app/files/models/gemma-4-E2B-it.litertlm",
            sizeBytes = 2_588_147_712L,
            sha256Hex = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c",
            attachedAtEpochMs = 1_780_000_000_000L,
        )

        repository.setAttached(attached)

        assertEquals(attached, repository.attached.first())
    }

    @Test
    fun `clearAttached removes the record entirely`() = runTest(testDispatcher) {
        repository.setAttached(
            AttachedModel(
                variant = ModelVariant.E4B,
                absolutePath = "/x",
                sizeBytes = 1L,
                sha256Hex = "a".repeat(64),
                attachedAtEpochMs = 1L,
            )
        )
        assertNotNull(repository.attached.first())

        repository.clearAttached()

        assertNull(repository.attached.first())
    }

    @Test
    fun `partial record yields null instead of crashing`() = runTest(testDispatcher) {
        // Симулируем «частично записанное» состояние — например, апгрейд приложения,
        // когда схема ключей расходится с предыдущей версией. Чтение не должно
        // падать; ожидаем `null`, чтобы UI трактовал ситуацию как «не подключено».
        val rogueKey = stringPreferencesKey("model_variant")
        dataStore.edit { it[rogueKey] = "E2B" }

        assertNull(repository.attached.first())
    }

    @Test
    fun `unknown variant name in storage maps to null`() = runTest(testDispatcher) {
        // Если в DataStore лежит variant с именем, отсутствующим в текущем enum
        // (например, после downgrade приложения), читаем `null` — это
        // «не подключено», а не краш.
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("model_variant")] = "UNKNOWN_VARIANT"
            prefs[stringPreferencesKey("model_file_path")] = "/x"
            prefs[stringPreferencesKey("model_sha256_hex")] = "a".repeat(64)
        }

        assertNull(repository.attached.first())
    }

    // --- Sub-PR #3c: backend persistence -----------------------------------

    @Test
    fun `selectedBackend defaults to GPU when DataStore is empty`() = runTest(testDispatcher) {
        assertEquals(EngineBackend.GPU, repository.selectedBackend.first())
    }

    @Test
    fun `setBackend CPU then selectedBackend emits CPU`() = runTest(testDispatcher) {
        repository.setBackend(EngineBackend.CPU)
        assertEquals(EngineBackend.CPU, repository.selectedBackend.first())
    }

    @Test
    fun `setBackend round-trip CPU then GPU returns latest write`() = runTest(testDispatcher) {
        repository.setBackend(EngineBackend.CPU)
        repository.setBackend(EngineBackend.GPU)
        assertEquals(EngineBackend.GPU, repository.selectedBackend.first())
    }

    @Test
    fun `unknown backend name in storage falls back to GPU`() = runTest(testDispatcher) {
        // Симулируем downgrade приложения / ручную правку: в DataStore лежит
        // имя enum'а, которого больше нет в коде. Читаем дефолт `GPU`, а не
        // крашимся и не возвращаем null — пользователь и так не понимает,
        // зачем ему этот тогл, давать ему чёрный экран — недопустимо.
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("engine_backend")] = "NPU_FROM_THE_FUTURE"
        }

        assertEquals(EngineBackend.GPU, repository.selectedBackend.first())
    }

    @Test
    fun `clearAttached does not reset backend preference`() = runTest(testDispatcher) {
        // backend — это user-preference, она должна переживать смену файла
        // модели и отключение. Иначе при каждом «Удалить» пользователь терял
        // бы свой осознанный выбор CPU.
        repository.setBackend(EngineBackend.CPU)
        repository.setAttached(
            AttachedModel(
                variant = ModelVariant.E2B,
                absolutePath = "/x",
                sizeBytes = 1L,
                sha256Hex = "a".repeat(64),
                attachedAtEpochMs = 1L,
            )
        )

        repository.clearAttached()

        assertNull(repository.attached.first())
        assertEquals(EngineBackend.CPU, repository.selectedBackend.first())
    }
}
