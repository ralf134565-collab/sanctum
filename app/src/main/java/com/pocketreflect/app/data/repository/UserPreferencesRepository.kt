// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.repository

import com.pocketreflect.app.core.locale.AppLanguage
import com.pocketreflect.app.domain.breathing.BreathingHapticIntensity
import com.pocketreflect.app.domain.breathing.BreathingPattern
import com.pocketreflect.app.domain.chat.ChatPersona
import kotlinx.coroutines.flow.Flow

/**
 * Абстракция над пользовательскими настройками PocketReflect.
 *
 * Использует DataStore<Preferences> вместо SharedPreferences:
 *  - корректно работает с корутинами (нет блокирующего apply()/commit());
 *  - переживает синхронные краши в момент записи;
 *  - реализация целиком в [com.pocketreflect.app.data.repository.DataStoreUserPreferencesRepository].
 *
 * В тестах подменяется на in-memory реализацию.
 */
interface UserPreferencesRepository {

    /**
     * Включён ли биометрический lock на запуске приложения и при возврате из background.
     * По умолчанию `false` — минимальное трение онбординга для свежей установки.
     */
    val biometricLockEnabled: Flow<Boolean>

    /**
     * Сколько миллисекунд приложение должно пробыть в background, прежде чем
     * при возврате потребуется новая аутентификация.
     */
    val autoLockTimeout: Flow<AutoLockTimeout>

    suspend fun setBiometricLockEnabled(enabled: Boolean)
    suspend fun setAutoLockTimeout(timeout: AutoLockTimeout)

    /**
     * Запрет скриншотов и записи экрана ([android.view.WindowManager.LayoutParams.FLAG_SECURE]).
     * По умолчанию `true` — содержимое дневника нельзя снять через системные средства.
     */
    val screenshotProtectionEnabled: Flow<Boolean>

    suspend fun setScreenshotProtectionEnabled(enabled: Boolean)

    // --- Чат (D5) ---

    val chatDisclaimerAccepted: Flow<Boolean>
    val chatPersona: Flow<ChatPersona>
    val chatJournalContextEnabled: Flow<Boolean>
    /** 1, 3 или 7 — дней дневника в контексте чата. */
    val chatJournalContextDays: Flow<Int>

    suspend fun setChatDisclaimerAccepted(accepted: Boolean)
    suspend fun setChatPersona(persona: ChatPersona)
    suspend fun setChatJournalContextEnabled(enabled: Boolean)
    suspend fun setChatJournalContextDays(days: Int)

    val chatCustomPersonaEnabled: Flow<Boolean>
    val chatCustomPersonaName: Flow<String>
    val chatCustomPersonaPrompt: Flow<String>

    suspend fun setChatCustomPersonaEnabled(enabled: Boolean)
    suspend fun setChatCustomPersonaName(name: String)
    suspend fun setChatCustomPersonaPrompt(prompt: String)

    /** Сброс настроек чата при «Стереть всю историю». */
    suspend fun clearChatPreferences()

    // --- Оформление ---

    val themeMode: Flow<AppThemeMode>

    suspend fun setThemeMode(mode: AppThemeMode)

    // --- Язык ---

    val appLanguage: Flow<AppLanguage>

    suspend fun setAppLanguage(language: AppLanguage)

    /** Личные ориентиры / Манифест бережности */
    val personalManifesto: Flow<String>

    suspend fun setPersonalManifesto(manifesto: String)

    /** Включать личные ориентиры в отклик ИИ-ментора на экране «Сегодня». */
    val mentorIncludeManifesto: Flow<Boolean>

    /** Включать личные ориентиры в «Зеркало недели». */
    val weeklyIncludeManifesto: Flow<Boolean>

    /** Учитывать личные ориентиры в промптах чата. */
    val chatManifestoContextEnabled: Flow<Boolean>

    suspend fun setMentorIncludeManifesto(enabled: Boolean)
    suspend fun setWeeklyIncludeManifesto(enabled: Boolean)
    suspend fun setChatManifestoContextEnabled(enabled: Boolean)

    /** Время последнего скрытия TimeEcho (запись год назад) */
    val lastTimeEchoDismissedAt: Flow<Long?>
    suspend fun markTimeEchoDismissed(timestamp: Long)

    /** Пользовательское поле на экране «Сегодня». */
    val customJournalFieldEnabled: Flow<Boolean>
    val customJournalFieldQuestion: Flow<String>
    val customJournalFieldHint: Flow<String>

