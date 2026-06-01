// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pocketreflect.app.R

@Composable
fun ChatSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    CollectSettingsEffects(viewModel, snackbarHostState)

    SettingsSubScreenScaffold(
        title = stringResource(R.string.settings_hub_chat_title),
        onBack = onBack,
        snackbarHostState = snackbarHostState,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CustomChatPersonaSection(
                enabled = state.chatCustomPersonaEnabled,
                stylePrompt = state.chatCustomPersonaPrompt,
                onToggle = { viewModel.onIntent(SettingsContract.Intent.ToggleChatCustomPersona(it)) },
                onStylePromptChange = {
                    viewModel.onIntent(SettingsContract.Intent.SetChatCustomPersonaPrompt(it))
                },
                onApplyTemplate = {
                    viewModel.onIntent(SettingsContract.Intent.ApplyChatCustomPersonaTemplate(it))
                },
            )
        }
    }
}
