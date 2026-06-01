// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.insights.domain

import com.pocketreflect.insights.model.InsightMoodTag

object StateMapCaptionBuilder {

    fun build(
        tagScores: Map<InsightMoodTag, Float>,
        polarityShares: PolarityShares,
        totalEvenings: Int,
        singleTagEveningRate: Float,
        activeTagCount: Int,
        english: Boolean,
    ): String {
        if (totalEvenings < InsightPolicy.MIN_ENTRIES_PREVIEW) {
            return if (english) {
                "Not enough evenings for a summary yet."
            } else {
                "Пока мало вечеров для сводки."
            }
        }

        if (singleTagEveningRate >= InsightPolicy.SINGLE_TAG_EVENING_RATE) {
            val top = topTags(tagScores, 1).firstOrNull()
            if (top != null && activeTagCount <= 2) {
                val count = (top.second * totalEvenings).toInt().coerceAtLeast(1)
                return if (english) {
                    "You often marked one label per evening — that's enough. Most often: " +
                        "\"${top.first.displayName(true)}\" ($count of $totalEvenings evenings)."
                } else {
                    "Вы часто отмечали один тег за вечер — для сводки этого достаточно. " +
                        "Чаще всего: «${top.first.displayName(false)}» ($count из $totalEvenings вечеров)."
                }
            }
            return if (english) {
                "You often marked one label per evening — that's a normal practice here."
            } else {
                "Вы часто отмечали один тег за вечер — это обычная практика."
            }
        }

        val top2 = topTags(tagScores, 2)
        if (top2.isEmpty()) {
            return if (english) "Few labels in this period." else "Мало отметок за период."
        }

        val polarityHint = polarityHint(polarityShares, english)
        val tagsLine = if (english) {
            top2.joinToString(" and ") { (tag, score) ->
                val n = (score * totalEvenings).toInt().coerceAtLeast(1)
                "\"${tag.displayName(true)}\" ($n evenings)"
            }
        } else {
            top2.joinToString(" и ") { (tag, score) ->
                val n = (score * totalEvenings).toInt().coerceAtLeast(1)
                "«${tag.displayName(false)}» ($n вечеров)"
            }
        }

        return if (english) {
            "$polarityHint Most often marked: $tagsLine."
        } else {
            "$polarityHint Чаще всего отмечали: $tagsLine."
        }
    }

    private fun polarityHint(shares: PolarityShares, english: Boolean): String {
        val dominant = listOf(
            "positive" to shares.positive,
            "neutral" to shares.neutral,
            "negative" to shares.negative,
        ).maxBy { it.second }
        if (dominant.second < 0.35f) return ""
        return when (dominant.first) {
            "negative" -> if (english) {
                "Many evenings included heavier labels."
            } else {
                "Во многих вечерах встречались более тяжёлые отметки."
            }
            "neutral" -> if (english) {
                "Fatigue and load labels were common."
            } else {
                "Часто встречались отметки усталости и перегруза."
            }
            else -> if (english) {
                "Resourceful labels appeared often."
            } else {
                "Часто встречались ресурсные отметки."
            }
        }
    }

    private fun topTags(
        scores: Map<InsightMoodTag, Float>,
        limit: Int,
    ): List<Pair<InsightMoodTag, Float>> =
        scores.entries
            .filter { it.value > 0f }
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key to it.value }
}
