// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketreflect.app.R
import com.pocketreflect.app.core.security.AuthSessionHolder
import com.pocketreflect.app.core.security.BiometricAvailability
import com.pocketreflect.app.core.locale.AppLocales
import com.pocketreflect.app.core.locale.AppLanguage
import com.pocketreflect.app.data.repository.AppThemeMode
import com.pocketreflect.app.data.repository.AutoLockTimeout
import com.pocketreflect.app.data.repository.ModelSelectionRepository
import com.pocketreflect.app.data.repository.UserDataRepository
import com.pocketreflect.app.data.repository.UserPreferencesRepository
import com.pocketreflect.app.domain.breathing.BreathingHapticIntensity
import com.pocketreflect.app.domain.breathing.BreathingPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val userDataRepository: UserDataRepository,
    private val modelSelectionRepository: ModelSelectionRepository,
    private val biometricAvailability: BiometricAvailability,
    private val authSessionHolder: AuthSessionHolder,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsContract.State())
    val state: StateFlow<SettingsContract.State> = _state.asStateFlow()

    private val _effects = Channel<SettingsContract.Effect>(capacity = Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        _state.update { it.copy(biometricStatus = biometricAvailability.status()) }

        viewModelScope.launch {
            userPreferencesRepository.biometricLockEnabled.collect { enabled ->
                _state.update { it.copy(biometricLockEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.autoLockTimeout.collect { timeout ->
                _state.update { it.copy(autoLockTimeout = timeout) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.screenshotProtectionEnabled.collect { enabled ->
                _state.update { it.copy(screenshotProtectionEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.uiHapticEnabled.collect { enabled ->
                _state.update { it.copy(uiHapticEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            modelSelectionRepository.attached.collect { attached ->
                _state.update { it.copy(attachedModel = attached) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.themeMode.collect { mode ->
                _state.update { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.appLanguage.collect { language ->
                _state.update { it.copy(appLanguage = language) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.customJournalFieldEnabled.collect { enabled ->
                _state.update { it.copy(customJournalFieldEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.customJournalFieldQuestion.collect { question ->
                _state.update { it.copy(customJournalFieldQuestion = question) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.customJournalFieldHint.collect { hint ->
                _state.update { it.copy(customJournalFieldHint = hint) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.breathingPattern.collect { pattern ->
                _state.update { it.copy(breathingPattern = pattern) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.breathingHapticEnabled.collect { enabled ->
                _state.update { it.copy(breathingHapticEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.breathingHapticIntensity.collect { intensity ->
                _state.update { it.copy(breathingHapticIntensity = intensity) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.breathingCycleCount.collect { count ->
                _state.update { it.copy(breathingCycleCount = count) }
            }
        }
    }

    fun onIntent(intent: SettingsContract.Intent) {
        when (intent) {
            SettingsContract.Intent.TogglePrivacyDetails ->
                _state.update { it.copy(isPrivacyExpanded = !it.isPrivacyExpanded) }

            is SettingsContract.Intent.ToggleBiometricLock ->
                setBiometricLockEnabled(intent.enabled)
            is SettingsContract.Intent.ToggleScreenshotProtection ->
                setScreenshotProtectionEnabled(intent.enabled)
            is SettingsContract.Intent.ToggleUiHaptic ->
                setUiHapticEnabled(intent.enabled)
            is SettingsContract.Intent.SetAutoLockTimeout ->
                setAutoLockTimeout(intent.timeout)

            SettingsContract.Intent.RequestWipe ->
                _state.update { it.copy(isConfirmingWipe = true) }
            SettingsContract.Intent.ConfirmFirstStep ->
                _state.update {
                    it.copy(isConfirmingWipe = false, isFinalConfirmingWipe = true)
                }
            SettingsContract.Intent.CancelWipe ->
                _state.update {
                    it.copy(isConfirmingWipe = false, isFinalConfirmingWipe = false)
                }
            SettingsContract.Intent.ConfirmFinalWipe -> performWipe()
            is SettingsContract.Intent.SetThemeMode ->
                setThemeMode(intent.mode)
            is SettingsContract.Intent.SetAppLanguage ->
                setAppLanguage(intent.language)
            is SettingsContract.Intent.ToggleCustomJournalField ->
                setCustomJournalFieldEnabled(intent.enabled)
            is SettingsContract.Intent.SetCustomJournalFieldQuestion ->
                setCustomJournalFieldQuestion(intent.question)
            is SettingsContract.Intent.SetCustomJournalFieldHint ->
                setCustomJournalFieldHint(intent.hint)
            is SettingsContract.Intent.SetBreathingPattern ->
                setBreathingPattern(intent.pattern)
            is SettingsContract.Intent.ToggleBreathingHaptic ->
                setBreathingHapticEnabled(intent.enabled)
            is SettingsContract.Intent.SetBreathingHapticIntensity ->
                setBreathingHapticIntensity(intent.intensity)
            is SettingsContract.Intent.SetBreathingCycleCount ->
                setBreathingCycleCount(intent.count)
        }
    }

    private fun setBiometricLockEnabled(enabled: Boolean) {
        if (enabled && _state.value.biometricStatus !is BiometricAvailability.Status.Available) {
            _effects.trySend(SettingsContract.Effect.ShowBiometricUnavailable)
            return
        }
        viewModelScope.launch {
            userPreferencesRepository.setBiometricLockEnabled(enabled)
            if (enabled) {
                authSessionHolder.markAuthenticated()
            }
        }
    }

    private fun setAutoLockTimeout(timeout: AutoLockTimeout) {
        viewModelScope.launch {
            userPreferencesRepository.setAutoLockTimeout(timeout)
        }
    }

    private fun setScreenshotProtectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setScreenshotProtectionEnabled(enabled)
        }
    }

    private fun setUiHapticEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setUiHapticEnabled(enabled)
        }
    }

    private fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeMode(mode)
        }
    }

    private fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            userPreferencesRepository.setAppLanguage(language)
            AppLocales.apply(language)
        }
    }

    private fun setCustomJournalFieldEnabled(enabled: Boolean) {
        if (enabled && _state.value.customJournalFieldQuestion.trim().isBlank()) {
            _effects.trySend(SettingsContract.Effect.CustomFieldQuestionRequired)
            return
        }
        viewModelScope.launch {
            userPreferencesRepository.setCustomJournalFieldEnabled(enabled)
        }
    }

    private fun setCustomJournalFieldQuestion(question: String) {
        _state.update { it.copy(customJournalFieldQuestion = question) }
        viewModelScope.launch {
            userPreferencesRepository.setCustomJournalFieldQuestion(question)
        }
    }

    private fun setCustomJournalFieldHint(hint: String) {
        _state.update { it.copy(customJournalFieldHint = hint) }
        viewModelScope.launch {
            userPreferencesRepository.setCustomJournalFieldHint(hint)
        }
    }

    private fun setBreathingPattern(pattern: BreathingPattern) {
        viewModelScope.launch {
            userPreferencesRepository.setBreathingPattern(pattern)
        }
    }

    private fun setBreathingHapticEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setBreathingHapticEnabled(enabled)
        }
    }

    private fun setBreathingHapticIntensity(intensity: BreathingHapticIntensity) {
        viewModelScope.launch {
            userPreferencesRepository.setBreathingHapticIntensity(intensity)
        }
    }

    private fun setBreathingCycleCount(count: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setBreathingCycleCount(count)
        }
    }

    private fun performWipe() {
        viewModelScope.launch {
            _state.update { it.copy(isWiping = true) }
            try {
                userDataRepository.wipeAllUserContent()
                _state.update {
                    it.copy(
                        isWiping = false,
                        isFinalConfirmingWipe = false,
                    )
                }
                _effects.trySend(SettingsContract.Effect.WipeCompleted)
            } catch (t: Throwable) {
                _state.update { it.copy(isWiping = false, isFinalConfirmingWipe = false) }
                _effects.trySend(
                    SettingsContract.Effect.ShowError(
                        appContext.getString(R.string.settings_wipe_error),
                    ),
                )
            }
        }
    }
}
