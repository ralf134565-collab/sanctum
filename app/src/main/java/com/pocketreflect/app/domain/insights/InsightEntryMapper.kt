// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.insights

import com.pocketreflect.app.data.local.entity.JournalEntry
import com.pocketreflect.app.domain.model.MoodTag
import com.pocketreflect.insights.model.InsightEntry
import com.pocketreflect.insights.model.InsightMoodTag

fun JournalEntry.toInsightEntry(): InsightEntry = InsightEntry(
    id = id,
    dayBucket = dayBucket,
    moodTags = moodTags.map { it.toInsightMoodTag() },
    microWins = microWins,
    reflection = reflection,
)

fun MoodTag.toInsightMoodTag(): InsightMoodTag =
    InsightMoodTag.entries.first { it.storageKey == storageKey }
