// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import com.pocketreflect.app.core.locale.AppLanguage
import com.pocketreflect.app.domain.chat.ChatPersona

/**
 * Юнит-тесты на DataStore-реализацию пользовательских настроек.
 *
 * Используем `PreferenceDataStoreFactory.create(...)` с файлом из `TemporaryFolder`,
 * чтобы каждый тест получил чистый файл и тесты не мешали друг другу.
 * Это работает на pure JVM благодаря `androidx.datastore:datastore-preferences-core`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreUserPreferencesRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var datastoreScope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: DataStoreUserPreferencesRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // Скоуп DataStore — отдельный SupervisorJob, который мы закроем в tearDown,
        // иначе фоновый writer корутины DataStore удерживает тестовый процесс.
        datastoreScope = CoroutineScope(testDispatcher + SupervisorJob())
        dataStore = PreferenceDataStoreFactory.create(
            scope = datastoreScope,
            produceFile = { tempFolder.newFile("user_prefs.preferences_pb") },
        )
        repository = DataStoreUserPreferencesRepository(dataStore)
    }

    @After
    fun tearDown() {
        datastoreScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `defaults are lock disabled and one minute timeout`() = runTest(testDispatcher) {
        assertEquals(false, repository.biometricLockEnabled.first())
        assertEquals(AutoLockTimeout.ONE_MINUTE, repository.autoLockTimeout.first())
        assertEquals(AppLanguage.SYSTEM, repository.appLanguage.first())
    }

    @Test
    fun `setAppLanguage is observed via flow`() = runTest(testDispatcher) {
        repository.setAppLanguage(AppLanguage.EN)
        assertEquals(AppLanguage.EN, repository.appLanguage.first())
        repository.setAppLanguage(AppLanguage.SYSTEM)
        assertEquals(AppLanguage.SYSTEM, repository.appLanguage.first())
    }

    @Test
    fun `setBiometricLockEnabled is observed via flow`() = runTest(testDispatcher) {
        repository.setBiometricLockEnabled(true)
        assertEquals(true, repository.biometricLockEnabled.first())

        repository.setBiometricLockEnabled(false)
        assertEquals(false, repository.biometricLockEnabled.first())
    }

    @Test
    fun `setUiHapticEnabled is observed via flow`() = runTest(testDispatcher) {
        assertEquals(true, repository.uiHapticEnabled.first())
        repository.setUiHapticEnabled(false)
        assertEquals(false, repository.uiHapticEnabled.first())
        repository.setUiHapticEnabled(true)
        assertEquals(true, repository.uiHapticEnabled.first())
    }

    @Test
    fun `setAutoLockTimeout is observed via flow`() = runTest(testDispatcher) {
        repository.setAutoLockTimeout(AutoLockTimeout.THIRTY_SECONDS)
        assertEquals(AutoLockTimeout.THIRTY_SECONDS, repository.autoLockTimeout.first())

        repository.setAutoLockTimeout(AutoLockTimeout.FIVE_MINUTES)
        assertEquals(AutoLockTimeout.FIVE_MINUTES, repository.autoLockTimeout.first())
    }

    @Test
    fun `unknown stored timeout value falls back to default`() = runTest(testDispatcher) {
        // Пишем мимо нашего enum'а через тот же ключ — имитируем сценарий
        // «новая версия приложения, старое значение из будущего».
        val rogueKey = longPreferencesKey("auto_lock_timeout_ms")
        dataStore.edit { it[rogueKey] = 12_345L }

        assertEquals(AutoLockTimeout.DEFAULT, repository.autoLockTimeout.first())
    }

    @Test
    fun `chat defaults and persistence`() = runTest(testDispatcher) {
        assertEquals(false, repository.chatDisclaimerAccepted.first())
        assertEquals(ChatPersona.DEFAULT, repository.chatPersona.first())
        assertEquals(false, repository.chatJournalContextEnabled.first())
        assertEquals(3, repository.chatJournalContextDays.first())

        repository.setChatDisclaimerAccepted(true)
        repository.setChatPersona(ChatPersona.FREE_DIALOG)
        repository.setChatJournalContextEnabled(true)
        repository.setChatJournalContextDays(7)

        assertEquals(true, repository.chatDisclaimerAccepted.first())
        assertEquals(ChatPersona.FREE_DIALOG, repository.chatPersona.first())
        assertEquals(true, repository.chatJournalContextEnabled.first())
        assertEquals(7, repository.chatJournalContextDays.first())

        repository.clearChatPreferences()
        assertEquals(false, repository.chatDisclaimerAccepted.first())
        assertEquals(ChatPersona.DEFAULT, repository.chatPersona.first())
    }
}
