// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.journal

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.pocketreflect.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.lifecycle.viewModelScope
import com.pocketreflect.app.core.audio.AmbientMusicController
import com.pocketreflect.app.core.locale.AppLanguageResolver
import com.pocketreflect.app.core.security.DatabaseAccess
import com.pocketreflect.app.core.time.Clock
import com.pocketreflect.app.core.time.DayBucket
import com.pocketreflect.app.core.work.WeeklySummaryPolicy
import com.pocketreflect.app.data.local.entity.AITrendProfile
import com.pocketreflect.app.data.local.entity.JournalEntry
import com.pocketreflect.app.data.repository.DailyPromptsHistoryRepository
import com.pocketreflect.app.data.repository.JournalRepository
import com.pocketreflect.app.data.repository.UserPreferencesRepository
import com.pocketreflect.app.domain.insights.InsightsBannerPolicy
import com.pocketreflect.app.domain.ritual.RitualMode
import com.pocketreflect.app.domain.timeecho.TimeEchoPolicy
import com.pocketreflect.insights.domain.InsightPolicy
import java.time.Instant
import java.time.ZoneId
import com.pocketreflect.app.domain.ai.AiEngineStatusSource
import com.pocketreflect.app.domain.ai.GemmaLocalEngine
import com.pocketreflect.app.domain.model.MoodTag
import com.pocketreflect.app.domain.prompts.DailyPrompts
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds
import javax.inject.Inject

