// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.settings.model.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pocketreflect.app.R
import com.pocketreflect.app.data.model.ModelManifest
import com.pocketreflect.app.data.model.ModelVariant
import com.pocketreflect.app.presentation.journal.components.SectionCard

/**
 * Карточка варианта модели (E2B / E4B) на экране подключения.
 *
 * Намеренно показываем только сухие характеристики — размер, RAM, латентность.
 * Никаких оценочных слов («лучше», «хуже»). Empathic UX: пользователь сам
 * соотносит характеристики со своим устройством.
 */
@Composable
fun ModelVariantCard(
    variant: ModelVariant,
    entry: ModelManifest.Entry,
    isSourcesExpanded: Boolean,
    onToggleSources: () -> Unit,
    onOpenSource: (String) -> Unit,
    onAttach: () -> Unit,
    isAttachInProgress: Boolean,
    modifier: Modifier = Modifier,
) {
    val subtitle = if (entry.recommended) {
        stringResource(R.string.model_variant_recommended_subtitle)
    } else {
        stringResource(R.string.model_variant_advanced_subtitle)
    }
    SectionCard(
        title = entry.displayName,
        subtitle = subtitle,
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BulletLine(
                text = stringResource(
                    R.string.model_variant_size_template,
                    humanGigabytes(entry.expectedSizeBytes),
                ),
            )
            val ramStr = stringResource(R.string.model_variant_ram_value, entry.minRamGb)
            BulletLine(
                text = stringResource(
                    R.string.model_variant_ram_template,
                    ramStr,
                ),
            )
            val latencyStr = stringResource(R.string.model_variant_latency_value, entry.latencyMinSec, entry.latencyMaxSec)
            BulletLine(
                text = stringResource(
                    R.string.model_variant_latency_template,
                    latencyStr,
                ),
            )
        }

        TextButton(onClick = onToggleSources) {
            Text(
                text = if (isSourcesExpanded) {
                    stringResource(R.string.model_sources_block_collapse)
                } else {
                    stringResource(R.string.model_sources_block_title)
                },
            )
        }

        AnimatedVisibility(visible = isSourcesExpanded) {
            ModelSourcesBlock(
                primaryUrl = entry.primarySourceUrl,
                onOpen = onOpenSource,
            )
        }

        Button(
            onClick = onAttach,
            enabled = !isAttachInProgress,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(imageVector = Icons.Outlined.FileDownload, contentDescription = null)
            Text(
                text = "  " + stringResource(R.string.model_attach_button),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun BulletLine(text: String) {
    Text(
        text = "• $text",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
