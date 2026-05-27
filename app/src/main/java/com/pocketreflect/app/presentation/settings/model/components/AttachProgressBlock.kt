// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.settings.model.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pocketreflect.app.R
import com.pocketreflect.app.presentation.journal.components.SectionCard
import com.pocketreflect.app.presentation.settings.model.ModelSettingsContract

/**
 * Блок «идёт подключение модели».
 *
 * Сознательно **без процентов**: гонять цифру 0 → 100 на 2.6 GB-файле в
 * фоне приватного дневника — давление. Показываем human-readable «уже X из Y»,
 * чтобы пользователь видел движение, но не считал в уме оставшееся время.
 */
@Composable
fun AttachProgressBlock(
    progress: ModelSettingsContract.AttachProgress,
    modifier: Modifier = Modifier,
) {
    val subtitle = when (progress) {
        is ModelSettingsContract.AttachProgress.Copying ->
            stringResource(R.string.model_attaching_copying)
        ModelSettingsContract.AttachProgress.Verifying ->
            stringResource(R.string.model_attaching_verifying)
    }
    SectionCard(
        title = stringResource(R.string.model_attaching_card_title),
        subtitle = subtitle,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (progress) {
                is ModelSettingsContract.AttachProgress.Copying -> {
                    val total = progress.bytesTotal.coerceAtLeast(1L)
                    val fraction = (progress.bytesCopied.toFloat() / total.toFloat())
                        .coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = humanGigabytes(progress.bytesCopied),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ModelSettingsContract.AttachProgress.Verifying -> {
                    LinearProgressIndicator(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
