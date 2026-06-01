// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.insights

import androidx.compose.runtime.Immutable
import com.pocketreflect.insights.domain.InsightSnapshot
import com.pocketreflect.insights.model.InsightMoodTag
import com.pocketreflect.insights.ui.InsightDaysSheetRequest

object InsightsContract {

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val snapshot: InsightSnapshot? = null,
        val english: Boolean = false,
        val expandedCards: Boolean = false,
        val highlightedTag: InsightMoodTag? = null,
        val highlightedPatternId: String? = null,
        val sheetRequest: InsightDaysSheetRequest? = null,
    )

    sealed interface Intent {
        data class SetWindowDays(val days: Int) : Intent
        data object ExpandCards : Intent
        data class PatternClick(val patternId: String) : Intent
        data class TagClick(val tag: InsightMoodTag) : Intent
        data class PolarityClick(val polarity: InsightMoodTag.Polarity) : Intent
        data object DismissSheet : Intent
    }

    sealed interface Effect {
        data class OpenEntry(val id: Long) : Effect
    }
}
