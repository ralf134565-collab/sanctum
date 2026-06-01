// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.insights

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pocketreflect.app.R
import com.pocketreflect.app.presentation.components.CalmLoadingIndicator
import com.pocketreflect.app.presentation.components.screenAtmosphereGradient
import com.pocketreflect.insights.ui.InsightsScreenContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    onOpenEntry: (Long) -> Unit,
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is InsightsContract.Effect.OpenEntry -> onOpenEntry(effect.id)
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
                    Column {
                        Text(
                            text = stringResource(R.string.insights_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = stringResource(R.string.insights_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .screenAtmosphereGradient(),
        ) {
            when {
                state.isLoading -> CalmLoadingIndicator(Modifier.fillMaxSize())
                state.snapshot == null -> Text(
                    text = stringResource(R.string.insights_load_error),
                    modifier = Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> {
                    val snapshot = state.snapshot!!
                    InsightsScreenContent(
                        snapshot = snapshot,
                        english = state.english,
                        expandedCards = state.expandedCards,
                        highlightedTag = state.highlightedTag,
                        highlightedPatternId = state.highlightedPatternId,
                        sheetRequest = state.sheetRequest,
                        onWindowDaysChange = { days ->
                            viewModel.onIntent(InsightsContract.Intent.SetWindowDays(days))
                        },
                        onExpandCards = {
                            viewModel.onIntent(InsightsContract.Intent.ExpandCards)
                        },
                        onPatternClick = { id ->
                            viewModel.onIntent(InsightsContract.Intent.PatternClick(id))
                        },
                        onTagHighlight = { /* highlight driven by tag click */ },
                        onPolarityClick = { polarity ->
                            viewModel.onIntent(InsightsContract.Intent.PolarityClick(polarity))
                        },
                        onTagClick = { tag ->
                            viewModel.onIntent(InsightsContract.Intent.TagClick(tag))
                        },
                        onSheetDismiss = {
                            viewModel.onIntent(InsightsContract.Intent.DismissSheet)
                        },
                        onOpenEntry = viewModel::onOpenEntry,
                    )
                }
            }
        }
    }
}
