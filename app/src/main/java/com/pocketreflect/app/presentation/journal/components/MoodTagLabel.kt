// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.journal.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pocketreflect.app.R
import com.pocketreflect.app.domain.model.MoodTag

@Composable
fun MoodTag.displayLabel(): String = when (this) {
    MoodTag.CALM -> stringResource(R.string.mood_calm)
    MoodTag.JOY -> stringResource(R.string.mood_joy)
    MoodTag.GRATITUDE -> stringResource(R.string.mood_gratitude)
    MoodTag.FOCUSED -> stringResource(R.string.mood_focused)
    MoodTag.TIRED -> stringResource(R.string.mood_tired)
    MoodTag.OVERWHELMED -> stringResource(R.string.mood_overwhelmed)
    MoodTag.ANXIETY -> stringResource(R.string.mood_anxiety)
    MoodTag.SADNESS -> stringResource(R.string.mood_sadness)
    MoodTag.IRRITATION -> stringResource(R.string.mood_irritation)
}
