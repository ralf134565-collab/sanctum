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

@Composable
fun RitualSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    CollectSettingsEffects(viewModel, snackbarHostState)

    SettingsSubScreenScaffold(
        title = stringResource(R.string.settings_hub_ritual_title),
        onBack = onBack,
        snackbarHostState = snackbarHostState,
    ) { padding ->
        RitualSettingsContent(
            padding = padding,
            state = state,
            onIntent = viewModel::onIntent,
        )
    }
}

@Composable
private fun RitualSettingsContent(
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
        BreathingRitualSection(
            pattern = state.breathingPattern,
            hapticEnabled = state.breathingHapticEnabled,
            hapticIntensity = state.breathingHapticIntensity,
            cycleCount = state.breathingCycleCount,
            onPatternSelected = { onIntent(SettingsContract.Intent.SetBreathingPattern(it)) },
            onToggleHaptic = { onIntent(SettingsContract.Intent.ToggleBreathingHaptic(it)) },
            onHapticIntensitySelected = {
                onIntent(SettingsContract.Intent.SetBreathingHapticIntensity(it))
            },
            onCycleCountSelected = { onIntent(SettingsContract.Intent.SetBreathingCycleCount(it)) },
        )
        CustomJournalFieldSection(
            enabled = state.customJournalFieldEnabled,
            question = state.customJournalFieldQuestion,
            hint = state.customJournalFieldHint,
            onToggle = { onIntent(SettingsContract.Intent.ToggleCustomJournalField(it)) },
            onQuestionChange = { onIntent(SettingsContract.Intent.SetCustomJournalFieldQuestion(it)) },
            onHintChange = { onIntent(SettingsContract.Intent.SetCustomJournalFieldHint(it)) },
        )
    }
}
