// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.history

import androidx.compose.runtime.Immutable
import com.pocketreflect.app.core.locale.AppLanguage
import com.pocketreflect.app.data.local.entity.JournalEntry
import com.pocketreflect.app.domain.history.TagFrequency
import java.time.YearMonth

/**
 * MVI-контракт экрана «История».
 *
 * Группировка по месяцам выполняется в ViewModel из «сырого» Flow,
 * а в State лежит уже подготовленная для рендера структура. Это снижает
 * нагрузку на Compose-рекомпозицию (не группируем заново при каждой подписке).
 */
object HistoryContract {

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val loadFailed: Boolean = false,
        val grouped: List<MonthGroup> = emptyList(),
        val personalManifesto: String = "",
        val mentorIncludeManifesto: Boolean = false,
        val weeklyIncludeManifesto: Boolean = false,
        val searchQuery: String = "",
        val searchFilterQuery: String = "",
        val isSearchActive: Boolean = false,
        val searchResultCount: Int = 0,
        val contentLanguage: AppLanguage = AppLanguage.DEFAULT,
    ) {
        val isEmpty: Boolean get() = !isLoading && !loadFailed && grouped.isEmpty() && !isSearchActive
        val isSearchEmpty: Boolean get() = isSearchActive && searchResultCount == 0
    }

    @Immutable
    data class MonthGroup(
        val yearMonth: YearMonth,
        val title: String,
        val entries: List<JournalEntry>,
        val topTags: List<TagFrequency> = emptyList(),
    )

    sealed interface Intent {
        data class OpenEntry(val id: Long) : Intent
        data class UpdatePersonalManifesto(val manifesto: String) : Intent
        data class SetMentorIncludeManifesto(val enabled: Boolean) : Intent
        data class SetWeeklyIncludeManifesto(val enabled: Boolean) : Intent
        data class UpdateSearchQuery(val query: String) : Intent
        data object ClearSearch : Intent
        data object RetryLoad : Intent
    }

    sealed interface Effect {
        data class NavigateToDetail(val id: Long) : Effect
    }
}
