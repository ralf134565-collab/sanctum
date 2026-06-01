// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.insights.domain

import com.pocketreflect.insights.model.InsightEntry
import com.pocketreflect.insights.model.InsightMoodTag

enum class MapReadiness { Insufficient, Preview, Full }

enum class MapPolygonMode { Hidden, Simplified, Full }

data class PolarityShares(
    val positive: Float,
    val neutral: Float,
    val negative: Float,
)

data class InsightSnapshot(
    val windowDays: Int,
    val totalEvenings: Int,
    val entries: List<InsightEntry>,
    val tagScores: Map<InsightMoodTag, Float>,
    val polarityShares: PolarityShares,
    val patterns: List<InsightPattern>,
    val mapReadiness: MapReadiness,
    val mapPolygonMode: MapPolygonMode,
    val activeTagCount: Int,
    val avgTagsPerEvening: Float,
    val singleTagEveningRate: Float,
    val caption: String,
)
