// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.settings

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.pocketreflect.app.R

@Composable
internal fun CollectSettingsEffects(
    viewModel: SettingsViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val biometricUnavailableMessage = stringResource(R.string.security_lock_unavailable)
    val customFieldQuestionRequiredMessage =
        stringResource(R.string.custom_journal_field_question_required)
    val wipeCompletedMessage = stringResource(R.string.settings_wipe_completed)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SettingsContract.Effect.WipeCompleted ->
                    snackbarHostState.showSnackbar(wipeCompletedMessage)
                is SettingsContract.Effect.ShowError ->
                    snackbarHostState.showSnackbar(effect.message)
                SettingsContract.Effect.ShowBiometricUnavailable ->
                    snackbarHostState.showSnackbar(biometricUnavailableMessage)
                SettingsContract.Effect.CustomFieldQuestionRequired ->
                    snackbarHostState.showSnackbar(customFieldQuestionRequiredMessage)
            }
        }
    }
}
