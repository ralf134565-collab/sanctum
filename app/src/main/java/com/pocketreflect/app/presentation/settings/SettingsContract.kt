// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.settings

import androidx.compose.runtime.Immutable
import com.pocketreflect.app.core.security.BiometricAvailability
import com.pocketreflect.app.data.repository.AttachedModel
import com.pocketreflect.app.core.locale.AppLanguage
import com.pocketreflect.app.data.repository.AppThemeMode
import com.pocketreflect.app.data.repository.AutoLockTimeout
import com.pocketreflect.app.domain.breathing.BreathingHapticIntensity
import com.pocketreflect.app.domain.breathing.BreathingPattern

object SettingsContract {

    @Immutable
    data class State(
        val isPrivacyExpanded: Boolean = false,

        val biometricLockEnabled: Boolean = false,
        val autoLockTimeout: AutoLockTimeout = AutoLockTimeout.DEFAULT,
        val screenshotProtectionEnabled: Boolean = true,
        val uiHapticEnabled: Boolean = true,
        val biometricStatus: BiometricAvailability.Status = BiometricAvailability.Status.Unavailable,

        val attachedModel: AttachedModel? = null,

        val isConfirmingWipe: Boolean = false,
        val isFinalConfirmingWipe: Boolean = false,
        val isWiping: Boolean = false,

        val themeMode: AppThemeMode = AppThemeMode.DEFAULT,
        val appLanguage: AppLanguage = AppLanguage.DEFAULT,

        val customJournalFieldEnabled: Boolean = false,
        val customJournalFieldQuestion: String = "",
        val customJournalFieldHint: String = "",

        val breathingPattern: BreathingPattern = BreathingPattern.DEFAULT,
        val breathingHapticEnabled: Boolean = true,
        val breathingHapticIntensity: BreathingHapticIntensity = BreathingHapticIntensity.DEFAULT,
        val breathingCycleCount: Int = 6,
    )

    sealed interface Intent {
        data object TogglePrivacyDetails : Intent

        data class ToggleBiometricLock(val enabled: Boolean) : Intent
        data class ToggleScreenshotProtection(val enabled: Boolean) : Intent
        data class ToggleUiHaptic(val enabled: Boolean) : Intent
        data class SetAutoLockTimeout(val timeout: AutoLockTimeout) : Intent

        data object RequestWipe : Intent
        data object ConfirmFirstStep : Intent
        data object CancelWipe : Intent
        data object ConfirmFinalWipe : Intent
        data class SetThemeMode(val mode: AppThemeMode) : Intent
        data class SetAppLanguage(val language: AppLanguage) : Intent

        data class ToggleCustomJournalField(val enabled: Boolean) : Intent
        data class SetCustomJournalFieldQuestion(val question: String) : Intent
        data class SetCustomJournalFieldHint(val hint: String) : Intent

        data class SetBreathingPattern(val pattern: BreathingPattern) : Intent
        data class ToggleBreathingHaptic(val enabled: Boolean) : Intent
        data class SetBreathingHapticIntensity(val intensity: BreathingHapticIntensity) : Intent
        data class SetBreathingCycleCount(val count: Int) : Intent
    }

    sealed interface Effect {
        data object WipeCompleted : Effect
        data class ShowError(val message: String) : Effect
        data object ShowBiometricUnavailable : Effect
        data object CustomFieldQuestionRequired : Effect
    }
}
