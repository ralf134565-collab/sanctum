// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.history

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketreflect.app.core.locale.AppLanguage
import com.pocketreflect.app.core.locale.AppLanguageResolver
import com.pocketreflect.app.core.time.DateFormats
import com.pocketreflect.app.data.local.entity.JournalEntry
import com.pocketreflect.app.data.repository.JournalRepository
import com.pocketreflect.app.data.repository.UserPreferencesRepository
import com.pocketreflect.app.domain.history.JournalSearchMatcher
import com.pocketreflect.app.domain.history.MonthTagFrequency
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel «истории». Подписывается на репозиторий через `stateIn`
 * с `WhileSubscribed(5_000)` — это стандартная защита от лишних подписок
 * на rotate / возврат с другого экрана.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val journalRepository: JournalRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val appLanguageResolver: AppLanguageResolver,
) : ViewModel() {

    private val reloadSignal = MutableStateFlow(0)
    private val searchQuery = MutableStateFlow("")
    private val debouncedSearchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            searchQuery
                .debounce(SEARCH_DEBOUNCE_MS)
                .collect { debouncedSearchQuery.value = it }
        }
    }

    val state: StateFlow<HistoryContract.State> = reloadSignal
        .flatMapLatest {
            combine(
                journalRepository.observeHistory(),
                userPreferencesRepository.personalManifesto,
                userPreferencesRepository.mentorIncludeManifesto,
                userPreferencesRepository.weeklyIncludeManifesto,
                appLanguageResolver.resolved,
                searchQuery,
                debouncedSearchQuery,
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                val entries = values[0] as List<JournalEntry>
                val manifesto = values[1] as String
                val mentorOn = values[2] as Boolean
                val weeklyOn = values[3] as Boolean
                val language = values[4] as AppLanguage
                val rawQuery = values[5] as String
                val filterQuery = values[6] as String
                buildState(
                    entries = entries,
                    manifesto = manifesto,
                    mentorIncludeManifesto = mentorOn,
                    weeklyIncludeManifesto = weeklyOn,
                    language = language,
                    searchQuery = rawQuery,
                    filterQuery = filterQuery,
                )
            }
                .catch { error ->
                    Log.e(TAG, "History load failed", error)
                    emit(
                        HistoryContract.State(
                            isLoading = false,
                            loadFailed = true,
                        ),
                    )
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = HistoryContract.State(isLoading = true),
        )

    private val _effects = Channel<HistoryContract.Effect>(capacity = Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onIntent(intent: HistoryContract.Intent) {
        when (intent) {
            is HistoryContract.Intent.OpenEntry ->
                _effects.trySend(HistoryContract.Effect.NavigateToDetail(intent.id))
            is HistoryContract.Intent.UpdatePersonalManifesto -> {
                viewModelScope.launch {
                    userPreferencesRepository.setPersonalManifesto(intent.manifesto)
                }
            }
            is HistoryContract.Intent.SetMentorIncludeManifesto -> {
                viewModelScope.launch {
                    userPreferencesRepository.setMentorIncludeManifesto(intent.enabled)
                }
            }
            is HistoryContract.Intent.SetWeeklyIncludeManifesto -> {
                viewModelScope.launch {
                    userPreferencesRepository.setWeeklyIncludeManifesto(intent.enabled)
                }
            }
            is HistoryContract.Intent.UpdateSearchQuery ->
                searchQuery.value = intent.query
            HistoryContract.Intent.ClearSearch ->
                searchQuery.value = ""
            HistoryContract.Intent.RetryLoad -> reloadSignal.update { it + 1 }
        }
    }

    private fun buildState(
        entries: List<JournalEntry>,
        manifesto: String,
        mentorIncludeManifesto: Boolean,
        weeklyIncludeManifesto: Boolean,
        language: AppLanguage,
        searchQuery: String,
        filterQuery: String,
    ): HistoryContract.State {
        val isSearchActive = JournalSearchMatcher.isActiveQuery(filterQuery)
        val filtered = if (isSearchActive) {
            entries.filter { JournalSearchMatcher.matches(it, filterQuery, language) }
        } else {
            entries
        }

        val locale = DateFormats.javaLocale(language)
        val groups = filtered
            .groupBy { DateFormats.yearMonthOf(it.timestamp) }
            .entries
            .sortedByDescending { it.key }
            .map { (ym, list) ->
                val sorted = list.sortedByDescending { it.timestamp }
                HistoryContract.MonthGroup(
                    yearMonth = ym,
                    title = DateFormats.monthHeader(ym, locale),
                    entries = sorted,
                    topTags = MonthTagFrequency.summarize(sorted),
                )
            }

        return HistoryContract.State(
            isLoading = false,
            loadFailed = false,
            grouped = groups,
            personalManifesto = manifesto,
            mentorIncludeManifesto = mentorIncludeManifesto,
            weeklyIncludeManifesto = weeklyIncludeManifesto,
            searchQuery = searchQuery,
            searchFilterQuery = if (isSearchActive) filterQuery else "",
            isSearchActive = isSearchActive,
            searchResultCount = filtered.size,
            contentLanguage = language,
        )
    }

    private companion object {
        const val TAG = "HistoryViewModel"
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
