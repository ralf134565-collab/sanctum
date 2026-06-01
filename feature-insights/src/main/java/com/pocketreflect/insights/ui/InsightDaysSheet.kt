// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.insights.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pocketreflect.insights.model.InsightEntry
import com.pocketreflect.insights.model.InsightMoodTag

data class InsightDaysSheetRequest(
    val title: String,
    val entryIds: List<Long>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightDaysSheet(
    request: InsightDaysSheetRequest?,
    entries: List<InsightEntry>,
    onDismiss: () -> Unit,
    onOpenEntry: (Long) -> Unit,
) {
    if (request == null) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val byId = entries.associateBy { it.id }
    val rows = request.entryIds.mapNotNull { byId[it] }.distinctBy { it.dayBucket }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = request.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            LazyColumn {
                items(rows, key = { it.id }) { entry ->
                    DayRow(
                        entry = entry,
                        onClick = { onOpenEntry(entry.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DayRow(
    entry: InsightEntry,
    onClick: () -> Unit,
) {
    val tags = entry.moodTags.joinToString(" · ") { it.displayNameRu }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Text(
            text = entry.dayBucket,
            style = MaterialTheme.typography.titleSmall,
        )
        if (tags.isNotBlank()) {
            Text(
                text = tags,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

fun polaritySheetTitle(polarity: InsightMoodTag.Polarity, english: Boolean): String = when (polarity) {
    InsightMoodTag.Polarity.POSITIVE -> if (english) "Evenings with positive labels" else "Вечера с позитивными отметками"
    InsightMoodTag.Polarity.NEUTRAL -> if (english) "Evenings with neutral labels" else "Вечера с нейтральными отметками"
    InsightMoodTag.Polarity.NEGATIVE -> if (english) "Evenings with heavier labels" else "Вечера с более тяжёлыми отметками"
}

fun tagSheetTitle(tag: InsightMoodTag, english: Boolean): String =
    if (english) "Evenings with \"${tag.displayNameEn}\"" else "Вечера с «${tag.displayNameRu}»"
