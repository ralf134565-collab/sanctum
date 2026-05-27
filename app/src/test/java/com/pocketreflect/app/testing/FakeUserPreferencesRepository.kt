// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.testing

import com.pocketreflect.app.core.locale.AppLanguage
import com.pocketreflect.app.data.repository.AppThemeMode
import com.pocketreflect.app.data.repository.AutoLockTimeout
import com.pocketreflect.app.data.repository.UserPreferencesRepository
import com.pocketreflect.app.domain.breathing.BreathingHapticIntensity
import com.pocketreflect.app.domain.breathing.BreathingPattern
import com.pocketreflect.app.domain.chat.ChatPersona
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeUserPreferencesRepository : UserPreferencesRepository {
    private val lockEnabled = MutableStateFlow(false)
    private val autoLock = MutableStateFlow(AutoLockTimeout.DEFAULT)
    private val screenshotProtection = MutableStateFlow(true)
    private val disclaimer = MutableStateFlow(false)
    private val persona = MutableStateFlow(ChatPersona.DEFAULT)
    private val journalOn = MutableStateFlow(false)
    private val journalDays = MutableStateFlow(3)
    private val theme = MutableStateFlow(AppThemeMode.DEFAULT)
    private val language = MutableStateFlow(AppLanguage.DEFAULT)
    private val manifesto = MutableStateFlow("")
    private val mentorIncludeManifestoPref = MutableStateFlow(false)
    private val weeklyIncludeManifestoPref = MutableStateFlow(false)
    private val chatManifestoContextEnabledPref = MutableStateFlow(false)
    private val customFieldEnabled = MutableStateFlow(false)
    private val customFieldQuestion = MutableStateFlow("")
    private val customFieldHint = MutableStateFlow("")
    private val breathingPatternPref = MutableStateFlow(BreathingPattern.DEFAULT)
    private val breathingHaptic = MutableStateFlow(true)
    private val breathingHapticIntensityPref = MutableStateFlow(BreathingHapticIntensity.DEFAULT)
    private val breathingCycles = MutableStateFlow(6)
    private val uiHaptic = MutableStateFlow(true)

    override val biometricLockEnabled: Flow<Boolean> = lockEnabled.asStateFlow()
    override val autoLockTimeout: Flow<AutoLockTimeout> = autoLock.asStateFlow()
    override val screenshotProtectionEnabled: Flow<Boolean> = screenshotProtection.asStateFlow()
    override val chatDisclaimerAccepted: Flow<Boolean> = disclaimer.asStateFlow()
    override val chatPersona: Flow<ChatPersona> = persona.asStateFlow()
    override val chatJournalContextEnabled: Flow<Boolean> = journalOn.asStateFlow()
    override val chatJournalContextDays: Flow<Int> = journalDays.asStateFlow()
    override val themeMode: Flow<AppThemeMode> = theme.asStateFlow()
    override val appLanguage: Flow<AppLanguage> = language.asStateFlow()
    override val personalManifesto: Flow<String> = manifesto.asStateFlow()
    override val mentorIncludeManifesto: Flow<Boolean> = mentorIncludeManifestoPref.asStateFlow()
    override val weeklyIncludeManifesto: Flow<Boolean> = weeklyIncludeManifestoPref.asStateFlow()
    override val chatManifestoContextEnabled: Flow<Boolean> = chatManifestoContextEnabledPref.asStateFlow()
    override val customJournalFieldEnabled: Flow<Boolean> = customFieldEnabled.asStateFlow()
    override val customJournalFieldQuestion: Flow<String> = customFieldQuestion.asStateFlow()
    override val customJournalFieldHint: Flow<String> = customFieldHint.asStateFlow()
    override val breathingPattern: Flow<BreathingPattern> = breathingPatternPref.asStateFlow()
    override val breathingHapticEnabled: Flow<Boolean> = breathingHaptic.asStateFlow()
    override val breathingHapticIntensity: Flow<BreathingHapticIntensity> =
        breathingHapticIntensityPref.asStateFlow()
    override val breathingCycleCount: Flow<Int> = breathingCycles.asStateFlow()
    override val lastTimeEchoDismissedAt: Flow<Long?> = MutableStateFlow(null).asStateFlow()
    override val uiHapticEnabled: Flow<Boolean> = uiHaptic.asStateFlow()

    override suspend fun markTimeEchoDismissed(timestamp: Long) = Unit

    override suspend fun setBiometricLockEnabled(enabled: Boolean) {
        lockEnabled.value = enabled
    }

    override suspend fun setAutoLockTimeout(timeout: AutoLockTimeout) {
        autoLock.value = timeout
    }

    override suspend fun setScreenshotProtectionEnabled(enabled: Boolean) {
        screenshotProtection.value = enabled
    }

    override suspend fun setChatDisclaimerAccepted(accepted: Boolean) {
        disclaimer.value = accepted
    }

    override suspend fun setChatPersona(persona: ChatPersona) {
        this.persona.value = persona
    }

    override suspend fun setChatJournalContextEnabled(enabled: Boolean) {
        journalOn.value = enabled
    }

    override suspend fun setChatJournalContextDays(days: Int) {
        journalDays.value = days
    }

    override suspend fun setThemeMode(mode: AppThemeMode) {
        theme.value = mode
    }

    override suspend fun setAppLanguage(language: AppLanguage) {
        this.language.value = language
    }

    override suspend fun setPersonalManifesto(manifesto: String) {
        this.manifesto.value = manifesto
    }

    override suspend fun setMentorIncludeManifesto(enabled: Boolean) {
        mentorIncludeManifestoPref.value = enabled
    }

    override suspend fun setWeeklyIncludeManifesto(enabled: Boolean) {
        weeklyIncludeManifestoPref.value = enabled
    }

    override suspend fun setChatManifestoContextEnabled(enabled: Boolean) {
        chatManifestoContextEnabledPref.value = enabled
    }

    override suspend fun setCustomJournalFieldEnabled(enabled: Boolean) {
        customFieldEnabled.value = enabled
    }

    override suspend fun setCustomJournalFieldQuestion(question: String) {
        customFieldQuestion.value = question
    }

    override suspend fun setCustomJournalFieldHint(hint: String) {
        customFieldHint.value = hint
    }

    override suspend fun setBreathingPattern(pattern: BreathingPattern) {
        breathingPatternPref.value = pattern
    }

    override suspend fun setBreathingHapticEnabled(enabled: Boolean) {
        breathingHaptic.value = enabled
    }

    override suspend fun setBreathingHapticIntensity(intensity: BreathingHapticIntensity) {
        breathingHapticIntensityPref.value = intensity
    }

    override suspend fun setBreathingCycleCount(count: Int) {
        breathingCycles.value = count
    }

    override suspend fun setUiHapticEnabled(enabled: Boolean) {
        uiHaptic.value = enabled
    }

    override suspend fun ensureFirstRunLanguageBootstrap() = Unit

    var clearChatInvocations: Int = 0
        private set

    override suspend fun clearChatPreferences() {
        clearChatInvocations++
        disclaimer.value = false
        persona.value = ChatPersona.DEFAULT
        journalOn.value = false
        journalDays.value = 3
    }
}
