// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.history.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import com.pocketreflect.app.presentation.components.CalmLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pocketreflect.app.R
import com.pocketreflect.app.core.time.DateFormats
import com.pocketreflect.app.data.local.entity.JournalEntry
import com.pocketreflect.app.domain.model.MoodTag
import com.pocketreflect.app.presentation.journal.components.SectionCard
import com.pocketreflect.app.presentation.journal.components.ReadOnlyMoodTagChips
import com.pocketreflect.app.presentation.journal.components.displayLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDetailScreen(
    onBack: () -> Unit,
    viewModel: EntryDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                EntryDetailContract.Effect.EntryDeleted -> onBack()
                is EntryDetailContract.Effect.ShowError ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    val locale = DateFormats.javaLocale(LocalConfiguration.current)
                    Text(
                        text = state.entry?.let { DateFormats.shortDay(it.timestamp, locale) }
                            ?: stringResource(R.string.entry_detail_title_fallback),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when {
            state.isLoading -> LoadingState(innerPadding)
            state.entry == null -> EmptyState(innerPadding)
            else -> EntryBody(
                padding = innerPadding,
                entry = state.entry!!,
                onDeleteClick = { viewModel.onIntent(EntryDetailContract.Intent.RequestDelete) },
            )
        }
    }

    if (state.isConfirmingDelete) {
        FirstConfirmDialog(
            onCancel = { viewModel.onIntent(EntryDetailContract.Intent.CancelDelete) },
            onConfirm = { viewModel.onIntent(EntryDetailContract.Intent.ConfirmFirstStep) },
        )
    }
}

@Composable
private fun LoadingState(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        CalmLoadingIndicator()
    }
}

@Composable
private fun EmptyState(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.entry_detail_not_found),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EntryBody(
    padding: PaddingValues,
    entry: JournalEntry,
    onDeleteClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionCard(
            title = stringResource(R.string.entry_detail_mood_section),
        ) {
            if (entry.moodTags.isEmpty()) {
                Text(
                    text = stringResource(R.string.entry_detail_no_tags),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                ReadOnlyMoodTagChips(tags = entry.moodTags)
            }
        }

        if (entry.microWins.isNotBlank()) {
            SectionCard(title = stringResource(R.string.micro_wins_title)) {
                Text(
                    text = entry.microWins,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        if (entry.tomorrowTasks.isNotBlank()) {
            SectionCard(title = stringResource(R.string.tasks_title)) {
                Text(
                    text = entry.tomorrowTasks,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        if (entry.customFieldQuestion.isNotBlank() && entry.customFieldAnswer.isNotBlank()) {
            SectionCard(
                title = entry.customFieldQuestion,
            ) {
                Text(
                    text = entry.customFieldAnswer,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        SectionCard(title = stringResource(R.string.entry_detail_prompt_section), subtitle = entry.promptShown) {
            if (entry.reflection.isNotBlank()) {
                Text(
                    text = entry.reflection,
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                Text(
                    text = stringResource(R.string.entry_detail_no_reflection),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!entry.aiReflection.isNullOrBlank()) {
            SectionCard(title = stringResource(R.string.ai_response_title)) {
                Text(
                    text = entry.aiReflection,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        OutlinedButton(
            onClick = onDeleteClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(imageVector = Icons.Outlined.DeleteOutline, contentDescription = null)
            Text(
                text = stringResource(R.string.entry_detail_delete_button),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun FirstConfirmDialog(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.entry_detail_delete_first_title)) },
        text = {
            Text(text = stringResource(R.string.entry_detail_delete_first_body))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(R.string.entry_detail_delete_confirm_action),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
