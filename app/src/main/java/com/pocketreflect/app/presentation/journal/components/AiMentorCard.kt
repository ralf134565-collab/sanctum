// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.journal.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.pocketreflect.app.presentation.components.CalmTypingIndicator
import com.pocketreflect.app.ui.theme.PocketReflectShapes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pocketreflect.app.R
import com.pocketreflect.app.domain.ai.AiEngineStatus

/**
 * «Отклик ментора» — мягкая ambient-карточка с откликом от локальной Gemma 4.
 *
 * Инференс запускается только по [onRequestClick], не при выборе тегов.
 */
@Composable
fun AiMentorCard(
    isThinking: Boolean,
    text: String?,
    supportiveMode: Boolean,
    aiEngineStatus: AiEngineStatus,
    canRequest: Boolean,
    requestButtonLabel: String,
    hintNoTags: String,
    hintRequest: String,
    onRequestClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (supportiveMode) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val onContainer = if (supportiveMode) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    val thinkingLabel = when (aiEngineStatus) {
        AiEngineStatus.REAL_READY,
        AiEngineStatus.WARMING,
        -> stringResource(R.string.ai_thinking_local)
        AiEngineStatus.MODEL_OFFLINE,
        AiEngineStatus.FALLBACK,
        -> stringResource(R.string.ai_thinking_offline)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = container,
        contentColor = onContainer,
        shape = PocketReflectShapes.Card,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = if (supportiveMode) {
                    stringResource(R.string.ai_mentor_beside_title)
                } else {
                    stringResource(R.string.ai_response_title)
                },
                style = MaterialTheme.typography.titleMedium,
            )

            AnimatedVisibility(visible = isThinking, enter = fadeIn(), exit = fadeOut()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CalmTypingIndicator()
                    Text(
                        text = thinkingLabel,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            AnimatedVisibility(visible = !isThinking && !text.isNullOrBlank(), enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = text.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            AnimatedVisibility(
                visible = !isThinking && text.isNullOrBlank() && canRequest,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Text(
                    text = hintRequest,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            AnimatedVisibility(
                visible = !isThinking && !canRequest,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Text(
                    text = hintNoTags,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            AnimatedVisibility(visible = canRequest, enter = fadeIn(), exit = fadeOut()) {
                FilledTonalButton(
                    onClick = onRequestClick,
                    enabled = canRequest,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = stringResource(R.string.cd_mentor_ai_icon),
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "  $requestButtonLabel",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}
