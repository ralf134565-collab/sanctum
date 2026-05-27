// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.settings.model.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pocketreflect.app.R

/**
 * Раскрываемая инструкция «Где взять файл?».
 *
 * Сам блок не открывает URL — только сообщает наружу через колбэк, чтобы
 * ViewModel могла перевести это в `Effect.OpenExternalUrl`, а UI — в
 * `Intent.ACTION_VIEW` для внешнего браузера.
 *
 * Инвариант приватности: приложение **не** загружает файл из сети. URL
 * передаётся системе через `ACTION_VIEW`, и физически файл качает чужой
 * браузер. В манифесте `android.permission.INTERNET` отсутствует — этот
 * композайбл просто не сможет открыть никакой сетевой сокет, даже если
 * захотел бы.
 */
@Composable
fun ModelSourcesBlock(
    primaryUrl: String,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.model_sources_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = { onOpen(primaryUrl) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "  " + stringResource(R.string.model_sources_open_hf),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
