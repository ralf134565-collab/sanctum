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
import android.net.Uri
import com.pocketreflect.app.data.ambient.AmbientMusicStorage
import com.pocketreflect.app.domain.ambient.AmbientMusicPolicy
import com.pocketreflect.app.domain.chat.ChatPersona
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val userDataRepository: UserDataRepository,
    private val modelSelectionRepository: ModelSelectionRepository,
    private val biometricAvailability: BiometricAvailability,
    private val authSessionHolder: AuthSessionHolder,
    private val ambientMusicStorage: AmbientMusicStorage,
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
            userPreferencesRepository.breathingBridgeEnabled.collect { enabled ->
                _state.update { it.copy(breathingBridgeEnabled = enabled) }
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
        viewModelScope.launch {
            userPreferencesRepository.sandFlowEnabled.collect { enabled ->
                _state.update { it.copy(sandFlowEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.sandFlowBreathingSyncEnabled.collect { enabled ->
                _state.update { it.copy(sandFlowBreathingSyncEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.sandFlowDifficulty.collect { difficulty ->
                _state.update { it.copy(sandFlowDifficulty = difficulty) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.chatCustomPersonaEnabled.collect { enabled ->
                _state.update { it.copy(chatCustomPersonaEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.chatCustomPersonaName.collect { name ->
                _state.update { it.copy(chatCustomPersonaName = name) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.chatCustomPersonaPrompt.collect { prompt ->
                _state.update { it.copy(chatCustomPersonaPrompt = prompt) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.ambientMusicEnabled.collect { enabled ->
                _state.update { it.copy(ambientMusicEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.ambientMusicCustomTracksJson.collect { json ->
                _state.update {
                    it.copy(ambientMusicCustomTracks = ambientMusicStorage.parseCustomTracks(json))
                }
            }
        }
    }

    fun importAmbientTrack(sourceUri: Uri, displayName: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val existing = ambientMusicStorage.parseCustomTracks(
                    userPreferencesRepository.ambientMusicCustomTracksJson.first(),
                )
                ambientMusicStorage.importCustomTrack(sourceUri, displayName = displayName, existing = existing)
            }
            result.fold(
                onSuccess = { track ->
                    val existing = _state.value.ambientMusicCustomTracks
                    val updated = existing + track
                    userPreferencesRepository.setAmbientMusicCustomTracksJson(
                        ambientMusicStorage.encodeCustomTracks(updated),
                    )
                },
                onFailure = { error ->
                    val messageRes = when (error.message) {
                        "max_custom_tracks" -> R.string.ambient_music_import_max
                        "file_too_large" -> R.string.ambient_music_import_too_large
                        "unsupported_format" -> R.string.ambient_music_import_unsupported
                        else -> R.string.ambient_music_import_failed
                    }
                    _effects.trySend(SettingsContract.Effect.ShowError(appContext.getString(messageRes)))
                },
            )
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
            is SettingsContract.Intent.ToggleBreathingBridge ->
                setBreathingBridgeEnabled(intent.enabled)
            is SettingsContract.Intent.SetBreathingPattern ->
                setBreathingPattern(intent.pattern)
            is SettingsContract.Intent.ToggleBreathingHaptic ->
                setBreathingHapticEnabled(intent.enabled)
            is SettingsContract.Intent.SetBreathingHapticIntensity ->
                setBreathingHapticIntensity(intent.intensity)
            is SettingsContract.Intent.SetBreathingCycleCount ->
                setBreathingCycleCount(intent.count)
            is SettingsContract.Intent.ToggleSandFlow ->
                setSandFlowEnabled(intent.enabled)
            is SettingsContract.Intent.ToggleSandFlowBreathingSync ->
                setSandFlowBreathingSyncEnabled(intent.enabled)
            is SettingsContract.Intent.SetSandFlowDifficulty ->
                setSandFlowDifficulty(intent.difficulty)
            is SettingsContract.Intent.ToggleChatCustomPersona ->
                setChatCustomPersonaEnabled(intent.enabled)
            is SettingsContract.Intent.SetChatCustomPersonaName ->
                setChatCustomPersonaName(intent.name)
            is SettingsContract.Intent.SetChatCustomPersonaPrompt ->
                setChatCustomPersonaPrompt(intent.prompt)
            is SettingsContract.Intent.ApplyChatCustomPersonaTemplate ->
                applyChatCustomPersonaTemplate(intent.template)
            is SettingsContract.Intent.ToggleAmbientMusic ->
                setAmbientMusicEnabled(intent.enabled)
            is SettingsContract.Intent.RemoveAmbientCustomTrack ->
                removeAmbientCustomTrack(intent.trackId)
            is SettingsContract.Intent.RenameAmbientCustomTrack ->
                renameAmbientCustomTrack(intent.trackId, intent.displayName)
        }
    }

    private fun setBiometricLockEnabled(enabled: Boolean) {
        if (enabled && _state.value.biometricStatus !is BiometricAvailability.Status.Available) {
            _effects.trySend(SettingsContract.Effect.ShowBiometricUnavailable)
            return
        }
        viewModelScope.launch {
            userPreferencesRepository.setBiometricLockEnabled(enabled)
            authSessionHolder.setRuntimeLockEnabled(enabled)
            if (enabled) {
                authSessionHolder.requireLockAfterEnabling()
            } else {
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

    private fun setBreathingBridgeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setBreathingBridgeEnabled(enabled)
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

    private fun setSandFlowEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setSandFlowEnabled(enabled)
        }
    }

    private fun setSandFlowBreathingSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setSandFlowBreathingSyncEnabled(enabled)
        }
    }

    private fun setSandFlowDifficulty(difficulty: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setSandFlowDifficulty(difficulty)
        }
    }

    private fun setChatCustomPersonaEnabled(enabled: Boolean) {
        if (enabled && _state.value.chatCustomPersonaPrompt.trim().isBlank()) {
            _effects.trySend(SettingsContract.Effect.CustomPersonaPromptRequired)
            return
        }
        viewModelScope.launch {
            userPreferencesRepository.setChatCustomPersonaEnabled(enabled)
            if (!enabled && userPreferencesRepository.chatPersona.first() == ChatPersona.CUSTOM) {
                userPreferencesRepository.setChatPersona(ChatPersona.DEFAULT)
            }
        }
    }

    private fun setChatCustomPersonaName(name: String) {
        _state.update { it.copy(chatCustomPersonaName = name) }
        viewModelScope.launch {
            userPreferencesRepository.setChatCustomPersonaName(name)
        }
    }

    private fun setChatCustomPersonaPrompt(prompt: String) {
        _state.update { it.copy(chatCustomPersonaPrompt = prompt) }
        viewModelScope.launch {
            userPreferencesRepository.setChatCustomPersonaPrompt(prompt)
        }
    }

    private fun setAmbientMusicEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setAmbientMusicEnabled(enabled)
            if (!enabled) {
                userPreferencesRepository.setAmbientMusicPausedByUser(true)
            }
        }
    }

    private fun removeAmbientCustomTrack(trackId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = _state.value.ambientMusicCustomTracks
            val track = existing.firstOrNull { it.id == trackId } ?: return@launch
            val updated = ambientMusicStorage.deleteCustomTrack(track, existing)
            userPreferencesRepository.setAmbientMusicCustomTracksJson(
                ambientMusicStorage.encodeCustomTracks(updated),
            )
        }
    }

    fun renameAmbientCustomTrack(trackId: String, displayName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = _state.value.ambientMusicCustomTracks
            val index = existing.indexOfFirst { it.id == trackId }
            if (index < 0) return@launch
            val trimmed = displayName.trim().take(AmbientMusicPolicy.MAX_DISPLAY_NAME_LENGTH)
            if (trimmed.isBlank()) return@launch
            val updated = existing.toMutableList().apply {
                set(index, existing[index].copy(displayName = trimmed))
            }
            userPreferencesRepository.setAmbientMusicCustomTracksJson(
                ambientMusicStorage.encodeCustomTracks(updated),
            )
        }
    }

    private fun applyChatCustomPersonaTemplate(template: SettingsContract.ChatCustomPersonaTemplate) {
        val text = when (template) {
            SettingsContract.ChatCustomPersonaTemplate.QUIET_LISTENER ->
                appContext.getString(R.string.chat_custom_persona_template_quiet)
            SettingsContract.ChatCustomPersonaTemplate.GENTLE_QUESTIONS ->
                appContext.getString(R.string.chat_custom_persona_template_questions)
            SettingsContract.ChatCustomPersonaTemplate.NO_CLICHES ->
                appContext.getString(R.string.chat_custom_persona_template_no_cliches)
        }
        setChatCustomPersonaPrompt(text)
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
