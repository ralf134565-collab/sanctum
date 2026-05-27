// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.settings.model.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketreflect.app.R
import com.pocketreflect.app.data.model.ModelManifest
import com.pocketreflect.app.data.repository.AttachedModel
import com.pocketreflect.app.presentation.journal.components.SectionCard
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Карточка «модель подключена».
 *
 * Без agressive call-to-action: пользователь сам решает, заменять файл или
 * удалить. Заменить = повторно запускаем тот же поток подключения (SAF),
 * только для уже выбранного варианта.
 */
@Composable
fun AttachedModelCard(
    attached: AttachedModel,
    onReplace: () -> Unit,
    onRequestDetach: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val entry = ModelManifest.entryOf(attached.variant)
    SectionCard(
        title = stringResource(R.string.model_attached_card_title),
        subtitle = entry.displayName,
        modifier = modifier,
    ) {
        Text(
            text = "• " + stringResource(
                R.string.model_attached_size_template,
                humanGigabytes(attached.sizeBytes),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "• " + stringResource(
                R.string.model_attached_at_template,
                formatAttachedAt(attached.attachedAtEpochMs),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // ВАЖНО: используем Material 3 паттерн `Icon (size=IconSize) +
        // Spacer (size=IconSpacing) + Text`, а НЕ `Icon + "  " + Text`:
        //  - "  " пробелы не управляются layout-движком (визуальный отступ
        //    разный для разных иконок);
        //  - SwapHoriz шире чем DeleteOutline по intrinsic content bounds,
        //    и без явного `Modifier.size` Button RowScope считает разную
        //    ширину content и центрирование съезжает — отсюда «кривая»
        //    кнопка «Заменить» относительно «Удалить».
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onReplace,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.SwapHoriz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(
                    text = stringResource(R.string.model_replace_button),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            OutlinedButton(
                onClick = onRequestDetach,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(
                    text = stringResource(R.string.model_detach_button),
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun formatAttachedAt(epochMs: Long): String {
    val instant = Instant.ofEpochMilli(epochMs)
    // `Locale.forLanguageTag` — BCP-47 эквивалент устаревшего `Locale(String)`.
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru"))
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}