@HiltViewModel
class JournalViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val journalRepository: JournalRepository,
    private val gemmaEngine: GemmaLocalEngine,
    private val clock: Clock,
    private val promptsHistory: DailyPromptsHistoryRepository,
    private val aiEngineStatusSource: AiEngineStatusSource,
    private val appLanguageResolver: AppLanguageResolver,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val databaseAccess: DatabaseAccess,
    private val ambientMusicController: AmbientMusicController,
) : ViewModel() {

    private val _state = MutableStateFlow(JournalContract.State())
    val state: StateFlow<JournalContract.State> = _state.asStateFlow()

    private val _effects = Channel<JournalContract.Effect>(capacity = Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var aiJob: Job? = null
    private var weeklyTrendJob: Job? = null

    init {
        bootstrap()
        refreshWeeklyEntryCount()
        viewModelScope.launch {
            aiEngineStatusSource.status.collect { status ->
                _state.update { it.copy(aiEngineStatus = status) }
            }
        }
        viewModelScope.launch {
            journalRepository.observeTrendProfiles().collect { profiles ->
                _state.update { it.copy(weeklyTrendSummary = resolveWeeklySummary(profiles)) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.customJournalFieldEnabled.collect { enabled ->
                _state.update { it.copy(customFieldEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.customJournalFieldQuestion.collect { question ->
                _state.update { it.copy(customFieldQuestion = question.trim()) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.customJournalFieldHint.collect { hint ->
                _state.update { it.copy(customFieldHint = hint.trim()) }
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
            userPreferencesRepository.uiHapticEnabled.collect { enabled ->
                _state.update { it.copy(uiHapticEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.breathingBridgeEnabled.collect { enabled ->
                _state.update { it.copy(breathingBridgeEnabled = enabled) }
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
            ambientMusicController.uiState.collect { ambient ->
                _state.update { it.copy(ambientMusic = ambient) }
            }
        }
        viewModelScope.launch {
            var wasReady = false
            databaseAccess.isReady.collect { ready ->
                if (ready && !wasReady) {
                    val current = _state.value
                    if (current.bootstrapFailed) {
                        val day = current.selectedDayBucket.takeIf { it.isNotBlank() } ?: clock.today()
                        loadDay(day)
                    }
                }
                wasReady = ready
            }
        }
        observeInsightsBanner()
    }

    private fun observeInsightsBanner() {
        viewModelScope.launch {
            combine(
                journalRepository.observeHistory(),
                userPreferencesRepository.insightsTabEverOpened,
                userPreferencesRepository.insightsTabLastOpenedAtMs,
                userPreferencesRepository.insightsBannerLastShownMs,
                _state.map { snapshot ->
                    snapshot.ritualMode == RitualMode.SHORT && !snapshot.isShortRitualOverridden
                }.distinctUntilChanged(),
            ) { history, tabEverOpened, tabLastOpenedAtMs, bannerLastShownMs, shortRitual ->
                val fromBucket = DayBucket.fromLocalDate(
                    DayBucket.toLocalDate(DayBucket.today())
                        .minusDays((InsightPolicy.WINDOW_30_DAYS - 1).toLong()),
                )
                val entriesLast30 = history.count { it.dayBucket >= fromBucket }
                InsightsBannerPolicy.shouldShow(
                    entriesLast30Days = entriesLast30,
                    tabEverOpened = tabEverOpened,
                    tabLastOpenedAtMs = tabLastOpenedAtMs,
                    bannerLastShownMs = bannerLastShownMs,
                    nowMs = clock.nowMillis(),
                    isShortRitualActive = shortRitual,
                )
            }.collect { show ->
                _state.update { it.copy(showInsightsBanner = show) }
            }
        }
    }

    private fun resolveWeeklySummary(
        profiles: List<com.pocketreflect.app.data.local.entity.AITrendProfile>,
    ): String? {
        val latest = profiles
            .asSequence()
            .filter { it.summary.isNotBlank() }
            .maxByOrNull { it.generatedAt }
            ?: return null
        val age = clock.nowMillis() - latest.generatedAt
        if (age > WeeklySummaryPolicy.DISPLAY_PROFILE_MAX_AGE_MS) return null
        return latest.summary
    }

    fun onIntent(intent: JournalContract.Intent) {
        when (intent) {
            is JournalContract.Intent.ToggleTag -> handleToggleTag(intent)
            is JournalContract.Intent.UpdateMicroWins -> _state.update { it.copy(microWins = intent.value) }
            is JournalContract.Intent.UpdateTomorrowTasks -> handleUpdateTomorrowTasks(intent)
            is JournalContract.Intent.UpdateCustomField -> handleUpdateCustomField(intent)
            is JournalContract.Intent.UpdateReflection -> _state.update { it.copy(reflection = intent.value) }
            is JournalContract.Intent.SelectDay -> loadDay(intent.dayBucket)
            JournalContract.Intent.ReshufflePrompt -> handleReshufflePrompt()
            JournalContract.Intent.SaveDay -> handleSaveDay()
            JournalContract.Intent.RetrySave -> handleSaveDay()
            JournalContract.Intent.RequestAiReflection -> launchAiReflection()
            JournalContract.Intent.RequestWeeklyTrend -> launchWeeklyTrend()
            JournalContract.Intent.DismissSavedState -> _state.update { it.copy(isSaved = false) }
            JournalContract.Intent.DismissTimeEcho -> handleDismissTimeEcho()
            JournalContract.Intent.ExpandFullRitual -> _state.update { it.copy(isShortRitualOverridden = true) }
            JournalContract.Intent.RetryBootstrap -> {
                val dayToRetry = _state.value.selectedDayBucket.takeIf { it.isNotBlank() } ?: clock.today()
                loadDay(dayToRetry)
            }
            JournalContract.Intent.OpenInsights -> handleOpenInsights()
            JournalContract.Intent.DismissInsightsBanner -> dismissInsightsBanner()
            JournalContract.Intent.ToggleAmbientMusicPlayPause ->
                ambientMusicController.togglePlayPause()
            JournalContract.Intent.AmbientMusicSkipNext ->
                ambientMusicController.skipNext()
            JournalContract.Intent.AmbientMusicSkipPrevious ->
                ambientMusicController.skipPrevious()
            is JournalContract.Intent.SetAmbientMusicVolume ->
                ambientMusicController.setVolume(intent.volume)
            is JournalContract.Intent.SelectAmbientTrack ->
                ambientMusicController.selectTrack(intent.trackId)
        }
    }

    private fun handleOpenInsights() {
        dismissInsightsBanner()
        _effects.trySend(JournalContract.Effect.NavigateToInsights)
    }

    private fun dismissInsightsBanner() {
        viewModelScope.launch {
            userPreferencesRepository.markInsightsBannerShown(clock.nowMillis())
        }
        _state.update { it.copy(showInsightsBanner = false) }
    }

    private fun bootstrap() {
        loadDay(clock.today())
    }

    private fun loadDay(dayBucket: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val todayBucket = clock.today()
            val isToday = dayBucket == todayBucket
            _state.update {
                it.copy(
                    isLoading = true,
                    bootstrapFailed = false,
                    selectedDayBucket = dayBucket,
                    isEditingPastDay = !isToday,
                )
            }
            try {
                val existing = journalRepository.findByDay(dayBucket)
                val initialPrompt = if (existing != null) {
                    existing.promptShown
                } else {
                    val history = promptsHistory.recent.first().toSet()
                    val language = appLanguageResolver.resolvedNow()
                    val chosen = DailyPrompts.random(language = language, history = history)
                    promptsHistory.push(chosen)
                    chosen
                }

                val timeEcho = if (isToday) {
                    val todayLocalDate = Instant.ofEpochMilli(clock.nowMillis())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    val lastDismissed = userPreferencesRepository.lastTimeEchoDismissedAt.first()
                    TimeEchoPolicy.findEcho(
                        today = todayLocalDate,
                        repository = journalRepository,
                        lastDismissedAtMs = lastDismissed,
                        currentTimeMs = clock.nowMillis(),
                    )
                } else {
                    null
                }

                val ritual = if (isToday) {
                    val lastThree = journalRepository.findLastNEntries(3)
                    val todayLocalDate = DayBucket.toLocalDate(todayBucket)
                    com.pocketreflect.app.domain.ritual.ShortEveningPolicy.compute(
                        lastEntries = lastThree,
                        today = todayLocalDate,
                    )
                } else {
                    com.pocketreflect.app.domain.ritual.RitualMode.FULL
                }

                _state.update {
                    it.copy(
                        isLoading = false,
                        bootstrapFailed = false,
                        saveFailed = false,
                        selectedTags = existing?.moodTags?.toSet() ?: emptySet(),
                        microWins = existing?.microWins ?: "",
                        tomorrowTasks = existing?.tomorrowTasks ?: "",
                        customFieldAnswer = existing?.customFieldAnswer ?: "",
                        reflection = existing?.reflection ?: "",
                        dailyPrompt = initialPrompt,
                        aiResponse = existing?.aiReflection,
                        wasSavedForDay = existing != null,
                        timeEcho = timeEcho,
                        ritualMode = ritual,
                        isShortRitualOverridden = false,
                        isSaved = false,
                    )
                }
                refreshWeeklyEntryCount()
            } catch (t: Throwable) {
                Log.e(TAG, "Journal bootstrap failed for $dayBucket", t)
                _state.update { it.copy(isLoading = false, bootstrapFailed = true) }
                _effects.trySend(
                    JournalContract.Effect.ShowError(
                        appContext.getString(R.string.journal_bootstrap_error),
                    ),
                )
            }
        }
    }

    private fun handleReshufflePrompt() {
        viewModelScope.launch {
            val current = _state.value.dailyPrompt
            val history = promptsHistory.recent.first().toSet() + current
            val language = appLanguageResolver.resolvedNow()
            val selectedTags = _state.value.selectedTags
            val next = DailyPrompts.forContext(
                language = language,
                selectedTags = selectedTags,
                history = history
            )
            promptsHistory.push(next)
            _state.update { it.copy(dailyPrompt = next) }
        }
    }

    private fun handleToggleTag(intent: JournalContract.Intent.ToggleTag) {
        _state.update { current ->
            val nextTags = current.selectedTags.toMutableSet().apply {
                if (!add(intent.tag)) remove(intent.tag)
            }
            val cleanedWins = if (nextTags.any { it.polarity == MoodTag.Polarity.NEGATIVE }) {
                ""
            } else {
                current.microWins
            }
            current.copy(
                selectedTags = nextTags,
                microWins = cleanedWins,
                aiResponse = null,
                isAiThinking = false,
            )
        }

        // Recalculate prompt dynamically if all input fields are completely empty
        val currentSnapshot = _state.value
        val isReflectionEmpty = currentSnapshot.reflection.isBlank() &&
                currentSnapshot.tomorrowTasks.isBlank() &&
                currentSnapshot.customFieldAnswer.isBlank() &&
                currentSnapshot.microWins.isBlank()

        if (isReflectionEmpty) {
            viewModelScope.launch {
                val history = promptsHistory.recent.first().toSet()
                val language = appLanguageResolver.resolvedNow()
                val nextPrompt = DailyPrompts.forContext(
                    language = language,
                    selectedTags = currentSnapshot.selectedTags,
                    history = history
                )
                if (nextPrompt != currentSnapshot.dailyPrompt) {
                    promptsHistory.push(nextPrompt)
                    _state.update { it.copy(dailyPrompt = nextPrompt) }
                }
            }
        }

        aiJob?.cancel()
    }

    private fun handleUpdateTomorrowTasks(intent: JournalContract.Intent.UpdateTomorrowTasks) {
        _state.update { it.copy(tomorrowTasks = intent.value) }
    }

    private fun handleUpdateCustomField(intent: JournalContract.Intent.UpdateCustomField) {
        _state.update {
            it.copy(
                customFieldAnswer = intent.value,
                aiResponse = null,
                isAiThinking = false,
            )
        }
        aiJob?.cancel()
    }

    private fun customFieldSnapshot(snapshot: JournalContract.State): Pair<String, String> {
        if (!snapshot.customFieldEnabled || snapshot.customFieldQuestion.isBlank()) {
            return "" to ""
        }
        val question = snapshot.customFieldQuestion.trim()
        val answer = snapshot.customFieldAnswer.trim()
        return question to answer
    }

    private fun handleSaveDay() {
        val snapshot = _state.value
        if (!snapshot.canSave) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, saveFailed = false) }
            try {
                withContext(Dispatchers.IO) {
                    val dayBucket = snapshot.selectedDayBucket.ifEmpty { clock.today() }
                    val timestamp = if (dayBucket == clock.today()) {
                        clock.nowMillis()
                    } else {
                        DayBucket.toNoonEpochMillis(dayBucket)
                    }
                    val (customQuestion, customAnswer) = customFieldSnapshot(snapshot)
                    val entry = JournalEntry(
                        timestamp = timestamp,
                        dayBucket = dayBucket,
                        moodTags = snapshot.selectedTags.toList(),
                        microWins = snapshot.microWins.trim(),
                        tomorrowTasks = snapshot.tomorrowTasks.lineSequence()
                            .take(JournalContract.MAX_TOMORROW_TASK_LINES)
                            .joinToString(separator = "\n")
                            .trim(),
                        reflection = snapshot.reflection.trim(),
                        promptShown = snapshot.dailyPrompt,
                        aiReflection = snapshot.aiResponse,
                        customFieldAnswer = customAnswer,
                        customFieldQuestion = customQuestion,
                    )
                    journalRepository.saveEntry(entry)
                }
                _state.update {
                    it.copy(
                        isSaving = false,
                        isSaved = true,
                        wasSavedForDay = true,
                        saveFailed = false,
                    )
                }
                _effects.trySend(JournalContract.Effect.DaySaved)
                refreshWeeklyEntryCount()
            } catch (t: Throwable) {
                Log.e(TAG, "Journal save failed", t)
                _state.update { it.copy(isSaving = false, saveFailed = true) }
                _effects.trySend(
                    JournalContract.Effect.ShowError(appContext.getString(R.string.journal_save_error)),
                )
            }
        }
    }

    private fun launchAiReflection() {
        val current = _state.value
        if (current.selectedTags.isEmpty()) {
            _state.update { it.copy(aiResponse = null, isAiThinking = false) }
            return
        }
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _state.update { it.copy(isAiThinking = true) }
            val dayBucket = current.selectedDayBucket.ifEmpty { clock.today() }
            val (customQuestion, customAnswer) = customFieldSnapshot(current)
            val draftEntry = JournalEntry(
                timestamp = clock.nowMillis(),
                dayBucket = dayBucket,
                moodTags = current.selectedTags.toList(),
                microWins = current.microWins,
                tomorrowTasks = current.tomorrowTasks,
                reflection = current.reflection,
                promptShown = current.dailyPrompt,
                aiReflection = null,
                customFieldAnswer = customAnswer,
                customFieldQuestion = customQuestion,
            )
            val response = runCatching {
                val includeManifesto = userPreferencesRepository.mentorIncludeManifesto.first()
                val manifesto = userPreferencesRepository.personalManifesto.first()
                val manifestoForPrompt = manifesto.takeIf { includeManifesto && it.isNotBlank() }
                gemmaEngine.generatePromptResponse(draftEntry, manifestoForPrompt)
            }
                .getOrElse { "" }
            _state.update {
                it.copy(
                    isAiThinking = false,
                    aiResponse = response.takeIf { r -> r.isNotBlank() } ?: it.aiResponse,
                )
            }
        }
    }

    private fun refreshWeeklyEntryCount() {
        viewModelScope.launch(Dispatchers.IO) {
            val count = journalRepository
                .entriesForLastDays(WeeklySummaryPolicy.SUMMARY_WINDOW_DAYS)
                .size
            _state.update { it.copy(recentEntriesInWeeklyWindow = count) }
        }
    }

    private fun launchWeeklyTrend() {
        if (_state.value.recentEntriesInWeeklyWindow < WeeklySummaryPolicy.MIN_ENTRIES_FOR_SUMMARY) {
            _effects.trySend(
                JournalContract.Effect.ShowError(
                    appContext.getString(R.string.weekly_trend_need_more_entries),
                ),
            )
            return
        }
        weeklyTrendJob?.cancel()
        weeklyTrendJob = viewModelScope.launch {
            _state.update { it.copy(isWeeklyTrendBuilding = true) }
            try {
                val entries = withContext(Dispatchers.IO) {
                    journalRepository.entriesForLastDays(WeeklySummaryPolicy.SUMMARY_WINDOW_DAYS)
                }
                if (entries.size < WeeklySummaryPolicy.MIN_ENTRIES_FOR_SUMMARY) {
                    refreshWeeklyEntryCount()
                    _effects.trySend(
                        JournalContract.Effect.ShowError(
                            appContext.getString(R.string.weekly_trend_need_more_entries),
                        ),
                    )
                    return@launch
                }
                val includeManifesto = userPreferencesRepository.weeklyIncludeManifesto.first()
                val manifesto = userPreferencesRepository.personalManifesto.first()
                val manifestoForPrompt = manifesto.takeIf { includeManifesto && it.isNotBlank() }
                val summary = withContext(Dispatchers.Default) {
                    withTimeout(WEEKLY_TREND_TIMEOUT) {
                        gemmaEngine.summarizeWeek(entries, manifestoForPrompt)
                    }
                }
                val summaryText = summary.humanReadable.trim()
                if (summaryText.isBlank()) {
                    _effects.trySend(
                        JournalContract.Effect.ShowError(
                            appContext.getString(R.string.weekly_trend_build_error),
                        ),
                    )
                    return@launch
                }
                withContext(Dispatchers.IO) {
                    journalRepository.saveTrendProfile(
                        AITrendProfile(
                            periodStart = entries.minOf { it.timestamp },
                            periodEnd = entries.maxOf { it.timestamp },
                            generatedAt = clock.nowMillis(),
                            entryCount = entries.size,
                            summary = summaryText,
                            structuredJson = summary.structuredJson,
                            schemaVersion = 1,
                        ),
                    )
                }
                _state.update { it.copy(weeklyTrendSummary = summaryText) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (t: Throwable) {
                Log.e(TAG, "Weekly trend build failed", t)
                _effects.trySend(
                    JournalContract.Effect.ShowError(
                        appContext.getString(R.string.weekly_trend_build_error),
                    ),
                )
            } finally {
                _state.update { it.copy(isWeeklyTrendBuilding = false) }
            }
        }
    }

    private fun handleDismissTimeEcho() {
        viewModelScope.launch {
            val now = clock.nowMillis()
            userPreferencesRepository.markTimeEchoDismissed(now)
            _state.update { it.copy(timeEcho = null) }
        }
    }

    private companion object {
        const val TAG = "JournalViewModel"
        private val WEEKLY_TREND_TIMEOUT = 120.seconds
    }
}
