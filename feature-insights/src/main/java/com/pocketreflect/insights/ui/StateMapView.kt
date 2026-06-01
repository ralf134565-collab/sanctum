// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.insights.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.pocketreflect.insights.domain.InsightPolicy
import com.pocketreflect.insights.domain.InsightSnapshot
import com.pocketreflect.insights.domain.MapReadiness
import com.pocketreflect.insights.domain.PolarityShares
import com.pocketreflect.insights.model.InsightMoodTag

/**
 * Сводка отметок: полоски и числа вместо «радара» — проще читать на телефоне.
 */
@Composable
fun StateMapView(
    snapshot: InsightSnapshot,
    english: Boolean,
    highlightedTag: InsightMoodTag?,
    onPolarityClick: (InsightMoodTag.Polarity) -> Unit,
    onTagClick: (InsightMoodTag) -> Unit,
    modifier: Modifier = Modifier,
) {
    key(snapshot.windowDays, snapshot.totalEvenings, snapshot.mapReadiness) {
        Column(modifier = modifier.fillMaxWidth()) {
            when (snapshot.mapReadiness) {
                MapReadiness.Insufficient -> {
                    Text(
                        text = if (english) {
                            "Need at least ${InsightPolicy.MIN_ENTRIES_FULL} evenings with entries."
                        } else {
                            "Нужно хотя бы ${InsightPolicy.MIN_ENTRIES_FULL} вечеров с записями."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                MapReadiness.Preview, MapReadiness.Full -> {
                    if (snapshot.mapReadiness == MapReadiness.Preview) {
                        Text(
                            text = if (english) "Preliminary summary" else "Предварительная сводка",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }

                    Text(
                        text = if (english) "By type of label" else "По типу отметок",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    PolarityBars(
                        shares = snapshot.polarityShares,
                        total = snapshot.totalEvenings,
                        english = english,
                        onPolarityClick = onPolarityClick,
                    )

                    Text(
                        text = if (english) "Most marked" else "Чаще всего отмечали",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    )
                    TagBars(
                        tagScores = snapshot.tagScores,
                        total = snapshot.totalEvenings,
                        english = english,
                        highlightedTag = highlightedTag,
                        onTagClick = onTagClick,
                    )
                }
            }

            Text(
                text = snapshot.caption,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun PolarityBars(
    shares: PolarityShares,
    total: Int,
    english: Boolean,
    onPolarityClick: (InsightMoodTag.Polarity) -> Unit,
) {
    val rows = listOf(
        PolarityRowSpec(
            polarity = InsightMoodTag.Polarity.POSITIVE,
            label = if (english) "Resourceful" else "Ресурсные",
            share = shares.positive,
            color = MaterialTheme.colorScheme.primary,
        ),
        PolarityRowSpec(
            polarity = InsightMoodTag.Polarity.NEUTRAL,
            label = if (english) "Fatigue & load" else "Усталость и перегруз",
            share = shares.neutral,
            color = MaterialTheme.colorScheme.secondary,
        ),
        PolarityRowSpec(
            polarity = InsightMoodTag.Polarity.NEGATIVE,
            label = if (english) "Heavier" else "Более тяжёлые",
            share = shares.negative,
            color = MaterialTheme.colorScheme.tertiary,
        ),
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { spec ->
            PolarityBarRow(
                spec = spec,
                total = total,
                english = english,
                onClick = { onPolarityClick(spec.polarity) },
            )
        }
    }
}

private data class PolarityRowSpec(
    val polarity: InsightMoodTag.Polarity,
    val label: String,
    val share: Float,
    val color: androidx.compose.ui.graphics.Color,
)

@Composable
private fun PolarityBarRow(
    spec: PolarityRowSpec,
    total: Int,
    english: Boolean,
    onClick: () -> Unit,
) {
    val count = (spec.share * total).toInt().coerceAtLeast(0)
    val pct = (spec.share * 100).toInt().coerceIn(0, 100)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = spec.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (english) "$count of $total · $pct%" else "$count из $total · $pct%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LinearProgressIndicator(
                progress = { spec.share.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = spec.color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun TagBars(
    tagScores: Map<InsightMoodTag, Float>,
    total: Int,
    english: Boolean,
    highlightedTag: InsightMoodTag?,
    onTagClick: (InsightMoodTag) -> Unit,
) {
    val top = tagScores.entries
        .filter { it.value > 0f }
        .sortedByDescending { it.value }
        .take(5)

    if (top.isEmpty()) {
        Text(
            text = if (english) "No labels in this period yet." else "За период пока нет отметок.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    val maxCount = top.maxOf { (it.value * total).toInt().coerceAtLeast(1) }.coerceAtLeast(1)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        top.forEach { (tag, score) ->
            val count = (score * total).toInt().coerceAtLeast(1)
            val fraction = count.toFloat() / maxCount.toFloat()
            val selected = tag == highlightedTag
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onTagClick(tag) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = tag.displayName(english),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(0.42f),
                )
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .weight(0.43f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round,
                )
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(0.15f)
                        .padding(start = 8.dp),
                )
            }
        }
    }
}
