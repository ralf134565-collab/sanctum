// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.insights

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketreflect.app.core.locale.AppLanguageResolver
import com.pocketreflect.app.core.security.DatabaseAccess
import com.pocketreflect.app.core.time.Clock
import com.pocketreflect.app.data.repository.JournalRepository
import com.pocketreflect.app.data.repository.UserPreferencesRepository
import com.pocketreflect.app.domain.insights.toInsightEntry
import com.pocketreflect.insights.domain.InsightPatternFormatter
import com.pocketreflect.insights.domain.InsightPolicy
import com.pocketreflect.insights.domain.InsightsSnapshotBuilder
import com.pocketreflect.insights.ui.InsightDaysSheetRequest
import com.pocketreflect.insights.ui.polaritySheetTitle
import com.pocketreflect.insights.ui.tagSheetTitle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val journalRepository: JournalRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val appLanguageResolver: AppLanguageResolver,
    private val databaseAccess: DatabaseAccess,
    private val clock: Clock,
) : ViewModel() {

    private val reloadSignal = MutableStateFlow(0)
    private val _state = MutableStateFlow(InsightsContract.State())
    val state: StateFlow<InsightsContract.State> = _state.asStateFlow()

    private val _effects = Channel<InsightsContract.Effect>(capacity = Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.markInsightsTabOpened(clock.nowMillis())
        }
        viewModelScope.launch {
            var wasReady = false
            databaseAccess.isReady.collect { ready ->
                if (ready && !wasReady) {
                    reloadSignal.update { it + 1 }
                    journalRepository.observeHistory().collect {
                        reloadSignal.update { it + 1 }
                    }
                }
                wasReady = ready
            }
        }
        viewModelScope.launch {
            combine(
                reloadSignal,
                userPreferencesRepository.insightsWindowDays.distinctUntilChanged(),
                appLanguageResolver.resolved,
            ) { _, windowDays, language ->
                windowDays to language.isEnglish
            }.collect { (windowDays, english) ->
                reload(windowDays, english)
            }
        }
    }

    fun onIntent(intent: InsightsContract.Intent) {
        when (intent) {
            is InsightsContract.Intent.SetWindowDays -> {
                viewModelScope.launch {
                    userPreferencesRepository.setInsightsWindowDays(intent.days)
                    _state.update {
                        it.copy(
                            expandedCards = false,
                            highlightedTag = null,
                            highlightedPatternId = null,
                            sheetRequest = null,
                        )
                    }
                }
            }
            InsightsContract.Intent.ExpandCards ->
                _state.update { it.copy(expandedCards = true) }
            is InsightsContract.Intent.PatternClick -> onPatternClick(intent.patternId)
            is InsightsContract.Intent.TagClick -> onTagClick(intent.tag)
            is InsightsContract.Intent.PolarityClick -> onPolarityClick(intent.polarity)
            InsightsContract.Intent.DismissSheet ->
                _state.update { it.copy(sheetRequest = null) }
        }
    }

    fun onOpenEntry(id: Long) {
        _state.update { it.copy(sheetRequest = null) }
        _effects.trySend(InsightsContract.Effect.OpenEntry(id))
    }

    private fun onPatternClick(id: String) {
        val snapshot = _state.value.snapshot ?: return
        val english = _state.value.english
        val toggled = if (_state.value.highlightedPatternId == id) null else id
        _state.update {
            it.copy(
                highlightedPatternId = toggled,
                highlightedTag = null,
            )
        }
        if (toggled == null) return
        val pattern = snapshot.patterns.firstOrNull { it.id == id } ?: return
        _state.update {
            it.copy(
                sheetRequest = InsightDaysSheetRequest(
                    title = InsightPatternFormatter.format(pattern, english).title,
                    entryIds = pattern.entryIds,
                ),
            )
        }
    }

    private fun onTagClick(tag: com.pocketreflect.insights.model.InsightMoodTag) {
        val snapshot = _state.value.snapshot ?: return
        val english = _state.value.english
        val toggled = if (_state.value.highlightedTag == tag) null else tag
        _state.update {
            it.copy(
                highlightedTag = toggled,
                highlightedPatternId = null,
            )
        }
        if (toggled == null) return
        val ids = snapshot.entries.filter { tag in it.moodTags }.map { it.id }
        _state.update {
            it.copy(
                sheetRequest = InsightDaysSheetRequest(
                    title = tagSheetTitle(tag, english),
                    entryIds = ids,
                ),
            )
        }
    }

    private fun onPolarityClick(polarity: com.pocketreflect.insights.model.InsightMoodTag.Polarity) {
        val snapshot = _state.value.snapshot ?: return
        val english = _state.value.english
        _state.update {
            it.copy(highlightedTag = null, highlightedPatternId = null)
        }
        val ids = snapshot.entries
            .filter { e -> e.moodTags.any { it.polarity == polarity } }
            .map { it.id }
        _state.update {
            it.copy(
                sheetRequest = InsightDaysSheetRequest(
                    title = polaritySheetTitle(polarity, english),
                    entryIds = ids,
                ),
            )
        }
    }

    private suspend fun reload(windowDays: Int, english: Boolean) {
        _state.update { it.copy(isLoading = true, english = english) }
        try {
            val entries = withContext(Dispatchers.Default) {
                journalRepository.entriesForLastDays(windowDays).map { it.toInsightEntry() }
            }
            val snapshot = withContext(Dispatchers.Default) {
                InsightsSnapshotBuilder.build(entries, windowDays, english)
            }
            _state.update {
                it.copy(
                    isLoading = false,
                    snapshot = snapshot,
                )
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Insights reload failed", t)
            _state.update { it.copy(isLoading = false, snapshot = null) }
        }
    }

    private companion object {
        const val TAG = "InsightsViewModel"
    }
}
