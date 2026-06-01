// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.insights.sandbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pocketreflect.insights.domain.InsightPatternFormatter
import com.pocketreflect.insights.domain.InsightPolicy
import com.pocketreflect.insights.domain.InsightSnapshot
import com.pocketreflect.insights.domain.InsightsSnapshotBuilder
import com.pocketreflect.insights.model.InsightMoodTag
import com.pocketreflect.insights.sandbox.fixtures.InsightFixtureId
import com.pocketreflect.insights.sandbox.fixtures.InsightFixtures
import com.pocketreflect.insights.ui.InsightDaysSheetRequest
import com.pocketreflect.insights.ui.InsightsScreenContent
import com.pocketreflect.insights.ui.InsightsTheme
import com.pocketreflect.insights.ui.polaritySheetTitle
import com.pocketreflect.insights.ui.tagSheetTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsPrototypeScreen() {
    var fixtureId by remember { mutableStateOf(InsightFixtureId.SPARSE_SINGLE_TAG) }
    var windowDays by remember { mutableIntStateOf(InsightPolicy.WINDOW_30_DAYS) }
    var english by remember { mutableStateOf(false) }
    var testPanelExpanded by remember { mutableStateOf(true) }
    var expandedCards by remember { mutableStateOf(false) }
    var highlightedTag by remember { mutableStateOf<InsightMoodTag?>(null) }
    var highlightedPatternId by remember { mutableStateOf<String?>(null) }
    var sheetRequest by remember { mutableStateOf<InsightDaysSheetRequest?>(null) }
    var lastOpenedEntryId by remember { mutableStateOf<Long?>(null) }

    val entries = remember(fixtureId, windowDays) {
        InsightFixtures.entries(fixtureId, windowDays)
    }
    val snapshot: InsightSnapshot = remember(entries, windowDays, english) {
        InsightsSnapshotBuilder.build(entries, windowDays, english)
    }

    InsightsTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Песочница «Картина»")
                            Text(
                                text = "Тест UI · не основное приложение",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                SandboxTestPanel(
                    expanded = testPanelExpanded,
                    fixtureId = fixtureId,
                    english = english,
                    snapshot = snapshot,
                    lastOpenedEntryId = lastOpenedEntryId,
                    onExpandedChange = { testPanelExpanded = it },
                    onFixture = {
                        fixtureId = it
                        highlightedTag = null
                        highlightedPatternId = null
                    },
                    onEnglish = { english = it },
                )

                AppTabPreviewSection(
                    modifier = Modifier.weight(1f),
                    snapshot = snapshot,
                    english = english,
                    expandedCards = expandedCards,
                    highlightedTag = highlightedTag,
                    highlightedPatternId = highlightedPatternId,
                    sheetRequest = sheetRequest,
                    onWindowDaysChange = { days ->
                        windowDays = days
                        expandedCards = false
                        highlightedTag = null
                        highlightedPatternId = null
                        sheetRequest = null
                    },
                    onExpandCards = { expandedCards = true },
                    onPatternClick = { id ->
                        highlightedPatternId = if (highlightedPatternId == id) null else id
                        highlightedTag = null
                        val pattern = snapshot.patterns.firstOrNull { it.id == id }
                            ?: return@AppTabPreviewSection
                        sheetRequest = InsightDaysSheetRequest(
                            title = InsightPatternFormatter.format(pattern, english).title,
                            entryIds = pattern.entryIds,
                        )
                    },
                    onTagHighlight = { highlightedTag = it },
                    onPolarityClick = { polarity ->
                        highlightedTag = null
                        highlightedPatternId = null
                        val ids = snapshot.entries
                            .filter { e -> e.moodTags.any { it.polarity == polarity } }
                            .map { it.id }
                        sheetRequest = InsightDaysSheetRequest(
                            title = polaritySheetTitle(polarity, english),
                            entryIds = ids,
                        )
                    },
                    onTagClick = { tag ->
                        highlightedTag = if (highlightedTag == tag) null else tag
                        highlightedPatternId = null
                        val ids = snapshot.entries.filter { tag in it.moodTags }.map { it.id }
                        sheetRequest = InsightDaysSheetRequest(
                            title = tagSheetTitle(tag, english),
                            entryIds = ids,
                        )
                    },
                    onSheetDismiss = { sheetRequest = null },
                    onOpenEntry = { id ->
                        lastOpenedEntryId = id
                        sheetRequest = null
                    },
                )
            }
        }
    }
}

/** Только sandbox: фикстуры, EN, отладка. В релизном приложении этого блока нет. */
@Composable
private fun SandboxTestPanel(
    expanded: Boolean,
    fixtureId: InsightFixtureId,
    english: Boolean,
    snapshot: InsightSnapshot,
    lastOpenedEntryId: Long?,
    onExpandedChange: (Boolean) -> Unit,
    onFixture: (InsightFixtureId) -> Unit,
    onEnglish: (Boolean) -> Unit,
) {
    val chipScroll = rememberScrollState()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Science,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                    Column {
                        Text(
                            text = "Настройки теста",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = "Не попадут во вкладку приложения",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = { onExpandedChange(!expanded) }) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Свернуть" else "Развернуть",
                    )
                }
            }

            if (expanded) {
                Text(
                    text = "Сценарий данных подставляет записи вместо журнала. " +
                        "Переключатель EN — только для проверки английских текстов.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 10.dp),
                )

                Text("Сценарий", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(chipScroll)
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    InsightFixtureId.entries.forEach { id ->
                        FilterChip(
                            selected = fixtureId == id,
                            onClick = { onFixture(id) },
                            label = { Text(id.labelRu) },
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Тексты EN", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = "В приложении язык из системы",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = english, onCheckedChange = onEnglish)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                Text(
                    text = buildString {
                        append("Записей в сценарии: ${snapshot.totalEvenings}")
                        append(" · закономерностей: ${snapshot.patterns.size}")
                        append(" · период в превью: ${snapshot.windowDays} дн.")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (lastOpenedEntryId != null) {
                    Text(
                        text = "Отладка: открыта запись id=$lastOpenedEntryId (заглушка детали)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

/** То, что увидит пользователь во вкладке «Картина» (без фикстур и EN). */
@Composable
private fun AppTabPreviewSection(
    modifier: Modifier = Modifier,
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
) {
    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Smartphone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column {
                    Text(
                        text = "Вкладка в приложении",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "Период 30/90, сводка и закономерности — как у пользователя",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))

        InsightsScreenContent(
            snapshot = snapshot,
            english = english,
            expandedCards = expandedCards,
            highlightedTag = highlightedTag,
            highlightedPatternId = highlightedPatternId,
            sheetRequest = sheetRequest,
            onWindowDaysChange = onWindowDaysChange,
            onExpandCards = onExpandCards,
            onPatternClick = onPatternClick,
            onTagHighlight = onTagHighlight,
            onPolarityClick = onPolarityClick,
            onTagClick = onTagClick,
            onSheetDismiss = onSheetDismiss,
            onOpenEntry = onOpenEntry,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