    suspend fun setCustomJournalFieldEnabled(enabled: Boolean)
    suspend fun setCustomJournalFieldQuestion(question: String)
    suspend fun setCustomJournalFieldHint(hint: String)

    /** Настройки практики «Песочный поток» (Sand Flow). */
    val sandFlowEnabled: Flow<Boolean>
    val sandFlowBreathingSyncEnabled: Flow<Boolean>
    val sandFlowDifficulty: Flow<Int> // 50 (легко), 80 (нормально), 100 (глубоко)

    suspend fun setSandFlowEnabled(enabled: Boolean)
    suspend fun setSandFlowBreathingSyncEnabled(enabled: Boolean)
    suspend fun setSandFlowDifficulty(difficulty: Int)

    /** Показывать карточку «Дыхательный мост» на экране «Сегодня». */
    val breathingBridgeEnabled: Flow<Boolean>
    suspend fun setBreathingBridgeEnabled(enabled: Boolean)

    /** Фоновая ambient-музыка, пока приложение на переднем плане. */
    val ambientMusicEnabled: Flow<Boolean>
    val ambientMusicPausedByUser: Flow<Boolean>
    val ambientMusicVolumePercent: Flow<Int>
    val ambientMusicSelectedTrackId: Flow<String>
    val ambientMusicCustomTracksJson: Flow<String>

    suspend fun setAmbientMusicEnabled(enabled: Boolean)
    suspend fun setAmbientMusicPausedByUser(paused: Boolean)
    suspend fun setAmbientMusicVolumePercent(percent: Int)
    suspend fun setAmbientMusicSelectedTrackId(trackId: String)
    suspend fun setAmbientMusicCustomTracksJson(json: String)

    /** Режим дыхательного моста на экране «Сегодня». */
    val breathingPattern: Flow<BreathingPattern>
    val breathingHapticEnabled: Flow<Boolean>
    val breathingHapticIntensity: Flow<BreathingHapticIntensity>
    /** Число циклов за сессию (3–8). */
    val breathingCycleCount: Flow<Int>

    suspend fun setBreathingPattern(pattern: BreathingPattern)
    suspend fun setBreathingHapticEnabled(enabled: Boolean)
    suspend fun setBreathingHapticIntensity(intensity: BreathingHapticIntensity)
    suspend fun setBreathingCycleCount(count: Int)

    /** Включен ли тактильный отклик интерфейса (выбор тегов, кнопки). */
    val uiHapticEnabled: Flow<Boolean>
    suspend fun setUiHapticEnabled(enabled: Boolean)

    /**
     * Прогревать локальную Gemma при запуске приложения (WorkManager + bootstrap UI).
     * По умолчанию `false` — модель загружается при первом AI-запросе и освобождается
     * при сворачивании приложения.
     */
    val warmupOnLaunchEnabled: Flow<Boolean>
    suspend fun setWarmupOnLaunchEnabled(enabled: Boolean)

    // --- Вкладка «Картина» (Insights) ---

    /** 30 или 90 дней — окно сводки и закономерностей. */
    val insightsWindowDays: Flow<Int>
    suspend fun setInsightsWindowDays(days: Int)

    val insightsTabEverOpened: Flow<Boolean>
    val insightsTabLastOpenedAtMs: Flow<Long?>
    val insightsBannerLastShownMs: Flow<Long?>

    suspend fun markInsightsTabOpened(timestampMs: Long)
    suspend fun markInsightsBannerShown(timestampMs: Long)

    /** Первый запуск на en*-устройстве → сохранить [AppLanguage.EN]. */
    suspend fun ensureFirstRunLanguageBootstrap()
}

/**
 * Дискретный набор интервалов auto-lock.
 *
 * Закрытый список — намеренный выбор: continuous-slider в этом разделе
 * провоцирует «оптимизацию» (а это про безопасность, а не про производительность),
 * и пользователю проще выбирать из понятных пресетов.
 *
 * [IMMEDIATELY] — любой уход в background сразу требует разблокировки.
 * [ONE_MINUTE] — дефолт по плану фазы A.
 */
enum class AutoLockTimeout(val millis: Long) {
    IMMEDIATELY(0L),
    THIRTY_SECONDS(30_000L),
    ONE_MINUTE(60_000L),
    FIVE_MINUTES(300_000L);

    companion object {
        val DEFAULT: AutoLockTimeout = ONE_MINUTE

        /** Безопасная десериализация: незнакомое значение → дефолт. */
        fun fromMillis(raw: Long?): AutoLockTimeout =
            entries.firstOrNull { it.millis == raw } ?: DEFAULT
    }
}
