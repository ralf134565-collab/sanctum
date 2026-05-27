// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pocketreflect.app.R
import com.pocketreflect.app.domain.ai.AiEngineStatus

/**
 * Информационный chip: какой режим ИИ сейчас активен на устройстве.
 */
@Composable
fun AiEngineStatusChip(
    status: AiEngineStatus,
    modifier: Modifier = Modifier,
) {
    val label = when (status) {
        AiEngineStatus.REAL_READY -> stringResource(R.string.ai_status_local_model)
        AiEngineStatus.WARMING -> stringResource(R.string.ai_status_warming)
        AiEngineStatus.MODEL_OFFLINE -> stringResource(R.string.ai_status_model_offline)
        AiEngineStatus.FALLBACK -> stringResource(R.string.ai_status_fallback)
    }
    val colors = when (status) {
        AiEngineStatus.REAL_READY -> AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        AiEngineStatus.WARMING -> AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        AiEngineStatus.MODEL_OFFLINE,
        AiEngineStatus.FALLBACK,
        -> AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    AssistChip(
        onClick = {},
        enabled = false,
        modifier = modifier,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        },
        colors = colors,
    )
}

/**
 * Компактная иконка-индикатор статуса локального ИИ для интеграции в TopAppBar.
 */
@Composable
fun AiEngineStatusIcon(
    status: AiEngineStatus,
    modifier: Modifier = Modifier,
) {
    val tint = when (status) {
        AiEngineStatus.REAL_READY -> MaterialTheme.colorScheme.secondary
        AiEngineStatus.WARMING -> MaterialTheme.colorScheme.tertiary
        AiEngineStatus.MODEL_OFFLINE,
        AiEngineStatus.FALLBACK,
        -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }
    val description = when (status) {
        AiEngineStatus.REAL_READY -> stringResource(R.string.ai_status_local_model)
        AiEngineStatus.WARMING -> stringResource(R.string.ai_status_warming)
        AiEngineStatus.MODEL_OFFLINE -> stringResource(R.string.ai_status_model_offline)
        AiEngineStatus.FALLBACK -> stringResource(R.string.ai_status_fallback)
    }
    Icon(
        imageVector = Icons.Outlined.AutoAwesome,
        contentDescription = description,
        tint = tint,
        modifier = modifier
            .size(24.dp)
            .padding(horizontal = 2.dp)
    )
}

/**
 * Диалог-пояснение статуса локальной ИИ-модели с переходом в настройки.
 */
@Composable
fun AiStatusDialog(
    status: AiEngineStatus,
    onDismiss: () -> Unit,
    onNavigateToModelSettings: () -> Unit,
) {
    val title = stringResource(R.string.ai_status_dialog_title)
    val body = when (status) {
        AiEngineStatus.REAL_READY -> stringResource(R.string.ai_status_dialog_ready)
        AiEngineStatus.WARMING -> stringResource(R.string.ai_status_dialog_warming)
        AiEngineStatus.MODEL_OFFLINE -> stringResource(R.string.ai_status_dialog_offline)
        AiEngineStatus.FALLBACK -> stringResource(R.string.ai_status_dialog_fallback)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onNavigateToModelSettings()
                }
            ) {
                Text(stringResource(R.string.ai_status_dialog_button_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_understood))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
