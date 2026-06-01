// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.insights.sandbox.fixtures

import com.pocketreflect.insights.domain.DayBucketCalendar
import com.pocketreflect.insights.model.InsightEntry
import com.pocketreflect.insights.model.InsightMoodTag
import java.time.LocalDate

enum class InsightFixtureId(val labelRu: String) {
    SPARSE_SINGLE_TAG("Один тег / день"),
    RICH_MULTI_TAG("Два тега, связки"),
    SEQUENCE_ANXIETY("Тревога → усталость"),
    WEEKEND_OVERLOAD("Выходные vs будни"),
    INSUFFICIENT("Мало записей"),
    NO_PATTERNS("Без повторов"),
    PREVIEW_10("10 вечеров (preview)"),
    FULL_30("30 вечеров (золотой)"),
}

object InsightFixtures {

    fun entries(id: InsightFixtureId, windowDays: Int): List<InsightEntry> = when (id) {
        InsightFixtureId.SPARSE_SINGLE_TAG -> sparseSingleTag(windowDays)
        InsightFixtureId.RICH_MULTI_TAG -> richMultiTag(windowDays)
        InsightFixtureId.SEQUENCE_ANXIETY -> sequenceAnxiety(windowDays)
        InsightFixtureId.WEEKEND_OVERLOAD -> weekendOverload(windowDays)
        InsightFixtureId.INSUFFICIENT -> insufficient()
        InsightFixtureId.NO_PATTERNS -> noPatterns(windowDays)
        InsightFixtureId.PREVIEW_10 -> preview10()
        InsightFixtureId.FULL_30 -> full30()
    }

    private fun sparseSingleTag(days: Int): List<InsightEntry> =
        lastDays(days).mapIndexed { i, bucket ->
            entry(
                id = i + 1L,
                bucket = bucket,
                tags = listOf(if (i % 5 == 0) InsightMoodTag.ANXIETY else InsightMoodTag.OVERWHELMED),
                micro = if (i % 7 == 0) "Короткая опора" else "",
            )
        }

    private fun richMultiTag(days: Int): List<InsightEntry> =
        lastDays(days).mapIndexed { i, bucket ->
            val tags = when (i % 4) {
                0 -> listOf(InsightMoodTag.ANXIETY, InsightMoodTag.OVERWHELMED)
                1 -> listOf(InsightMoodTag.OVERWHELMED, InsightMoodTag.TIRED)
                2 -> listOf(InsightMoodTag.TIRED)
                else -> listOf(InsightMoodTag.CALM, InsightMoodTag.GRATITUDE)
            }
            entry(i + 1L, bucket, tags)
        }

    private fun sequenceAnxiety(days: Int): List<InsightEntry> {
        val buckets = lastDays(days)
        return buckets.mapIndexed { i, bucket ->
            val prev = if (i > 0) buckets[i - 1] else null
            val tags = when {
                prev != null && i > 0 && (i % 3 != 0) -> listOf(InsightMoodTag.TIRED)
                i % 3 == 0 -> listOf(InsightMoodTag.ANXIETY)
                else -> listOf(InsightMoodTag.OVERWHELMED)
            }
            val micro = if (tags == listOf(InsightMoodTag.TIRED) && i % 2 == 0) "Прогулка" else ""
            entry(i + 1L, bucket, tags, micro)
        }
    }

    private fun weekendOverload(days: Int): List<InsightEntry> =
        lastDays(days).mapIndexed { i, bucket ->
            val weekend = DayBucketCalendar.isWeekend(bucket)
            val tags = when {
                weekend -> listOf(InsightMoodTag.OVERWHELMED, InsightMoodTag.TIRED)
                i % 5 == 0 -> listOf(InsightMoodTag.ANXIETY)
                else -> listOf(InsightMoodTag.FOCUSED)
            }
            entry(i + 1L, bucket, tags)
        }

    private fun insufficient(): List<InsightEntry> =
        lastDays(6).mapIndexed { i, bucket ->
            entry(i + 1L, bucket, listOf(InsightMoodTag.TIRED))
        }

    private fun noPatterns(days: Int): List<InsightEntry> =
        lastDays(days).mapIndexed { i, bucket ->
            entry(i + 1L, bucket, listOf(InsightMoodTag.entries[i % InsightMoodTag.entries.size]))
        }

    private fun preview10(): List<InsightEntry> =
        lastDays(10).mapIndexed { i, bucket ->
            entry(i + 1L, bucket, listOf(InsightMoodTag.TIRED, InsightMoodTag.CALM).take(1 + (i % 2)))
        }

    private fun full30(): List<InsightEntry> =
        lastDays(30).mapIndexed { i, bucket ->
            when (i % 6) {
                0 -> entry(i + 1L, bucket, listOf(InsightMoodTag.ANXIETY, InsightMoodTag.OVERWHELMED))
                1 -> entry(i + 1L, bucket, listOf(InsightMoodTag.TIRED), "Шаг к делу")
                2 -> entry(i + 1L, bucket, listOf(InsightMoodTag.ANXIETY))
                3 -> entry(i + 1L, bucket, listOf(InsightMoodTag.TIRED))
                4 -> entry(i + 1L, bucket, listOf(InsightMoodTag.CALM))
                else -> entry(i + 1L, bucket, listOf(InsightMoodTag.JOY, InsightMoodTag.GRATITUDE))
            }
        }

    private fun entry(
        id: Long,
        bucket: String,
        tags: List<InsightMoodTag>,
        micro: String = "",
    ) = InsightEntry(
        id = id,
        dayBucket = bucket,
        moodTags = tags,
        microWins = micro,
        reflection = "Тестовая рефлексия для $bucket",
    )

    private fun lastDays(count: Int, end: LocalDate = LocalDate.of(2026, 5, 30)): List<String> =
        (0 until count).map { offset ->
            end.minusDays((count - 1 - offset).toLong()).toString()
        }
}
