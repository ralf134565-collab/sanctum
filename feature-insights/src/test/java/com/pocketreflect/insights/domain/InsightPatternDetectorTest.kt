// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.insights.domain

import com.pocketreflect.insights.model.InsightEntry
import com.pocketreflect.insights.model.InsightMoodTag
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightPatternDetectorTest {

    @Test
    fun coOccurrence_detected() {
        val entries = (1..12).map { i ->
            InsightEntry(
                id = i.toLong(),
                dayBucket = "2026-05-%02d".format(i),
                moodTags = listOf(InsightMoodTag.ANXIETY, InsightMoodTag.OVERWHELMED),
            )
        }
        val patterns = InsightPatternDetector.detect(entries, avgTagsPerEvening = 2f)
        assertTrue(patterns.any { it.type == InsightPatternType.CO_OCCURRENCE })
    }

    @Test
    fun coOccurrence_noDuplicateReversePair() {
        val entries = (1..9).map { i ->
            InsightEntry(
                id = i.toLong(),
                dayBucket = "2026-05-%02d".format(i),
                moodTags = listOf(InsightMoodTag.TIRED, InsightMoodTag.OVERWHELMED),
            )
        }
        val patterns = InsightPatternDetector.detect(entries, avgTagsPerEvening = 2f)
        val co = patterns.filter { it.type == InsightPatternType.CO_OCCURRENCE }
        assertEquals(1, co.size)
        assertEquals(setOf(InsightMoodTag.TIRED, InsightMoodTag.OVERWHELMED), co.first().involvedTags)
    }

    @Test
    fun sequence_anxietyToTired() {
        val entries = listOf(
            InsightEntry(1, "2026-05-01", listOf(InsightMoodTag.ANXIETY)),
            InsightEntry(2, "2026-05-02", listOf(InsightMoodTag.TIRED)),
            InsightEntry(3, "2026-05-03", listOf(InsightMoodTag.ANXIETY)),
            InsightEntry(4, "2026-05-04", listOf(InsightMoodTag.TIRED)),
            InsightEntry(5, "2026-05-05", listOf(InsightMoodTag.ANXIETY)),
            InsightEntry(6, "2026-05-06", listOf(InsightMoodTag.TIRED)),
            InsightEntry(7, "2026-05-07", listOf(InsightMoodTag.ANXIETY)),
            InsightEntry(8, "2026-05-08", listOf(InsightMoodTag.TIRED)),
        )
        val patterns = InsightPatternDetector.detect(entries, avgTagsPerEvening = 1f)
        assertTrue(
            patterns.any {
                it.type == InsightPatternType.SEQUENCE &&
                    it.tagA == InsightMoodTag.ANXIETY &&
                    it.tagB == InsightMoodTag.TIRED
            },
        )
    }
}
