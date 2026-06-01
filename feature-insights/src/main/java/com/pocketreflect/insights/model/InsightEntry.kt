// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.insights.model

/**
 * Минимальная запись дневника для insights (без Room).
 */
data class InsightEntry(
    val id: Long,
    val dayBucket: String,
    val moodTags: List<InsightMoodTag>,
    val microWins: String = "",
    val reflection: String = "",
)
