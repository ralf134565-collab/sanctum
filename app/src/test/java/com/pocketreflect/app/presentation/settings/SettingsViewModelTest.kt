// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pocketreflect.app.core.security.AuthSessionHolder
import com.pocketreflect.app.core.security.BiometricAvailability
import com.pocketreflect.app.domain.breathing.BreathingPattern
import com.pocketreflect.app.testing.FakeClock
import com.pocketreflect.app.testing.RobolectricRoomTestSupport
import com.pocketreflect.app.testing.StaticDatabaseProvider
import com.pocketreflect.app.testing.FakeModelSelectionRepository
import com.pocketreflect.app.testing.FakeUserDataRepository
import com.pocketreflect.app.testing.FakeUserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var userData: FakeUserDataRepository
    private lateinit var prefs: FakeUserPreferencesRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        RobolectricRoomTestSupport.prepareWorkManager(
            ApplicationProvider.getApplicationContext(),
        )
        userData = FakeUserDataRepository()
        prefs = FakeUserPreferencesRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        RobolectricRoomTestSupport.shutdownWorkManager()
    }

    private fun viewModel(): SettingsViewModel {
        val context: Context = ApplicationProvider.getApplicationContext()
        val room = RobolectricRoomTestSupport.inMemoryDatabase(context)
        val authSessionHolder = AuthSessionHolder(FakeClock(), StaticDatabaseProvider(room.database)).apply {
            markAuthenticated()
        }
        return SettingsViewModel(
            appContext = context,
            userPreferencesRepository = prefs,
            userDataRepository = userData,
            modelSelectionRepository = FakeModelSelectionRepository(),
            biometricAvailability = BiometricAvailability(context),
            authSessionHolder = authSessionHolder,
        )
    }

    @Test
    fun toggleScreenshotProtection_persistsPreference() = runTest(testDispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        assertTrue(vm.state.value.screenshotProtectionEnabled)

        vm.onIntent(SettingsContract.Intent.ToggleScreenshotProtection(false))
        advanceUntilIdle()

        assertFalse(vm.state.value.screenshotProtectionEnabled)
        assertFalse(prefs.screenshotProtectionEnabled.first())
    }

    @Test
    fun toggleUiHaptic_persistsPreference() = runTest(testDispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        assertTrue(vm.state.value.uiHapticEnabled)

        vm.onIntent(SettingsContract.Intent.ToggleUiHaptic(false))
        advanceUntilIdle()

        assertFalse(vm.state.value.uiHapticEnabled)
        assertFalse(prefs.uiHapticEnabled.first())
    }

    @Test
    fun setBreathingPattern_persistsPreference() = runTest(testDispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(BreathingPattern.RESONANT, vm.state.value.breathingPattern)

        vm.onIntent(SettingsContract.Intent.SetBreathingPattern(BreathingPattern.BOX))
        advanceUntilIdle()

        assertEquals(BreathingPattern.BOX, vm.state.value.breathingPattern)
        assertEquals(BreathingPattern.BOX, prefs.breathingPattern.first())
    }

    @Test
    fun toggleBreathingHaptic_persistsPreference() = runTest(testDispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        assertTrue(vm.state.value.breathingHapticEnabled)

        vm.onIntent(SettingsContract.Intent.ToggleBreathingHaptic(false))
        advanceUntilIdle()

        assertFalse(vm.state.value.breathingHapticEnabled)
        assertFalse(prefs.breathingHapticEnabled.first())
    }

    @Test
    fun confirmFinalWipe_callsUserDataRepository() = runTest(testDispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onIntent(SettingsContract.Intent.RequestWipe)
        vm.onIntent(SettingsContract.Intent.ConfirmFirstStep)
        vm.onIntent(SettingsContract.Intent.ConfirmFinalWipe)
        advanceUntilIdle()

        assertEquals(1, userData.wipeInvocations)
        assertFalse(vm.state.value.isWiping)
        assertFalse(vm.state.value.isFinalConfirmingWipe)
    }
}
