// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.insights.domain

import com.pocketreflect.insights.model.InsightEntry
import com.pocketreflect.insights.model.InsightMoodTag
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeekpartPatternTest {

    @Test
    fun weekendOverload_detectsWeekendAffinity() {
        val end = LocalDate.of(2026, 5, 30) // пятница
        val entries = (0 until 28).map { offset ->
            val day = end.minusDays(27 - offset.toLong())
            val bucket = day.toString()
            val weekend = DayBucketCalendar.isWeekend(bucket)
            InsightEntry(
                id = offset.toLong() + 1,
                dayBucket = bucket,
                moodTags = if (weekend) {
                    listOf(InsightMoodTag.OVERWHELMED, InsightMoodTag.TIRED)
                } else {
                    listOf(InsightMoodTag.FOCUSED)
                },
            )
        }
        val patterns = InsightPatternDetector.detect(entries, avgTagsPerEvening = 1.5f)
        assertTrue(
            patterns.any {
                it.type == InsightPatternType.WEEKEND_AFFINITY &&
                    it.tag == InsightMoodTag.OVERWHELMED
            },
        )
    }
}
