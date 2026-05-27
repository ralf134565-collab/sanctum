// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pocketreflect.app.R
import androidx.compose.material3.SnackbarHostState
import com.pocketreflect.app.core.haptic.rememberHapticFeedback

@Composable
fun AppearanceSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = rememberHapticFeedback(state.uiHapticEnabled)

    CollectSettingsEffects(viewModel, snackbarHostState)

    SettingsSubScreenScaffold(
        title = stringResource(R.string.settings_hub_appearance_title),
        onBack = onBack,
        snackbarHostState = snackbarHostState,
    ) { padding ->
        AppearanceSettingsContent(
            padding = padding,
            state = state,
            onIntent = { intent ->
                if (intent is SettingsContract.Intent.ToggleUiHaptic) {
                    haptic.tick()
                }
                viewModel.onIntent(intent)
            },
        )
    }
}

@Composable
private fun AppearanceSettingsContent(
    padding: PaddingValues,
    state: SettingsContract.State,
    onIntent: (SettingsContract.Intent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        InterfaceSection(
            themeMode = state.themeMode,
            onThemeSelected = { onIntent(SettingsContract.Intent.SetThemeMode(it)) },
            appLanguage = state.appLanguage,
            onLanguageSelected = { onIntent(SettingsContract.Intent.SetAppLanguage(it)) },
            uiHapticEnabled = state.uiHapticEnabled,
            onToggleUiHaptic = { onIntent(SettingsContract.Intent.ToggleUiHaptic(it)) },
        )
    }
}
