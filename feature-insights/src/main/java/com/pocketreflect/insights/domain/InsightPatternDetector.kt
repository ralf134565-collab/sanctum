// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.insights.domain

import com.pocketreflect.insights.model.InsightEntry
import com.pocketreflect.insights.model.InsightMoodTag
import com.pocketreflect.insights.model.hasNegative
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object InsightPatternDetector {

    private val dayFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun detect(entries: List<InsightEntry>, avgTagsPerEvening: Float): List<InsightPattern> {
        if (entries.size < InsightPolicy.MIN_ENTRIES_PREVIEW) return emptyList()

        val sorted = entries.sortedBy { it.dayBucket }
        val byDay = sorted.associateBy { it.dayBucket }

        val candidates = buildList {
            addAll(detectCoOccurrence(sorted, avgTagsPerEvening))
            addAll(detectSequence(sorted, byDay, avgTagsPerEvening))
            addAll(detectStreak(sorted))
            addAll(detectRecovery(sorted, byDay))
            addAll(detectWeekpartAffinity(sorted))
            addAll(detectDominant(sorted))
        }

        return candidates
            .sortedByDescending { it.score }
            .distinctBy { patternDedupKey(it) }
            .take(InsightPolicy.MAX_CARDS)
    }

    /** Одна карточка на неупорядоченную пару тегов (без «A+B» и «B+A»). */
    private fun patternDedupKey(pattern: InsightPattern): String = when (pattern.type) {
        InsightPatternType.CO_OCCURRENCE -> {
            val keys = listOf(pattern.tagA!!.storageKey, pattern.tagB!!.storageKey).sorted()
            "co_${keys[0]}|${keys[1]}"
        }
        else -> pattern.id
    }

    private fun detectCoOccurrence(
        entries: List<InsightEntry>,
        avgTagsPerEvening: Float,
    ): List<InsightPattern> {
        val weight = if (avgTagsPerEvening < InsightPolicy.MIN_AVG_TAGS_FOR_FULL_POLYGON) 0.9f else 1f
        val result = mutableListOf<InsightPattern>()
        val tags = InsightMoodTag.entries

        for (i in tags.indices) {
            val a = tags[i]
            for (j in i + 1 until tags.size) {
                val b = tags[j]
                val matched = entries.filter { a in it.moodTags && b in it.moodTags }
                val support = matched.size
                if (support < InsightPolicy.MIN_PATTERN_SUPPORT) continue
                val unionBase = entries.count { a in it.moodTags || b in it.moodTags }
                if (unionBase == 0) continue
                val rate = support.toFloat() / unionBase
                if (rate < InsightPolicy.MIN_PATTERN_RATE) continue
                val pairScore = pairTypeWeight(a, b) * support * weight
                result += InsightPattern(
                    id = coOccurrenceId(a, b),
                    type = InsightPatternType.CO_OCCURRENCE,
                    support = support,
                    base = unionBase,
                    entryIds = matched.map { it.id },
                    involvedTags = setOf(a, b),
                    score = pairScore,
                    tagA = a,
                    tagB = b,
                )
            }
        }
        return result
    }

    private fun coOccurrenceId(a: InsightMoodTag, b: InsightMoodTag): String {
        val keys = listOf(a.storageKey, b.storageKey).sorted()
        return "co_${keys[0]}_${keys[1]}"
    }

    private fun detectSequence(
        sorted: List<InsightEntry>,
        byDay: Map<String, InsightEntry>,
        avgTagsPerEvening: Float,
    ): List<InsightPattern> {
        val weight = 1.1f
        val result = mutableListOf<InsightPattern>()
        val tags = InsightMoodTag.entries

        for (a in tags) {
            val daysWithA = sorted.filter { a in it.moodTags }
            for (b in tags) {
                if (b == a) continue
                var support = 0
                val matchedIds = mutableListOf<Long>()
                for (entry in daysWithA) {
                    val nextDay = LocalDate.parse(entry.dayBucket, dayFormatter).plusDays(1)
                    val nextBucket = nextDay.format(dayFormatter)
                    val nextEntry = byDay[nextBucket] ?: continue
                    if (b in nextEntry.moodTags) {
                        support++
                        matchedIds += nextEntry.id
                    }
                }
                if (support < InsightPolicy.MIN_PATTERN_SUPPORT) continue
                val base = daysWithA.count { day ->
                    val nextDay = LocalDate.parse(day.dayBucket, dayFormatter).plusDays(1)
                    byDay.containsKey(nextDay.format(dayFormatter))
                }
                if (base == 0) continue
                val rate = support.toFloat() / base
                if (rate < InsightPolicy.MIN_PATTERN_RATE) continue
                val seqWeight = sequencePairWeight(a, b)
                result += InsightPattern(
                    id = "seq_${a.storageKey}_${b.storageKey}",
                    type = InsightPatternType.SEQUENCE,
                    support = support,
                    base = base,
                    entryIds = matchedIds.distinct(),
                    involvedTags = setOf(a, b),
                    score = seqWeight * support * weight * avgTagsPerEvening.coerceAtLeast(0.8f),
                    tagA = a,
                    tagB = b,
                )
            }
        }
        return result
    }

    private fun detectStreak(sorted: List<InsightEntry>): List<InsightPattern> {
        if (sorted.size < 3) return emptyList()
        val lastThree = sorted.takeLast(3)
        val common = lastThree.map { it.moodTags.toSet() }.reduce { acc, set -> acc intersect set }
        if (common.isEmpty()) return emptyList()

        val bestTag = common.maxByOrNull { tag ->
            lastThree.count { tag in it.moodTags }
        } ?: return emptyList()

        if (lastThree.any { bestTag !in it.moodTags }) return emptyList()

        val isNegative = bestTag.polarity == InsightMoodTag.Polarity.NEGATIVE
        return listOf(
            InsightPattern(
                id = "streak_${bestTag.storageKey}",
                type = InsightPatternType.STREAK,
                support = 3,
                base = 3,
                entryIds = lastThree.map { it.id },
                involvedTags = setOf(bestTag),
                score = (if (isNegative) 1.15f else 1f) * 3f,
                fromDayBucket = lastThree.first().dayBucket,
                toDayBucket = lastThree.last().dayBucket,
                tag = bestTag,
            ),
        )
    }

    private fun detectRecovery(
        sorted: List<InsightEntry>,
        byDay: Map<String, InsightEntry>,
    ): List<InsightPattern> {
        var support = 0
        var base = 0
        val matchedIds = mutableListOf<Long>()

        for (entry in sorted) {
            if (!entry.moodTags.hasNegative) continue
            val nextDay = LocalDate.parse(entry.dayBucket, dayFormatter).plusDays(1)
            val nextEntry = byDay[nextDay.format(dayFormatter)] ?: continue
            base++
            if (nextEntry.microWins.isNotBlank() && !nextEntry.moodTags.hasNegative) {
                support++
                matchedIds += nextEntry.id
            }
        }

        if (support < InsightPolicy.MIN_PATTERN_SUPPORT || base == 0) return emptyList()
        val rate = support.toFloat() / base
        if (rate < InsightPolicy.MIN_PATTERN_RATE) return emptyList()

        return listOf(
            InsightPattern(
                id = "recovery_micro_wins",
                type = InsightPatternType.RECOVERY_MICRO_WINS,
                support = support,
                base = base,
                entryIds = matchedIds.distinct(),
                involvedTags = emptySet(),
                score = support * 1.05f,
            ),
        )
    }

    private fun detectWeekpartAffinity(sorted: List<InsightEntry>): List<InsightPattern> {
        val weekend = sorted.filter { DayBucketCalendar.isWeekend(it.dayBucket) }
        val weekday = sorted.filter { DayBucketCalendar.isWeekday(it.dayBucket) }
        if (weekend.size < InsightPolicy.MIN_WEEKEND_EVENINGS ||
            weekday.size < InsightPolicy.MIN_WEEKDAY_EVENINGS
        ) {
            return emptyList()
        }

        val result = mutableListOf<InsightPattern>()
        for (tag in InsightMoodTag.entries) {
            val weekendHits = weekend.filter { tag in it.moodTags }
            val weekdayHits = weekday.filter { tag in it.moodTags }
            val weekendRate = weekendHits.size.toFloat() / weekend.size
            val weekdayRate = weekdayHits.size.toFloat() / weekday.size

            if (weekendHits.size >= InsightPolicy.MIN_PATTERN_SUPPORT &&
                weekendRate >= InsightPolicy.MIN_WEEKPART_TAG_RATE &&
                weekendRate - weekdayRate >= InsightPolicy.WEEKPART_CONTRAST_DELTA
            ) {
                result += InsightPattern(
                    id = "weekend_${tag.storageKey}",
                    type = InsightPatternType.WEEKEND_AFFINITY,
                    support = weekendHits.size,
                    base = weekend.size,
                    entryIds = weekendHits.map { it.id },
                    involvedTags = setOf(tag),
                    score = weekendHits.size * 1.12f,
                    tag = tag,
                )
            }

            if (weekdayHits.size >= InsightPolicy.MIN_PATTERN_SUPPORT &&
                weekdayRate >= InsightPolicy.MIN_WEEKPART_TAG_RATE &&
                weekdayRate - weekendRate >= InsightPolicy.WEEKPART_CONTRAST_DELTA
            ) {
                result += InsightPattern(
                    id = "weekday_${tag.storageKey}",
                    type = InsightPatternType.WEEKDAY_AFFINITY,
                    support = weekdayHits.size,
                    base = weekday.size,
                    entryIds = weekdayHits.map { it.id },
                    involvedTags = setOf(tag),
                    score = weekdayHits.size * 1.08f,
                    tag = tag,
                )
            }
        }
        return result
    }

    private fun detectDominant(sorted: List<InsightEntry>): List<InsightPattern> {
        val total = sorted.size
        val counts = InsightMoodTag.entries.associateWith { tag ->
            sorted.count { tag in it.moodTags }
        }
        val dominant = counts.maxByOrNull { it.value } ?: return emptyList()
        if (dominant.value < InsightPolicy.MIN_PATTERN_SUPPORT) return emptyList()
        val rate = dominant.value.toFloat() / total
        if (rate < InsightPolicy.DOMINANT_TAG_RATE) return emptyList()

        return listOf(
            InsightPattern(
                id = "dominant_${dominant.key.storageKey}",
                type = InsightPatternType.DOMINANT_TAG,
                support = dominant.value,
                base = total,
                entryIds = sorted.filter { dominant.key in it.moodTags }.map { it.id },
                involvedTags = setOf(dominant.key),
                score = dominant.value * 0.7f,
                tag = dominant.key,
            ),
        )
    }

    private fun pairTypeWeight(a: InsightMoodTag, b: InsightMoodTag): Float {
        val pa = a.polarity
        val pb = b.polarity
        return when {
            pa == InsightMoodTag.Polarity.NEGATIVE && pb == InsightMoodTag.Polarity.NEGATIVE -> 1.2f
            pa == InsightMoodTag.Polarity.NEGATIVE && pb == InsightMoodTag.Polarity.NEUTRAL -> 1.2f
            pa == InsightMoodTag.Polarity.NEUTRAL && pb == InsightMoodTag.Polarity.NEGATIVE -> 1.2f
            pa == InsightMoodTag.Polarity.NEGATIVE && pb == InsightMoodTag.Polarity.POSITIVE -> 1.1f
            pa == InsightMoodTag.Polarity.POSITIVE && pb == InsightMoodTag.Polarity.NEGATIVE -> 1.1f
            else -> 1f
        }
    }

    private fun sequencePairWeight(a: InsightMoodTag, b: InsightMoodTag): Float {
        val key = "${a.storageKey}->${b.storageKey}"
        val prioritized = setOf(
            "anxiety->tired",
            "overwhelmed->tired",
            "irritation->anxiety",
            "anxiety->calm",
            "overwhelmed->calm",
            "sadness->calm",
        )
        return if (key in prioritized) 1.15f else 1f
    }
}
