// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.insights.domain

import com.pocketreflect.insights.model.InsightEntry
import com.pocketreflect.insights.model.InsightMoodTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightsSnapshotBuilderTest {

    @Test
    fun sparseSingleTag_hidesPolygon() {
        val entries = (1..20).map { i ->
            InsightEntry(
                id = i.toLong(),
                dayBucket = "2026-05-%02d".format(i),
                moodTags = listOf(InsightMoodTag.OVERWHELMED),
            )
        }
        val snapshot = InsightsSnapshotBuilder.build(entries, 30, english = false)
        assertEquals(MapPolygonMode.Hidden, snapshot.mapPolygonMode)
        assertEquals(MapReadiness.Full, snapshot.mapReadiness)
        assertTrue(snapshot.singleTagEveningRate >= 0.99f)
        assertFalse(snapshot.caption.contains("дисбаланс", ignoreCase = true))
    }

    @Test
    fun insufficient_readiness() {
        val entries = (1..6).map { i ->
            InsightEntry(i.toLong(), "2026-05-0$i", listOf(InsightMoodTag.CALM))
        }
        val snapshot = InsightsSnapshotBuilder.build(entries, 30, english = false)
        assertEquals(MapReadiness.Insufficient, snapshot.mapReadiness)
        assertTrue(snapshot.patterns.isEmpty())
    }

    @Test
    fun richMultiTag_canShowFullOrSimplifiedPolygon() {
        val entries = (1..18).map { i ->
            InsightEntry(
                id = i.toLong(),
                dayBucket = "2026-05-%02d".format(i),
                moodTags = when (i % 3) {
                    0 -> listOf(InsightMoodTag.ANXIETY, InsightMoodTag.OVERWHELMED)
                    1 -> listOf(InsightMoodTag.TIRED, InsightMoodTag.OVERWHELMED)
                    else -> listOf(InsightMoodTag.CALM, InsightMoodTag.JOY)
                },
            )
        }
        val snapshot = InsightsSnapshotBuilder.build(entries, 30, english = false)
        assertTrue(
            snapshot.mapPolygonMode == MapPolygonMode.Full ||
                snapshot.mapPolygonMode == MapPolygonMode.Simplified,
        )
        assertTrue(snapshot.patterns.isNotEmpty())
    }
}
