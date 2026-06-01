// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.insights.domain

import com.pocketreflect.insights.model.InsightEntry
import com.pocketreflect.insights.model.InsightMoodTag

object InsightsSnapshotBuilder {

    fun build(
        entries: List<InsightEntry>,
        windowDays: Int,
        english: Boolean,
    ): InsightSnapshot {
        val sorted = entries.sortedBy { it.dayBucket }
        val total = sorted.size

        val tagScores = computeTagScores(sorted, total)
        val polarityShares = computePolarityShares(sorted, total)
        val avgTags = if (total == 0) 0f else sorted.sumOf { it.moodTags.size }.toFloat() / total
        val singleTagRate = if (total == 0) {
            0f
        } else {
            sorted.count { it.moodTags.size == 1 }.toFloat() / total
        }
        val activeCount = tagScores.count { it.value > 0f }

        val mapReadiness = when {
            total < InsightPolicy.MIN_ENTRIES_PREVIEW -> MapReadiness.Insufficient
            total < InsightPolicy.MIN_ENTRIES_FULL -> MapReadiness.Preview
            else -> MapReadiness.Full
        }

        val mapPolygonMode = resolvePolygonMode(activeCount, avgTags, tagScores)

        var patterns = InsightPatternDetector.detect(sorted, avgTags)
        if (patterns.count { it.type != InsightPatternType.DOMINANT_TAG } >= 3) {
            patterns = patterns.filter { it.type != InsightPatternType.DOMINANT_TAG }
        }

        val caption = StateMapCaptionBuilder.build(
            tagScores = tagScores,
            polarityShares = polarityShares,
            totalEvenings = total,
            singleTagEveningRate = singleTagRate,
            activeTagCount = activeCount,
            english = english,
        )

        return InsightSnapshot(
            windowDays = windowDays,
            totalEvenings = total,
            entries = sorted,
            tagScores = tagScores,
            polarityShares = polarityShares,
            patterns = patterns,
            mapReadiness = mapReadiness,
            mapPolygonMode = mapPolygonMode,
            activeTagCount = activeCount,
            avgTagsPerEvening = avgTags,
            singleTagEveningRate = singleTagRate,
            caption = caption,
        )
    }

    private fun computeTagScores(
        entries: List<InsightEntry>,
        total: Int,
    ): Map<InsightMoodTag, Float> {
        if (total == 0) return InsightMoodTag.entries.associateWith { 0f }
        return InsightMoodTag.entries.associateWith { tag ->
            entries.count { tag in it.moodTags }.toFloat() / total
        }
    }

    private fun computePolarityShares(
        entries: List<InsightEntry>,
        total: Int,
    ): PolarityShares {
        if (total == 0) return PolarityShares(0f, 0f, 0f)
        val pos = entries.count { entry ->
            entry.moodTags.any { it.polarity == InsightMoodTag.Polarity.POSITIVE }
        }
        val neg = entries.count { entry ->
            entry.moodTags.any { it.polarity == InsightMoodTag.Polarity.NEGATIVE }
        }
        val neu = entries.count { entry ->
            entry.moodTags.any { it.polarity == InsightMoodTag.Polarity.NEUTRAL }
        }
        return PolarityShares(
            positive = pos.toFloat() / total,
            neutral = neu.toFloat() / total,
            negative = neg.toFloat() / total,
        )
    }

    private fun resolvePolygonMode(
        activeTagCount: Int,
        avgTagsPerEvening: Float,
        tagScores: Map<InsightMoodTag, Float>,
    ): MapPolygonMode {
        if (activeTagCount <= 1) return MapPolygonMode.Hidden
        if (activeTagCount >= InsightPolicy.MIN_ACTIVE_TAGS_FOR_FULL_POLYGON &&
            avgTagsPerEvening >= InsightPolicy.MIN_AVG_TAGS_FOR_FULL_POLYGON
        ) {
            return MapPolygonMode.Full
        }
        val aboveThreshold = tagScores.count { it.value >= InsightPolicy.MIN_VERTEX_SCORE_SIMPLIFIED }
        return if (aboveThreshold >= 2) MapPolygonMode.Simplified else MapPolygonMode.Hidden
    }
}
