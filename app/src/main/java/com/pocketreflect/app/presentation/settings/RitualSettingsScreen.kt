// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pocketreflect.app.R
import com.pocketreflect.app.domain.ambient.AmbientMusicPolicy

@Composable
fun RitualSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var importTrackName by remember { mutableStateOf("") }
    var renameTrackId by remember { mutableStateOf<String?>(null) }
    var renameTrackName by remember { mutableStateOf("") }

    val importAmbientLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            importTrackName = suggestAmbientTrackName(uri)
        }
    }

    CollectSettingsEffects(viewModel, snackbarHostState)

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = {
                pendingImportUri = null
                importTrackName = ""
            },
            title = { Text(stringResource(R.string.ambient_music_import_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = importTrackName,
                    onValueChange = {
                        importTrackName = it.take(AmbientMusicPolicy.MAX_DISPLAY_NAME_LENGTH)
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = { Text(stringResource(R.string.ambient_music_track_name_label)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.importAmbientTrack(uri, importTrackName)
                        pendingImportUri = null
                        importTrackName = ""
                    },
                ) {
                    Text(stringResource(R.string.action_save_general))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingImportUri = null
                        importTrackName = ""
                    },
                ) {
                    Text(stringResource(R.string.chat_cancel))
                }
            },
        )
    }

    renameTrackId?.let { trackId ->
        AlertDialog(
            onDismissRequest = {
                renameTrackId = null
                renameTrackName = ""
            },
            title = { Text(stringResource(R.string.ambient_music_rename_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = renameTrackName,
                    onValueChange = {
                        renameTrackName = it.take(AmbientMusicPolicy.MAX_DISPLAY_NAME_LENGTH)
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = { Text(stringResource(R.string.ambient_music_track_name_label)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renameAmbientCustomTrack(trackId, renameTrackName)
                        renameTrackId = null
                        renameTrackName = ""
                    },
                ) {
                    Text(stringResource(R.string.action_save_general))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        renameTrackId = null
                        renameTrackName = ""
                    },
                ) {
                    Text(stringResource(R.string.chat_cancel))
                }
            },
        )
    }

    SettingsSubScreenScaffold(
        title = stringResource(R.string.settings_hub_ritual_title),
        onBack = onBack,
        snackbarHostState = snackbarHostState,
    ) { padding ->
        RitualSettingsContent(
            padding = padding,
            state = state,
            onIntent = viewModel::onIntent,
            onImportAmbient = {
                importAmbientLauncher.launch(
                    arrayOf("audio/ogg", "application/ogg", "audio/mpeg", "audio/mp3", "audio/*"),
                )
            },
            onRenameAmbient = { trackId ->
                val track = state.ambientMusicCustomTracks.firstOrNull { it.id == trackId } ?: return@RitualSettingsContent
                renameTrackId = trackId
                renameTrackName = track.displayName
            },
        )
    }
}

private fun suggestAmbientTrackName(uri: Uri): String {
    val raw = uri.lastPathSegment ?: return ""
    val decoded = Uri.decode(raw).substringAfterLast('/')
    return decoded.substringBeforeLast('.').take(AmbientMusicPolicy.MAX_DISPLAY_NAME_LENGTH)
}

@Composable
private fun RitualSettingsContent(
    padding: PaddingValues,
    state: SettingsContract.State,
    onIntent: (SettingsContract.Intent) -> Unit,
    onImportAmbient: () -> Unit,
    onRenameAmbient: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TodayTabRitualSection(
            breathingEnabled = state.breathingBridgeEnabled,
            sandFlowEnabled = state.sandFlowEnabled,
            customFieldEnabled = state.customJournalFieldEnabled,
            ambientMusicEnabled = state.ambientMusicEnabled,
            onToggleBreathing = { onIntent(SettingsContract.Intent.ToggleBreathingBridge(it)) },
            onToggleSandFlow = { onIntent(SettingsContract.Intent.ToggleSandFlow(it)) },
            onToggleCustomField = { onIntent(SettingsContract.Intent.ToggleCustomJournalField(it)) },
            onToggleAmbientMusic = { onIntent(SettingsContract.Intent.ToggleAmbientMusic(it)) },
        )
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
        AmbientMusicSettingsSection(
            musicEnabled = state.ambientMusicEnabled,
            customTracks = state.ambientMusicCustomTracks,
            onImport = onImportAmbient,
            onRename = onRenameAmbient,
            onRemove = { onIntent(SettingsContract.Intent.RemoveAmbientCustomTrack(it)) },
        )
        CustomJournalFieldSection(
            question = state.customJournalFieldQuestion,
            hint = state.customJournalFieldHint,
            onQuestionChange = { onIntent(SettingsContract.Intent.SetCustomJournalFieldQuestion(it)) },
            onHintChange = { onIntent(SettingsContract.Intent.SetCustomJournalFieldHint(it)) },
        )
        SandFlowSettingsSection(
            breathingSyncEnabled = state.sandFlowBreathingSyncEnabled,
            difficulty = state.sandFlowDifficulty,
            onToggleBreathingSync = { onIntent(SettingsContract.Intent.ToggleSandFlowBreathingSync(it)) },
            onDifficultySelected = { onIntent(SettingsContract.Intent.SetSandFlowDifficulty(it)) },
        )
    }
}
