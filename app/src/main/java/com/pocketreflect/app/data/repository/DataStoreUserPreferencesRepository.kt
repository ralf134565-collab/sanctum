// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pocketreflect.app.core.locale.AppLanguage
import com.pocketreflect.app.domain.breathing.BreathingHapticIntensity
import com.pocketreflect.app.domain.breathing.BreathingPattern
import com.pocketreflect.app.domain.chat.ChatCustomPersonaPolicy
import com.pocketreflect.app.domain.chat.ChatPersona
import com.pocketreflect.insights.domain.InsightPolicy
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-реализация [UserPreferencesRepository].
 *
 * `DataStore<Preferences>` инжектится из [com.pocketreflect.app.di.DataStoreModule],
 * чтобы тесты могли подменить файл-источник через `PreferenceDataStoreFactory.create`
 * с временным каталогом.
 */
@Singleton
class DataStoreUserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    private val safePrefs: Flow<Preferences> = dataStore.data.catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }

    override val biometricLockEnabled: Flow<Boolean> =
        safePrefs.map { prefs -> prefs[KEY_BIOMETRIC_LOCK_ENABLED] ?: false }

    override val autoLockTimeout: Flow<AutoLockTimeout> =
        safePrefs.map { prefs -> AutoLockTimeout.fromMillis(prefs[KEY_AUTO_LOCK_TIMEOUT_MS]) }

    override suspend fun setBiometricLockEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_BIOMETRIC_LOCK_ENABLED] = enabled }
    }

    override suspend fun setAutoLockTimeout(timeout: AutoLockTimeout) {
        dataStore.edit { prefs -> prefs[KEY_AUTO_LOCK_TIMEOUT_MS] = timeout.millis }
    }

    override val screenshotProtectionEnabled: Flow<Boolean> =
        safePrefs.map { prefs -> prefs[KEY_SCREENSHOT_PROTECTION_ENABLED] ?: true }

    override suspend fun setScreenshotProtectionEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_SCREENSHOT_PROTECTION_ENABLED] = enabled }
    }

    override val chatDisclaimerAccepted: Flow<Boolean> =
        safePrefs.map { prefs -> prefs[KEY_CHAT_DISCLAIMER_ACCEPTED] ?: false }

    override val chatPersona: Flow<ChatPersona> =
        safePrefs.map { prefs ->
            ChatPersona.fromStorageKey(prefs[KEY_CHAT_PERSONA])
        }

    override val chatJournalContextEnabled: Flow<Boolean> =
        safePrefs.map { prefs -> prefs[KEY_CHAT_JOURNAL_CONTEXT_ENABLED] ?: false }

    override val chatJournalContextDays: Flow<Int> =
        safePrefs.map { prefs ->
            normalizeJournalDays(prefs[KEY_CHAT_JOURNAL_CONTEXT_DAYS])
        }

    override suspend fun setChatDisclaimerAccepted(accepted: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_CHAT_DISCLAIMER_ACCEPTED] = accepted }
    }

    override suspend fun setChatPersona(persona: ChatPersona) {
        dataStore.edit { prefs -> prefs[KEY_CHAT_PERSONA] = persona.storageKey }
    }

    override val chatCustomPersonaEnabled: Flow<Boolean> =
        safePrefs.map { prefs -> prefs[KEY_CHAT_CUSTOM_PERSONA_ENABLED] ?: false }

    override val chatCustomPersonaName: Flow<String> =
        safePrefs.map { prefs -> prefs[KEY_CHAT_CUSTOM_PERSONA_NAME] ?: "" }

    override val chatCustomPersonaPrompt: Flow<String> =
        safePrefs.map { prefs -> prefs[KEY_CHAT_CUSTOM_PERSONA_PROMPT] ?: "" }

    override suspend fun setChatCustomPersonaEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_CHAT_CUSTOM_PERSONA_ENABLED] = enabled }
    }

    override suspend fun setChatCustomPersonaName(name: String) {
        dataStore.edit { prefs ->
            prefs[KEY_CHAT_CUSTOM_PERSONA_NAME] =
                ChatCustomPersonaPolicy.normalizeDisplayName(name)
        }
    }

    override suspend fun setChatCustomPersonaPrompt(prompt: String) {
        dataStore.edit { prefs ->
            prefs[KEY_CHAT_CUSTOM_PERSONA_PROMPT] =
                ChatCustomPersonaPolicy.normalizePrompt(prompt)
        }
    }

    override suspend fun setChatJournalContextEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_CHAT_JOURNAL_CONTEXT_ENABLED] = enabled }
    }

    override suspend fun setChatJournalContextDays(days: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_CHAT_JOURNAL_CONTEXT_DAYS] = normalizeJournalDays(days)
        }
    }

    override suspend fun clearChatPreferences() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_CHAT_DISCLAIMER_ACCEPTED)
            prefs.remove(KEY_CHAT_PERSONA)
            prefs.remove(KEY_CHAT_JOURNAL_CONTEXT_ENABLED)
            prefs.remove(KEY_CHAT_JOURNAL_CONTEXT_DAYS)
        }
    }

    override val themeMode: Flow<AppThemeMode> =
        safePrefs.map { prefs ->
            AppThemeMode.fromStorageKey(prefs[KEY_APP_THEME_MODE])
        }

    override suspend fun setThemeMode(mode: AppThemeMode) {
        dataStore.edit { prefs -> prefs[KEY_APP_THEME_MODE] = mode.storageKey }
    }

    override val appLanguage: Flow<AppLanguage> =
        safePrefs.map { prefs ->
            AppLanguage.fromStorageKey(prefs[KEY_APP_LANGUAGE])
        }

    override suspend fun setAppLanguage(language: AppLanguage) {
        dataStore.edit { prefs ->
            val key = language.storageKey
            if (key == null) {
                prefs.remove(KEY_APP_LANGUAGE)
            } else {
                prefs[KEY_APP_LANGUAGE] = key
            }
        }
    }

    override val personalManifesto: Flow<String> =
        safePrefs.map { prefs -> prefs[KEY_PERSONAL_MANIFESTO] ?: "" }

    override suspend fun setPersonalManifesto(manifesto: String) {
        dataStore.edit { prefs -> prefs[KEY_PERSONAL_MANIFESTO] = manifesto }
    }

    override val mentorIncludeManifesto: Flow<Boolean> =
        safePrefs.map { prefs -> prefs[KEY_MENTOR_INCLUDE_MANIFESTO] ?: false }

    override val weeklyIncludeManifesto: Flow<Boolean> =
        safePrefs.map { prefs -> prefs[KEY_WEEKLY_INCLUDE_MANIFESTO] ?: false }

    override val chatManifestoContextEnabled: Flow<Boolean> =
        safePrefs.map { prefs -> prefs[KEY_CHAT_MANIFESTO_CONTEXT_ENABLED] ?: false }

    override suspend fun setMentorIncludeManifesto(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_MENTOR_INCLUDE_MANIFESTO] = enabled }
    }

    override suspend fun setWeeklyIncludeManifesto(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_WEEKLY_INCLUDE_MANIFESTO] = enabled }
    }

    override suspend fun setChatManifestoContextEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_CHAT_MANIFESTO_CONTEXT_ENABLED] = enabled }
    }

    override val lastTimeEchoDismissedAt: Flow<Long?> =
        safePrefs.map { prefs -> prefs[KEY_LAST_TIME_ECHO_DISMISSED_AT] }

    override suspend fun markTimeEchoDismissed(timestamp: Long) {
        dataStore.edit { prefs -> prefs[KEY_LAST_TIME_ECHO_DISMISSED_AT] = timestamp }
    }

    override val customJournalFieldEnabled: Flow<Boolean> =
        safePrefs.map { prefs -> prefs[KEY_CUSTOM_JOURNAL_FIELD_ENABLED] ?: false }

    override val customJournalFieldQuestion: Flow<String> =
        safePrefs.map { prefs -> prefs[KEY_CUSTOM_JOURNAL_FIELD_QUESTION] ?: "" }

    override val customJournalFieldHint: Flow<String> =
        safePrefs.map { prefs -> prefs[KEY_CUSTOM_JOURNAL_FIELD_HINT] ?: "" }

    override val sandFlowEnabled: Flow<Boolean> =
        safePrefs.map { prefs -> prefs[KEY_SAND_FLOW_ENABLED] ?: true }

    override val sandFlowBreathingSyncEnabled: Flow<Boolean> =
        safePrefs.map { prefs -> prefs[KEY_SAND_FLOW_BREATHING_SYNC_ENABLED] ?: true }

    override val sandFlowDifficulty: Flow<Int> =
        safePrefs.map { prefs -> prefs[KEY_SAND_FLOW_DIFFICULTY] ?: 80 }

    override suspend fun setSandFlowEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_SAND_FLOW_ENABLED] = enabled }
    }

    override suspend fun setSandFlowBreathingSyncEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_SAND_FLOW_BREATHING_SYNC_ENABLED] = enabled }
    }

    override suspend fun setSandFlowDifficulty(difficulty: Int) {
        dataStore.edit { prefs -> prefs[KEY_SAND_FLOW_DIFFICULTY] = difficulty }
    }

    override suspend fun setCustomJournalFieldEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_CUSTOM_JOURNAL_FIELD_ENABLED] = enabled }
    }

    override suspend fun setCustomJournalFieldQuestion(question: String) {
        dataStore.edit { prefs ->
            prefs[KEY_CUSTOM_JOURNAL_FIELD_QUESTION] = question.take(CUSTOM_JOURNAL_FIELD_QUESTION_MAX)
        }
    }

    override suspend fun setCustomJournalFieldHint(hint: String) {
        dataStore.edit { prefs ->
            prefs[KEY_CUSTOM_JOURNAL_FIELD_HINT] = hint.take(CUSTOM_JOURNAL_FIELD_HINT_MAX)
        }
    }

    override val breathingBridgeEnabled: Flow<Boolean> =
        safePrefs.map { prefs -> prefs[KEY_BREATHING_BRIDGE_ENABLED] ?: true }

    override suspend fun setBreathingBridgeEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_BREATHING_BRIDGE_ENABLED] = enabled }
    }

    override val ambientMusicEnabled: Flow<Boolean> =
        safePrefs.map { prefs -> prefs[KEY_AMBIENT_MUSIC_ENABLED] ?: true }

    override val ambientMusicPausedByUser: Flow<Boolean> =
        safePrefs.map { prefs -> prefs[KEY_AMBIENT_MUSIC_PAUSED_BY_USER] ?: true }

    override val ambientMusicVolumePercent: Flow<Int> =
        safePrefs.map { prefs ->
            normalizeAmbientVolumePercent(prefs[KEY_AMBIENT_MUSIC_VOLUME_PERCENT])
        }

    override val ambientMusicSelectedTrackId: Flow<String> =
        safePrefs.map { prefs -> prefs[KEY_AMBIENT_MUSIC_SELECTED_TRACK_ID] ?: "" }

    override val ambientMusicCustomTracksJson: Flow<String> =
        safePrefs.map { prefs -> prefs[KEY_AMBIENT_MUSIC_CUSTOM_TRACKS_JSON] ?: "" }

    override suspend fun setAmbientMusicEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_AMBIENT_MUSIC_ENABLED] = enabled }
    }

    override suspend fun setAmbientMusicPausedByUser(paused: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_AMBIENT_MUSIC_PAUSED_BY_USER] = paused }
    }

    override suspend fun setAmbientMusicVolumePercent(percent: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_AMBIENT_MUSIC_VOLUME_PERCENT] = normalizeAmbientVolumePercent(percent)
        }
    }

    override suspend fun setAmbientMusicSelectedTrackId(trackId: String) {
        dataStore.edit { prefs -> prefs[KEY_AMBIENT_MUSIC_SELECTED_TRACK_ID] = trackId }
    }

    override suspend fun setAmbientMusicCustomTracksJson(json: String) {
        dataStore.edit { prefs -> prefs[KEY_AMBIENT_MUSIC_CUSTOM_TRACKS_JSON] = json }
    }

    override val breathingPattern: Flow<BreathingPattern> =
        safePrefs.map { prefs ->
            BreathingPattern.fromStorageKey(prefs[KEY_BREATHING_PATTERN])
        }

    override val breathingHapticEnabled: Flow<Boolean> =
        safePrefs.map { prefs -> prefs[KEY_BREATHING_HAPTIC_ENABLED] ?: true }

    override val breathingHapticIntensity: Flow<BreathingHapticIntensity> =
        safePrefs.map { prefs ->
            BreathingHapticIntensity.fromStorageKey(prefs[KEY_BREATHING_HAPTIC_INTENSITY])
        }

    override val breathingCycleCount: Flow<Int> =
        safePrefs.map { prefs ->
            normalizeBreathingCycleCount(prefs[KEY_BREATHING_CYCLE_COUNT])
        }

    override val uiHapticEnabled: Flow<Boolean> =
        safePrefs.map { prefs -> prefs[KEY_UI_HAPTIC_ENABLED] ?: true }

    override val warmupOnLaunchEnabled: Flow<Boolean> =
        safePrefs.map { prefs -> prefs[KEY_WARMUP_ON_LAUNCH_ENABLED] ?: false }

    override suspend fun setWarmupOnLaunchEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_WARMUP_ON_LAUNCH_ENABLED] = enabled }
    }

    override suspend fun setBreathingPattern(pattern: BreathingPattern) {
        dataStore.edit { prefs -> prefs[KEY_BREATHING_PATTERN] = pattern.storageKey }
    }

    override suspend fun setBreathingHapticEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_BREATHING_HAPTIC_ENABLED] = enabled }
    }

    override suspend fun setBreathingHapticIntensity(intensity: BreathingHapticIntensity) {
        dataStore.edit { prefs -> prefs[KEY_BREATHING_HAPTIC_INTENSITY] = intensity.storageKey }
    }

    override suspend fun setBreathingCycleCount(count: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_BREATHING_CYCLE_COUNT] = normalizeBreathingCycleCount(count)
        }
    }

    override suspend fun setUiHapticEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_UI_HAPTIC_ENABLED] = enabled }
    }

    override val insightsWindowDays: Flow<Int> =
        safePrefs.map { prefs -> normalizeInsightsWindowDays(prefs[KEY_INSIGHTS_WINDOW_DAYS]) }

    override suspend fun setInsightsWindowDays(days: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_INSIGHTS_WINDOW_DAYS] = normalizeInsightsWindowDays(days)
        }
    }

    override val insightsTabEverOpened: Flow<Boolean> =
        safePrefs.map { prefs -> prefs[KEY_INSIGHTS_TAB_EVER_OPENED] ?: false }

    override val insightsTabLastOpenedAtMs: Flow<Long?> =
        safePrefs.map { prefs -> prefs[KEY_INSIGHTS_TAB_LAST_OPENED_AT] }

    override val insightsBannerLastShownMs: Flow<Long?> =
        safePrefs.map { prefs -> prefs[KEY_INSIGHTS_BANNER_LAST_SHOWN_AT] }

    override suspend fun markInsightsTabOpened(timestampMs: Long) {
        dataStore.edit { prefs ->
            prefs[KEY_INSIGHTS_TAB_EVER_OPENED] = true
            prefs[KEY_INSIGHTS_TAB_LAST_OPENED_AT] = timestampMs
        }
    }

    override suspend fun markInsightsBannerShown(timestampMs: Long) {
        dataStore.edit { prefs -> prefs[KEY_INSIGHTS_BANNER_LAST_SHOWN_AT] = timestampMs }
    }

    override suspend fun ensureFirstRunLanguageBootstrap() {
        val prefs = safePrefs.first()
        if (prefs[KEY_APP_LANGUAGE] != null) return
        val lang = Locale.getDefault().language.lowercase(Locale.ROOT)
        if (!lang.startsWith("en")) return
        setAppLanguage(AppLanguage.EN)
    }

    private companion object {
        val KEY_BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("biometric_lock_enabled")
        val KEY_AUTO_LOCK_TIMEOUT_MS = longPreferencesKey("auto_lock_timeout_ms")
        val KEY_SCREENSHOT_PROTECTION_ENABLED = booleanPreferencesKey("screenshot_protection_enabled")
        val KEY_CHAT_DISCLAIMER_ACCEPTED = booleanPreferencesKey("chat_disclaimer_accepted")
        val KEY_CHAT_PERSONA = stringPreferencesKey("chat_persona")
        val KEY_CHAT_CUSTOM_PERSONA_ENABLED = booleanPreferencesKey("chat_custom_persona_enabled")
        val KEY_CHAT_CUSTOM_PERSONA_NAME = stringPreferencesKey("chat_custom_persona_name")
        val KEY_CHAT_CUSTOM_PERSONA_PROMPT = stringPreferencesKey("chat_custom_persona_prompt")
        val KEY_CHAT_JOURNAL_CONTEXT_ENABLED = booleanPreferencesKey("chat_journal_context_enabled")
        val KEY_CHAT_JOURNAL_CONTEXT_DAYS = intPreferencesKey("chat_journal_context_days")
        val KEY_APP_THEME_MODE = stringPreferencesKey("app_theme_mode")
        val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_PERSONAL_MANIFESTO = stringPreferencesKey("personal_manifesto")
        val KEY_MENTOR_INCLUDE_MANIFESTO = booleanPreferencesKey("mentor_include_manifesto")
        val KEY_WEEKLY_INCLUDE_MANIFESTO = booleanPreferencesKey("weekly_include_manifesto")
        val KEY_CHAT_MANIFESTO_CONTEXT_ENABLED = booleanPreferencesKey("chat_manifesto_context_enabled")
        val KEY_LAST_TIME_ECHO_DISMISSED_AT = longPreferencesKey("last_time_echo_dismissed_at")
        val KEY_CUSTOM_JOURNAL_FIELD_ENABLED = booleanPreferencesKey("custom_journal_field_enabled")
        val KEY_CUSTOM_JOURNAL_FIELD_QUESTION = stringPreferencesKey("custom_journal_field_question")
        val KEY_CUSTOM_JOURNAL_FIELD_HINT = stringPreferencesKey("custom_journal_field_hint")
        val KEY_SAND_FLOW_ENABLED = booleanPreferencesKey("sand_flow_enabled")
        val KEY_SAND_FLOW_BREATHING_SYNC_ENABLED = booleanPreferencesKey("sand_flow_breathing_sync_enabled")
        val KEY_SAND_FLOW_DIFFICULTY = intPreferencesKey("sand_flow_difficulty")
        val KEY_BREATHING_BRIDGE_ENABLED = booleanPreferencesKey("breathing_bridge_enabled")
        val KEY_AMBIENT_MUSIC_ENABLED = booleanPreferencesKey("ambient_music_enabled")
        val KEY_AMBIENT_MUSIC_PAUSED_BY_USER = booleanPreferencesKey("ambient_music_paused_by_user")
        val KEY_AMBIENT_MUSIC_VOLUME_PERCENT = intPreferencesKey("ambient_music_volume_percent")
        val KEY_AMBIENT_MUSIC_SELECTED_TRACK_ID = stringPreferencesKey("ambient_music_selected_track_id")
        val KEY_AMBIENT_MUSIC_CUSTOM_TRACKS_JSON = stringPreferencesKey("ambient_music_custom_tracks_json")
        val KEY_BREATHING_PATTERN = stringPreferencesKey("breathing_pattern")
        val KEY_BREATHING_HAPTIC_ENABLED = booleanPreferencesKey("breathing_haptic_enabled")
        val KEY_BREATHING_HAPTIC_INTENSITY = stringPreferencesKey("breathing_haptic_intensity")
        val KEY_BREATHING_CYCLE_COUNT = intPreferencesKey("breathing_cycle_count")
        val KEY_UI_HAPTIC_ENABLED = booleanPreferencesKey("ui_haptic_enabled")
        val KEY_WARMUP_ON_LAUNCH_ENABLED = booleanPreferencesKey("warmup_on_launch_enabled")
        val KEY_INSIGHTS_WINDOW_DAYS = intPreferencesKey("insights_window_days")
        val KEY_INSIGHTS_TAB_EVER_OPENED = booleanPreferencesKey("insights_tab_ever_opened")
        val KEY_INSIGHTS_TAB_LAST_OPENED_AT = longPreferencesKey("insights_tab_last_opened_at")
        val KEY_INSIGHTS_BANNER_LAST_SHOWN_AT = longPreferencesKey("insights_banner_last_shown_ms")

        const val CUSTOM_JOURNAL_FIELD_QUESTION_MAX = 120
        const val CUSTOM_JOURNAL_FIELD_HINT_MAX = 240

        fun normalizeJournalDays(raw: Int?): Int = when (raw) {
            1, 3, 7 -> raw
            else -> 3
        }

        fun normalizeBreathingCycleCount(raw: Int?): Int = when (raw) {
            3, 4, 5, 6, 7, 8 -> raw
            else -> 6
        }

        fun normalizeInsightsWindowDays(raw: Int?): Int = when (raw) {
            InsightPolicy.WINDOW_90_DAYS -> InsightPolicy.WINDOW_90_DAYS
            else -> InsightPolicy.WINDOW_30_DAYS
        }

        fun normalizeAmbientVolumePercent(raw: Int?): Int =
            (raw ?: com.pocketreflect.app.domain.ambient.AmbientMusicPolicy.volumeToPercent(
                com.pocketreflect.app.domain.ambient.AmbientMusicPolicy.DEFAULT_VOLUME,
            )).coerceIn(0, 100)
    }
}
