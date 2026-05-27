// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.journal.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.pocketreflect.app.presentation.components.pressScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pocketreflect.app.domain.model.MoodTag

/**
 * Набор «таблеток-тегов» для аффективного лейблинга.
 *
 * UX-замечания:
 *  - FlowRow вместо LazyRow: пользователь должен видеть все варианты разом,
 *    а не «свайпать» — это снижает шанс выбрать ближайший по доступности тег.
 *  - Цвет чипа намекает на полярность тега, но крайне мягко
 *    (никаких красных рамок «у тебя плохо»).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoodTagChips(
    tags: List<MoodTag>,
    selected: Set<MoodTag>,
    onToggle: (MoodTag) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            val isSelected = tag in selected
            val interactionSource = remember { MutableInteractionSource() }
            FilterChip(
                selected = isSelected,
                onClick = { onToggle(tag) },
                modifier = Modifier.pressScale(interactionSource),
                interactionSource = interactionSource,
                label = {
                    Text(
                        text = tag.displayLabel(),
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = when (tag.polarity) {
                        MoodTag.Polarity.POSITIVE -> MaterialTheme.colorScheme.secondaryContainer
                        MoodTag.Polarity.NEUTRAL  -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)
                        MoodTag.Polarity.NEGATIVE -> MaterialTheme.colorScheme.primaryContainer
                    },
                    selectedLabelColor = when (tag.polarity) {
                        MoodTag.Polarity.POSITIVE -> MaterialTheme.colorScheme.onSecondaryContainer
                        MoodTag.Polarity.NEUTRAL  -> MaterialTheme.colorScheme.onSurface
                        MoodTag.Polarity.NEGATIVE -> MaterialTheme.colorScheme.onPrimaryContainer
                    },
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outline,
                ),
            )
        }
    }
}

/**
 * Неинтерактивный (read-only) набор «таблеток-тегов» для детального просмотра записи.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReadOnlyMoodTagChips(
    tags: List<MoodTag>,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when (tag.polarity) {
                    MoodTag.Polarity.POSITIVE -> MaterialTheme.colorScheme.secondaryContainer
                    MoodTag.Polarity.NEUTRAL  -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)
                    MoodTag.Polarity.NEGATIVE -> MaterialTheme.colorScheme.primaryContainer
                },
                contentColor = when (tag.polarity) {
                    MoodTag.Polarity.POSITIVE -> MaterialTheme.colorScheme.onSecondaryContainer
                    MoodTag.Polarity.NEUTRAL  -> MaterialTheme.colorScheme.onSurface
                    MoodTag.Polarity.NEGATIVE -> MaterialTheme.colorScheme.onPrimaryContainer
                },
            ) {
                Text(
                    text = tag.displayLabel(),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
