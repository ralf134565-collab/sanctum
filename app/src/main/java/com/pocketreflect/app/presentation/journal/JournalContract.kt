// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.journal

import androidx.compose.runtime.Immutable
import com.pocketreflect.app.core.work.WeeklySummaryPolicy
import com.pocketreflect.app.domain.ai.AiEngineStatus
import com.pocketreflect.app.domain.breathing.BreathingHapticIntensity
import com.pocketreflect.app.domain.breathing.BreathingPattern
import com.pocketreflect.app.domain.breathing.BreathingSessionController
import com.pocketreflect.app.domain.model.MoodTag
import com.pocketreflect.app.domain.model.hasNegative

/**
 * Контракт MVI для экрана «Итоги дня».
 */
object JournalContract {

    /** Лимит строк в блоке «Фокус на завтра» (борьба с эффектом Зейгарник). */
    const val MAX_TOMORROW_TASK_LINES = 3

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val bootstrapFailed: Boolean = false,
        val isSaving: Boolean = false,
        val isSaved: Boolean = false,
        val saveFailed: Boolean = false,

        /** Ключ выбранного дня YYYY-MM-DD. */
        val selectedDayBucket: String = "",

        /** Выбран не сегодняшний день — режим дописывания прошлого. */
        val isEditingPastDay: Boolean = false,

        /** За выбранный день уже есть сохранённая запись. */
        val wasSavedForDay: Boolean = false,

        val selectedTags: Set<MoodTag> = emptySet(),
        val microWins: String = "",
        val tomorrowTasks: String = "",
        val customFieldAnswer: String = "",
        val customFieldEnabled: Boolean = false,
        val customFieldQuestion: String = "",
        val customFieldHint: String = "",
        val reflection: String = "",
        val dailyPrompt: String = "",
        val aiResponse: String? = null,
        val isAiThinking: Boolean = false,
        val availableTags: List<MoodTag> = MoodTag.orderedForUi,
        val aiEngineStatus: AiEngineStatus = AiEngineStatus.MODEL_OFFLINE,
        val weeklyTrendSummary: String? = null,
        val isWeeklyTrendBuilding: Boolean = false,
        /** Сохранённых записей за последние 7 дней — для кнопки недельной картины. */
        val recentEntriesInWeeklyWindow: Int = 0,
        val timeEcho: com.pocketreflect.app.domain.timeecho.TimeEchoPolicy.Echo? = null,
        val ritualMode: com.pocketreflect.app.domain.ritual.RitualMode = com.pocketreflect.app.domain.ritual.RitualMode.FULL,
        val isShortRitualOverridden: Boolean = false,
        val breathingPattern: BreathingPattern = BreathingPattern.DEFAULT,
        val breathingHapticEnabled: Boolean = true,
        val breathingHapticIntensity: BreathingHapticIntensity = BreathingHapticIntensity.DEFAULT,
        val breathingCycleCount: Int = BreathingSessionController.defaultCycleCount(BreathingPattern.DEFAULT),
        val uiHapticEnabled: Boolean = true,

        val sandFlowEnabled: Boolean = true,
        val sandFlowBreathingSyncEnabled: Boolean = true,
        val sandFlowDifficulty: Int = 80,
    ) {

        val isMicroWinsHidden: Boolean
            get() = selectedTags.hasNegative

        val isSupportiveModeActive: Boolean
            get() = selectedTags.hasNegative

        val canSave: Boolean
            get() = selectedTags.isNotEmpty() && !isSaving

        val canRequestAiReflection: Boolean
            get() = selectedTags.isNotEmpty() && !isAiThinking

        val hasAiResponse: Boolean
            get() = !aiResponse.isNullOrBlank()

        val showSaveHintNoTags: Boolean
            get() = !isSaving && selectedTags.isEmpty()

        val tomorrowTaskLineCount: Int
            get() = if (tomorrowTasks.isEmpty()) 0
            else tomorrowTasks.lineSequence().count()

        val isTomorrowTasksLimitExceeded: Boolean
            get() = tomorrowTaskLineCount > MAX_TOMORROW_TASK_LINES

        val showCustomField: Boolean
            get() = customFieldEnabled && customFieldQuestion.isNotBlank()

        val hasWeeklyTrendSummary: Boolean
            get() = !weeklyTrendSummary.isNullOrBlank()

        val canRequestWeeklyTrend: Boolean
            get() = recentEntriesInWeeklyWindow >= WeeklySummaryPolicy.MIN_ENTRIES_FOR_SUMMARY &&
                !isWeeklyTrendBuilding
    }

    sealed interface Intent {
        data class ToggleTag(val tag: MoodTag) : Intent
        data class UpdateMicroWins(val value: String) : Intent
        data class UpdateTomorrowTasks(val value: String) : Intent
        data class UpdateCustomField(val value: String) : Intent
        data class UpdateReflection(val value: String) : Intent
        data class SelectDay(val dayBucket: String) : Intent
        data object ReshufflePrompt : Intent
        data object SaveDay : Intent
        data object RetrySave : Intent
        data object RequestAiReflection : Intent
        data object RequestWeeklyTrend : Intent
        data object DismissSavedState : Intent
        data object DismissTimeEcho : Intent
        data object ExpandFullRitual : Intent
        data object RetryBootstrap : Intent
    }

    sealed interface Effect {
        data class ShowError(val message: String) : Effect
        data object DaySaved : Effect
        data object ScrollToTop : Effect
    }
}
