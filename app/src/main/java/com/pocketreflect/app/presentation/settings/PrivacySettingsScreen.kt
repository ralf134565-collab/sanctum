// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pocketreflect.app.R
import androidx.compose.material3.SnackbarHostState
import com.pocketreflect.app.core.haptic.rememberHapticFeedback

@Composable
fun PrivacySettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = rememberHapticFeedback(state.uiHapticEnabled)

    CollectSettingsEffects(viewModel, snackbarHostState)

    SettingsSubScreenScaffold(
        title = stringResource(R.string.settings_hub_privacy_title),
        onBack = onBack,
        snackbarHostState = snackbarHostState,
    ) { padding ->
        PrivacySettingsContent(
            padding = padding,
            state = state,
            onIntent = { intent ->
                when (intent) {
                    is SettingsContract.Intent.ToggleScreenshotProtection,
                    is SettingsContract.Intent.ToggleBiometricLock,
                    is SettingsContract.Intent.SetAutoLockTimeout -> {
                        haptic.tick()
                    }
                    else -> {}
                }
                viewModel.onIntent(intent)
            },
        )
    }

    if (state.isConfirmingWipe) {
        FirstWipeDialog(
            onCancel = { viewModel.onIntent(SettingsContract.Intent.CancelWipe) },
            onContinue = { viewModel.onIntent(SettingsContract.Intent.ConfirmFirstStep) },
        )
    }
    if (state.isFinalConfirmingWipe) {
        FinalWipeDialog(
            isWiping = state.isWiping,
            onCancel = { viewModel.onIntent(SettingsContract.Intent.CancelWipe) },
            onConfirm = { viewModel.onIntent(SettingsContract.Intent.ConfirmFinalWipe) },
        )
    }
}

@Composable
private fun PrivacySettingsContent(
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
        PrivacyAndSecuritySection(
            isPrivacyExpanded = state.isPrivacyExpanded,
            onTogglePrivacy = { onIntent(SettingsContract.Intent.TogglePrivacyDetails) },
            screenshotProtectionEnabled = state.screenshotProtectionEnabled,
            onToggleScreenshotProtection = {
                onIntent(SettingsContract.Intent.ToggleScreenshotProtection(it))
            },
            isEnabled = state.biometricLockEnabled,
            biometricStatus = state.biometricStatus,
            timeout = state.autoLockTimeout,
            onToggleLock = { onIntent(SettingsContract.Intent.ToggleBiometricLock(it)) },
            onTimeoutSelected = { onIntent(SettingsContract.Intent.SetAutoLockTimeout(it)) },
        )
        DangerZoneSection(
            onRequestWipe = { onIntent(SettingsContract.Intent.RequestWipe) },
        )
        Spacer(modifier = Modifier.height(16.dp))
        val uriHandler = LocalUriHandler.current
        val context = LocalContext.current
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    val isRussian = context.resources.configuration.locales[0].language == "ru"
                    val url = if (isRussian) {
                        "https://github.com/ralf134565-collab/sanctum/blob/main/PRIVACY.ru.md"
                    } else {
                        "https://github.com/ralf134565-collab/sanctum/blob/main/PRIVACY.md"
                    }
                    try {
                        uriHandler.openUri(url)
                    } catch (e: Exception) {
                        // Безопасный фоллбек на случай отсутствия браузера
                    }
                }
            ) {
                Text(
                    text = stringResource(R.string.settings_privacy_policy),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
