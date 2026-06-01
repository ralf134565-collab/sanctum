// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.testing

import com.pocketreflect.app.core.locale.AppLanguage
import com.pocketreflect.app.data.repository.AppThemeMode
import com.pocketreflect.app.data.repository.AutoLockTimeout
import com.pocketreflect.app.data.repository.UserPreferencesRepository
import com.pocketreflect.app.domain.breathing.BreathingHapticIntensity
import com.pocketreflect.app.domain.breathing.BreathingPattern
import com.pocketreflect.app.domain.chat.ChatPersona
import com.pocketreflect.insights.domain.InsightPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeUserPreferencesRepository : UserPreferencesRepository {
    private val lockEnabled = MutableStateFlow(false)
    private val autoLock = MutableStateFlow(AutoLockTimeout.DEFAULT)
    private val screenshotProtection = MutableStateFlow(true)
    private val disclaimer = MutableStateFlow(false)
    private val persona = MutableStateFlow(ChatPersona.DEFAULT)
    private val customPersonaEnabled = MutableStateFlow(false)
    private val customPersonaName = MutableStateFlow("")
    private val customPersonaPrompt = MutableStateFlow("")
    private val ambientMusicEnabledPref = MutableStateFlow(true)
    private val ambientMusicPausedPref = MutableStateFlow(true)
    private val ambientMusicVolumePref = MutableStateFlow(45)
    private val ambientMusicSelectedTrackPref = MutableStateFlow("")
    private val ambientMusicCustomJsonPref = MutableStateFlow("")
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
    private val breathingBridgeEnabledPref = MutableStateFlow(true)
    private val breathingPatternPref = MutableStateFlow(BreathingPattern.DEFAULT)
    private val breathingHaptic = MutableStateFlow(true)
    private val breathingHapticIntensityPref = MutableStateFlow(BreathingHapticIntensity.DEFAULT)
    private val breathingCycles = MutableStateFlow(6)
    private val uiHaptic = MutableStateFlow(true)
    private val warmupOnLaunch = MutableStateFlow(false)
    private val insightsWindowDaysPref = MutableStateFlow(InsightPolicy.WINDOW_30_DAYS)
    private val insightsTabEverOpenedPref = MutableStateFlow(false)
    private val insightsTabLastOpenedAtPref = MutableStateFlow<Long?>(null)
    private val insightsBannerLastShownPref = MutableStateFlow<Long?>(null)

    override val biometricLockEnabled: Flow<Boolean> = lockEnabled.asStateFlow()
    override val autoLockTimeout: Flow<AutoLockTimeout> = autoLock.asStateFlow()
    override val screenshotProtectionEnabled: Flow<Boolean> = screenshotProtection.asStateFlow()
    override val chatDisclaimerAccepted: Flow<Boolean> = disclaimer.asStateFlow()
    override val chatPersona: Flow<ChatPersona> = persona.asStateFlow()
    override val chatJournalContextEnabled: Flow<Boolean> = journalOn.asStateFlow()
    override val chatJournalContextDays: Flow<Int> = journalDays.asStateFlow()
    override val chatCustomPersonaEnabled: Flow<Boolean> = customPersonaEnabled.asStateFlow()
    override val chatCustomPersonaName: Flow<String> = customPersonaName.asStateFlow()
    override val chatCustomPersonaPrompt: Flow<String> = customPersonaPrompt.asStateFlow()
    override val ambientMusicEnabled: Flow<Boolean> = ambientMusicEnabledPref.asStateFlow()
    override val ambientMusicPausedByUser: Flow<Boolean> = ambientMusicPausedPref.asStateFlow()
    override val ambientMusicVolumePercent: Flow<Int> = ambientMusicVolumePref.asStateFlow()
    override val ambientMusicSelectedTrackId: Flow<String> = ambientMusicSelectedTrackPref.asStateFlow()
    override val ambientMusicCustomTracksJson: Flow<String> = ambientMusicCustomJsonPref.asStateFlow()
    override val themeMode: Flow<AppThemeMode> = theme.asStateFlow()
    override val appLanguage: Flow<AppLanguage> = language.asStateFlow()
    override val personalManifesto: Flow<String> = manifesto.asStateFlow()
    override val mentorIncludeManifesto: Flow<Boolean> = mentorIncludeManifestoPref.asStateFlow()
    override val weeklyIncludeManifesto: Flow<Boolean> = weeklyIncludeManifestoPref.asStateFlow()
    override val chatManifestoContextEnabled: Flow<Boolean> = chatManifestoContextEnabledPref.asStateFlow()
    override val customJournalFieldEnabled: Flow<Boolean> = customFieldEnabled.asStateFlow()
    override val customJournalFieldQuestion: Flow<String> = customFieldQuestion.asStateFlow()
    override val customJournalFieldHint: Flow<String> = customFieldHint.asStateFlow()
    override val breathingBridgeEnabled: Flow<Boolean> = breathingBridgeEnabledPref.asStateFlow()
    override val breathingPattern: Flow<BreathingPattern> = breathingPatternPref.asStateFlow()
    override val breathingHapticEnabled: Flow<Boolean> = breathingHaptic.asStateFlow()
    override val breathingHapticIntensity: Flow<BreathingHapticIntensity> =
        breathingHapticIntensityPref.asStateFlow()
    override val breathingCycleCount: Flow<Int> = breathingCycles.asStateFlow()
    override val lastTimeEchoDismissedAt: Flow<Long?> = MutableStateFlow(null).asStateFlow()
    override val uiHapticEnabled: Flow<Boolean> = uiHaptic.asStateFlow()
    override val warmupOnLaunchEnabled: Flow<Boolean> = warmupOnLaunch.asStateFlow()
    override val insightsWindowDays: Flow<Int> = insightsWindowDaysPref.asStateFlow()
    override val insightsTabEverOpened: Flow<Boolean> = insightsTabEverOpenedPref.asStateFlow()
    override val insightsTabLastOpenedAtMs: Flow<Long?> = insightsTabLastOpenedAtPref.asStateFlow()
    override val insightsBannerLastShownMs: Flow<Long?> = insightsBannerLastShownPref.asStateFlow()

    override val sandFlowEnabled: Flow<Boolean> = MutableStateFlow(true).asStateFlow()
    override val sandFlowBreathingSyncEnabled: Flow<Boolean> = MutableStateFlow(true).asStateFlow()
    override val sandFlowDifficulty: Flow<Int> = MutableStateFlow(80).asStateFlow()

    override suspend fun setSandFlowEnabled(enabled: Boolean) = Unit
    override suspend fun setSandFlowBreathingSyncEnabled(enabled: Boolean) = Unit
    override suspend fun setSandFlowDifficulty(difficulty: Int) = Unit

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

    override suspend fun setChatCustomPersonaEnabled(enabled: Boolean) {
        customPersonaEnabled.value = enabled
    }

    override suspend fun setChatCustomPersonaName(name: String) {
        customPersonaName.value = name
    }

    override suspend fun setChatCustomPersonaPrompt(prompt: String) {
        customPersonaPrompt.value = prompt
    }

    override suspend fun setAmbientMusicEnabled(enabled: Boolean) {
        ambientMusicEnabledPref.value = enabled
    }

    override suspend fun setAmbientMusicPausedByUser(paused: Boolean) {
        ambientMusicPausedPref.value = paused
    }

    override suspend fun setAmbientMusicVolumePercent(percent: Int) {
        ambientMusicVolumePref.value = percent
    }

    override suspend fun setAmbientMusicSelectedTrackId(trackId: String) {
        ambientMusicSelectedTrackPref.value = trackId
    }

    override suspend fun setAmbientMusicCustomTracksJson(json: String) {
        ambientMusicCustomJsonPref.value = json
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

    override suspend fun setBreathingBridgeEnabled(enabled: Boolean) {
        breathingBridgeEnabledPref.value = enabled
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

    override suspend fun setWarmupOnLaunchEnabled(enabled: Boolean) {
        warmupOnLaunch.value = enabled
    }

    override suspend fun setInsightsWindowDays(days: Int) {
        insightsWindowDaysPref.value = days
    }

    override suspend fun markInsightsTabOpened(timestampMs: Long) {
        insightsTabEverOpenedPref.value = true
        insightsTabLastOpenedAtPref.value = timestampMs
    }

    override suspend fun markInsightsBannerShown(timestampMs: Long) {
        insightsBannerLastShownPref.value = timestampMs
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
