// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pocketreflect.app.R
import com.pocketreflect.app.core.security.BiometricAvailability
import com.pocketreflect.app.data.model.ModelManifest
import com.pocketreflect.app.data.repository.AttachedModel

@Composable
internal fun privacyHubSummary(state: SettingsContract.State): String {
    val lockPart = if (
        state.biometricLockEnabled &&
        state.biometricStatus is BiometricAvailability.Status.Available
    ) {
        stringResource(
            R.string.settings_hub_privacy_lock_on,
            state.autoLockTimeout.label(),
        )
    } else {
        stringResource(R.string.settings_hub_privacy_lock_off)
    }
    val screenshotPart = if (state.screenshotProtectionEnabled) {
        stringResource(R.string.settings_hub_privacy_screenshot_on)
    } else {
        stringResource(R.string.settings_hub_privacy_screenshot_off)
    }
    return "$lockPart · $screenshotPart"
}

@Composable
internal fun appearanceHubSummary(state: SettingsContract.State): String =
    stringResource(
        R.string.settings_hub_appearance_summary,
        state.themeMode.label(),
        state.appLanguage.label(),
    )

@Composable
internal fun ritualHubSummary(state: SettingsContract.State): String {
    val customPart = if (state.customJournalFieldEnabled) {
        stringResource(R.string.settings_hub_ritual_custom_on)
    } else {
        stringResource(R.string.settings_hub_ritual_custom_off)
    }
    return stringResource(
        R.string.settings_hub_ritual_summary,
        state.breathingPattern.label(),
        state.breathingCycleCount,
        customPart,
    )
}

@Composable
internal fun modelHubSummary(attached: AttachedModel?): String =
    if (attached == null) {
        stringResource(R.string.model_section_subtitle_unattached)
    } else {
        stringResource(
            R.string.model_section_subtitle_attached_template,
            ModelManifest.entryOf(attached.variant).displayName,
        )
    }
