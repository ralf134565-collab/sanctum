// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.insights.domain

import com.pocketreflect.insights.model.InsightMoodTag

enum class InsightPatternType {
    CO_OCCURRENCE,
    SEQUENCE,
    STREAK,
    RECOVERY_MICRO_WINS,
    DOMINANT_TAG,
    WEEKEND_AFFINITY,
    WEEKDAY_AFFINITY,
}

data class InsightPattern(
    val id: String,
    val type: InsightPatternType,
    val support: Int,
    val base: Int,
    val entryIds: List<Long>,
    val involvedTags: Set<InsightMoodTag>,
    val score: Float,
    /** Для streak / sequence — опциональные метаданные. */
    val fromDayBucket: String? = null,
    val toDayBucket: String? = null,
    val tag: InsightMoodTag? = null,
    val tagA: InsightMoodTag? = null,
    val tagB: InsightMoodTag? = null,
) {
    fun involves(tag: InsightMoodTag): Boolean = tag in involvedTags
}
