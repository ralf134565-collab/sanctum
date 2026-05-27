// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.settings.model.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pocketreflect.app.R
import com.pocketreflect.app.presentation.journal.components.SectionCard

/**
 * Интерактивная карточка с подробным объяснением работы локального ИИ,
 * вопросов приватности, офлайн-режима и влияния на батарею.
 */
@Composable
fun ModelInfoCard(
    modifier: Modifier = Modifier,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    SectionCard(
        title = stringResource(R.string.model_info_card_title),
        subtitle = stringResource(R.string.model_info_card_subtitle),
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null, // Без эффекта нажатия для сохранения спокойного UX
            onClick = { isExpanded = !isExpanded }
        ),
    ) {
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                InfoBlock(
                    title = stringResource(R.string.model_info_autonomy_title),
                    text = stringResource(R.string.model_info_autonomy_text)
                )
                InfoBlock(
                    title = stringResource(R.string.model_info_privacy_title),
                    text = stringResource(R.string.model_info_privacy_text)
                )
                InfoBlock(
                    title = stringResource(R.string.model_info_battery_title),
                    text = stringResource(R.string.model_info_battery_text)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { isExpanded = !isExpanded }
            ) {
                Text(
                    text = if (isExpanded) {
                        stringResource(R.string.model_info_card_hide)
                    } else {
                        stringResource(R.string.model_info_card_show)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.size(4.dp))
                Icon(
                    imageVector = if (isExpanded) {
                        Icons.Outlined.KeyboardArrowUp
                    } else {
                        Icons.Outlined.KeyboardArrowDown
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun InfoBlock(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
