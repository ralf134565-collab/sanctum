// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import com.pocketreflect.app.presentation.components.screenAtmosphereGradient
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pocketreflect.app.R
import com.pocketreflect.app.core.support.SupportLinks
import com.pocketreflect.app.presentation.journal.components.SectionCard
import kotlinx.coroutines.launch

/**
 * Хаб настроек: краткий список разделов с актуальными статусами.
 * Детальные секции вынесены на подэкраны, чтобы не перегружать один scroll.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenPrivacy: () -> Unit = {},
    onOpenAppearance: () -> Unit = {},
    onOpenRitual: () -> Unit = {},
    onOpenModelSettings: () -> Unit = {},
    onOpenData: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_screen_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
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
                .screenAtmosphereGradient(),
        ) {
            SettingsHubContent(
                padding = padding,
                state = state,
                onOpenUrl = { url ->
                    val opened = runCatching {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }.isSuccess
                    if (!opened) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.support_open_url_error),
                            )
                        }
                    }
                },
                onOpenPrivacy = onOpenPrivacy,
                onOpenAppearance = onOpenAppearance,
                onOpenRitual = onOpenRitual,
                onOpenModelSettings = onOpenModelSettings,
                onOpenData = onOpenData,
            )
        }
    }
}

@Composable
private fun SettingsHubContent(
    padding: PaddingValues,
    state: SettingsContract.State,
    onOpenUrl: (String) -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenRitual: () -> Unit,
    onOpenModelSettings: () -> Unit,
    onOpenData: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Элегантный манифест-эпиграф в самом верху экрана настроек
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_manifesto_line1),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.settings_manifesto_line2),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            )
            Text(
                text = stringResource(R.string.settings_manifesto_line3),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            )
        }

        SupportHubSection(onOpenUrl = onOpenUrl)
        SettingsHubRow(
            icon = Icons.Outlined.Shield,
            title = stringResource(R.string.settings_hub_privacy_title),
            summary = privacyHubSummary(state),
            onClick = onOpenPrivacy,
        )
        SettingsHubRow(
            icon = Icons.Outlined.Palette,
            title = stringResource(R.string.settings_hub_appearance_title),
            summary = appearanceHubSummary(state),
            onClick = onOpenAppearance,
        )
        SettingsHubRow(
            icon = Icons.Outlined.SelfImprovement,
            title = stringResource(R.string.settings_hub_ritual_title),
            summary = ritualHubSummary(state),
            onClick = onOpenRitual,
        )
        SettingsHubRow(
            icon = Icons.Outlined.AutoAwesome,
            title = stringResource(R.string.model_section_title),
            summary = modelHubSummary(state.attachedModel),
            onClick = onOpenModelSettings,
        )
        SettingsHubRow(
            icon = Icons.Outlined.Storage,
            title = stringResource(R.string.settings_hub_data_title),
            summary = stringResource(R.string.settings_hub_data_summary),
            onClick = onOpenData,
        )
    }
}

@Composable
private fun SupportHubSection(onOpenUrl: (String) -> Unit) {
    var bodyExpanded by rememberSaveable { mutableStateOf(false) }

    SectionCard(title = stringResource(R.string.support_section_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { bodyExpanded = !bodyExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.support_why_free),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (bodyExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            AnimatedVisibility(
                visible = bodyExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Text(
                    text = stringResource(R.string.support_section_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            val context = LocalContext.current
            val donateUrl = stringResource(R.string.support_donate_url)
            Button(
                onClick = {
                    onOpenUrl(donateUrl)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.support_learn_how))
            }
            OutlinedButton(
                onClick = { onOpenUrl(SupportLinks.GITHUB) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.support_github))
            }
        }
    }
}
