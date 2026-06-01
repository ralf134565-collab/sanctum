// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.insights.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pocketreflect.insights.domain.InsightPolicy
import com.pocketreflect.insights.domain.InsightSnapshot
import com.pocketreflect.insights.model.InsightMoodTag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreenContent(
    snapshot: InsightSnapshot,
    english: Boolean,
    expandedCards: Boolean,
    highlightedTag: InsightMoodTag?,
    highlightedPatternId: String?,
    sheetRequest: InsightDaysSheetRequest?,
    onWindowDaysChange: (Int) -> Unit,
    onExpandCards: () -> Unit,
    onPatternClick: (String) -> Unit,
    onTagHighlight: (InsightMoodTag?) -> Unit,
    onPolarityClick: (InsightMoodTag.Polarity) -> Unit,
    onTagClick: (InsightMoodTag) -> Unit,
    onSheetDismiss: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PeriodSelector(
            windowDays = snapshot.windowDays,
            english = english,
            onSelect = onWindowDaysChange,
        )

        StateMapView(
            snapshot = snapshot,
            english = english,
            highlightedTag = highlightedTag,
            onPolarityClick = onPolarityClick,
            onTagClick = onTagClick,
        )

        Text(
            text = if (english) "Patterns" else "Закономерности",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp),
        )

        PatternsSection(
            snapshot = snapshot,
            english = english,
            expanded = expandedCards,
            highlightedTag = highlightedTag,
            highlightedPatternId = highlightedPatternId,
            onExpand = onExpandCards,
            onPatternClick = onPatternClick,
        )
    }

    InsightDaysSheet(
        request = sheetRequest,
        entries = snapshot.entries,
        onDismiss = onSheetDismiss,
        onOpenEntry = onOpenEntry,
    )
}

@Composable
private fun PeriodSelector(
    windowDays: Int,
    english: Boolean,
    onSelect: (Int) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = windowDays == InsightPolicy.WINDOW_30_DAYS,
            onClick = { onSelect(InsightPolicy.WINDOW_30_DAYS) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
        ) {
            Text(if (english) "30 days" else "30 дней")
        }
        SegmentedButton(
            selected = windowDays == InsightPolicy.WINDOW_90_DAYS,
            onClick = { onSelect(InsightPolicy.WINDOW_90_DAYS) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
        ) {
            Text(if (english) "90 days" else "90 дней")
        }
    }
}

@Composable
private fun PatternsSection(
    snapshot: InsightSnapshot,
    english: Boolean,
    expanded: Boolean,
    highlightedTag: InsightMoodTag?,
    highlightedPatternId: String?,
    onExpand: () -> Unit,
    onPatternClick: (String) -> Unit,
) {
    if (snapshot.totalEvenings < InsightPolicy.MIN_ENTRIES_FULL) {
        EmptyPatternsCard(
            title = if (english) "Too early for patterns" else "Пока рано для закономерностей",
            body = if (english) {
                "Need at least ${InsightPolicy.MIN_ENTRIES_FULL} evenings with entries. You have ${snapshot.totalEvenings}."
            } else {
                "Нужно хотя бы ${InsightPolicy.MIN_ENTRIES_FULL} вечеров с записями. Сейчас — ${snapshot.totalEvenings}."
            },
        )
        return
    }

    if (snapshot.patterns.isEmpty()) {
        EmptyPatternsCard(
            title = if (english) "No clear repeats yet" else "Явных повторов пока нет",
            body = if (english) {
                "Labels varied without strong links in this period."
            } else {
                "За период отметки менялись без устойчивых связок."
            },
        )
        return
    }

    val visible = if (expanded) {
        snapshot.patterns
    } else {
        snapshot.patterns.take(InsightPolicy.VISIBLE_CARDS_COLLAPSED)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        visible.forEach { pattern ->
            val highlighted = pattern.id == highlightedPatternId
            val dimmed = highlightedTag != null &&
                !pattern.involves(highlightedTag) &&
                highlightedPatternId == null
            InsightPatternCard(
                pattern = pattern,
                english = english,
                highlighted = highlighted,
                dimmed = dimmed,
                onClick = { onPatternClick(pattern.id) },
            )
        }
        if (!expanded && snapshot.patterns.size > InsightPolicy.VISIBLE_CARDS_COLLAPSED) {
            TextButton(onClick = onExpand, modifier = Modifier.fillMaxWidth()) {
                Text(if (english) "Show more" else "Показать ещё")
            }
        }
    }
}

@Composable
private fun EmptyPatternsCard(title: String, body: String) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
