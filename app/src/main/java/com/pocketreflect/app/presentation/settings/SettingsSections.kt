// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NoPhotography
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketreflect.app.R
import com.pocketreflect.app.core.locale.AppLanguage
import com.pocketreflect.app.core.security.BiometricAvailability
import com.pocketreflect.app.data.model.ModelManifest
import com.pocketreflect.app.data.repository.AppThemeMode
import com.pocketreflect.app.data.repository.AttachedModel
import com.pocketreflect.app.data.repository.AutoLockTimeout
import com.pocketreflect.app.domain.breathing.BreathingHapticIntensity
import com.pocketreflect.app.domain.breathing.BreathingPattern
import com.pocketreflect.app.presentation.components.SegmentedControl
import com.pocketreflect.app.presentation.journal.components.SectionCard
import com.pocketreflect.app.ui.theme.PocketReflectShapes

/**
 * Секция «ИИ-ментор»: статус подключённой локальной модели и переход к её
 * выбору. Сама секция держит **только** статус (подключена / не подключена) —
 * вся логика выбора, копирования и SHA-256 верификации живёт в отдельном
 * [com.pocketreflect.app.presentation.settings.model.ModelSettingsScreen],
 * чтобы экран настроек не разрастался.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PrivacyAndSecuritySection(
    isPrivacyExpanded: Boolean,
    onTogglePrivacy: () -> Unit,
    screenshotProtectionEnabled: Boolean,
    onToggleScreenshotProtection: (Boolean) -> Unit,
    isEnabled: Boolean,
    biometricStatus: BiometricAvailability.Status,
    timeout: AutoLockTimeout,
    onToggleLock: (Boolean) -> Unit,
    onTimeoutSelected: (AutoLockTimeout) -> Unit,
) {
    SectionCard(
        title = stringResource(R.string.privacy_section_title),
        subtitle = stringResource(R.string.privacy_section_subtitle),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = stringResource(R.string.cd_privacy_on_device),
                tint = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = stringResource(R.string.privacy_on_device),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        TextButton(onClick = onTogglePrivacy) {
            Text(
                if (isPrivacyExpanded) {
                    stringResource(R.string.privacy_hide_details)
                } else {
                    stringResource(R.string.privacy_show_details)
                },
            )
        }
        AnimatedVisibility(visible = isPrivacyExpanded) {
            Text(
                text = stringResource(R.string.privacy_details_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.NoPhotography,
                contentDescription = stringResource(R.string.cd_screenshot_protection),
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 2.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.screenshot_protection_switch),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.screenshot_protection_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Switch(
                checked = screenshotProtectionEnabled,
                onCheckedChange = onToggleScreenshotProtection,
                modifier = Modifier.padding(top = 2.dp),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )

        val isAvailable = biometricStatus is BiometricAvailability.Status.Available
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = stringResource(R.string.cd_security_lock),
                tint = if (isAvailable) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.security_lock_switch),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isAvailable) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Switch(
                checked = isEnabled && isAvailable,
                enabled = isAvailable,
                onCheckedChange = onToggleLock,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
        if (!isAvailable) {
            Text(
                text = stringResource(R.string.security_lock_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AnimatedVisibility(visible = isEnabled && isAvailable) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.auto_lock_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AutoLockTimeout.entries.forEach { option ->
                        FilterChip(
                            selected = option == timeout,
                            onClick = { onTimeoutSelected(option) },
                            label = { Text(option.label()) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AutoLockTimeout.label(): String = when (this) {
    AutoLockTimeout.IMMEDIATELY -> stringResource(R.string.auto_lock_immediately)
    AutoLockTimeout.THIRTY_SECONDS -> stringResource(R.string.auto_lock_30_seconds)
    AutoLockTimeout.ONE_MINUTE -> stringResource(R.string.auto_lock_1_minute)
    AutoLockTimeout.FIVE_MINUTES -> stringResource(R.string.auto_lock_5_minutes)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun InterfaceSection(
    themeMode: AppThemeMode,
    onThemeSelected: (AppThemeMode) -> Unit,
    appLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    uiHapticEnabled: Boolean,
    onToggleUiHaptic: (Boolean) -> Unit,
) {
    SectionCard(
        title = stringResource(R.string.appearance_section_title),
        subtitle = stringResource(R.string.appearance_section_subtitle),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Palette,
                contentDescription = stringResource(R.string.cd_appearance_theme),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.appearance_theme_label),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        SegmentedControl(
            options = AppThemeMode.entries,
            selected = themeMode,
            onSelect = onThemeSelected,
            label = { it.label() },
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Language,
                contentDescription = stringResource(R.string.cd_app_language),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.language_label),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        SegmentedControl(
            options = AppLanguage.entries,
            selected = appLanguage,
            onSelect = onLanguageSelected,
            label = { it.label() },
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Vibration,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.security_haptic_switch),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.security_haptic_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Switch(
                checked = uiHapticEnabled,
                onCheckedChange = onToggleUiHaptic,
                modifier = Modifier.padding(top = 2.dp),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )

        var showPhilosophyDialog by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showPhilosophyDialog = true },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.philosophy_card_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.philosophy_card_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        if (showPhilosophyDialog) {
            PhilosophyDialog(onDismiss = { showPhilosophyDialog = false })
        }
    }
}

/**
 * Полноэкранный диалог с философией, методологией и практическим руководством.
 */
@Composable
internal fun PhilosophyDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = stringResource(R.string.philosophy_dialog_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.action_cancel),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Section 1: Philosophy
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.philosophy_section_philosophy_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.philosophy_section_philosophy_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Section 2: Science
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.philosophy_section_science_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.philosophy_section_science_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Section 3: Privacy
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.philosophy_section_privacy_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.philosophy_section_privacy_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Section 4: Guide
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.philosophy_section_guide_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.philosophy_section_guide_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = stringResource(R.string.action_understood),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
internal fun AppThemeMode.label(): String = when (this) {
    AppThemeMode.DARK -> stringResource(R.string.appearance_theme_dark)
    AppThemeMode.LIGHT -> stringResource(R.string.appearance_theme_light)
}

@Composable
internal fun AppLanguage.label(): String = when (this) {
    AppLanguage.SYSTEM -> stringResource(R.string.language_system)
    AppLanguage.RU -> stringResource(R.string.language_russian)
    AppLanguage.EN -> stringResource(R.string.language_english)
}

@Composable
internal fun AiMentorSection(
    attached: AttachedModel?,
    onOpenModelSettings: () -> Unit,
) {
    val subtitle = if (attached == null) {
        stringResource(R.string.model_section_subtitle_unattached)
    } else {
        stringResource(
            R.string.model_section_subtitle_attached_template,
            ModelManifest.entryOf(attached.variant).displayName,
        )
    }
    SectionCard(
        title = stringResource(R.string.model_section_title),
        subtitle = subtitle,
    ) {
        OutlinedButton(
            onClick = onOpenModelSettings,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = stringResource(R.string.cd_open_model_settings),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.model_section_open_button),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = stringResource(R.string.cd_navigate_forward),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

internal const val CUSTOM_JOURNAL_FIELD_QUESTION_MAX = 120
internal const val CUSTOM_JOURNAL_FIELD_HINT_MAX = 240

internal val BREATHING_CYCLE_OPTIONS = listOf(4, 6, 8)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BreathingRitualSection(
    pattern: BreathingPattern,
    hapticEnabled: Boolean,
    hapticIntensity: BreathingHapticIntensity,
    cycleCount: Int,
    onPatternSelected: (BreathingPattern) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onHapticIntensitySelected: (BreathingHapticIntensity) -> Unit,
    onCycleCountSelected: (Int) -> Unit,
) {
    SectionCard(
        title = stringResource(R.string.breathing_section_title),
        subtitle = stringResource(R.string.breathing_section_subtitle),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.SelfImprovement,
                contentDescription = stringResource(R.string.cd_breathing_ritual),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.breathing_pattern_label),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        BreathingPatternSelector(
            pattern = pattern,
            onPatternSelected = onPatternSelected,
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.breathing_cycle_count_label),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BREATHING_CYCLE_OPTIONS.forEach { option ->
                FilterChip(
                    selected = option == cycleCount,
                    onClick = { onCycleCountSelected(option) },
                    label = {
                        Text(stringResource(R.string.breathing_cycle_count_option, option))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.breathing_haptic_label),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.breathing_haptic_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = hapticEnabled,
                onCheckedChange = onToggleHaptic,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }

        AnimatedVisibility(visible = hapticEnabled && pattern == BreathingPattern.RESONANT) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.breathing_haptic_intensity_label),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.breathing_haptic_intensity_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SegmentedControl(
                    options = BreathingHapticIntensity.entries,
                    selected = hapticIntensity,
                    onSelect = onHapticIntensitySelected,
                    label = { it.label() },
                )
            }
        }
    }
}

@Composable
private fun BreathingPatternSelector(
    pattern: BreathingPattern,
    onPatternSelected: (BreathingPattern) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BreathingPattern.entries.forEach { option ->
            val selected = option == pattern
            Surface(
                onClick = { onPatternSelected(option) },
                modifier = Modifier.fillMaxWidth(),
                shape = PocketReflectShapes.Chip,
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                },
                border = BorderStroke(
                    width = 1.dp,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                    },
                ),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = option.title(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    Text(
                        text = option.detail(),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun BreathingHapticIntensity.label(): String = when (this) {
    BreathingHapticIntensity.GENTLE -> stringResource(R.string.breathing_haptic_intensity_gentle)
    BreathingHapticIntensity.MODERATE -> stringResource(R.string.breathing_haptic_intensity_moderate)
}

@Composable
internal fun BreathingPattern.title(): String = when (this) {
    BreathingPattern.RESONANT -> stringResource(R.string.breathing_pattern_resonant_title)
    BreathingPattern.BOX -> stringResource(R.string.breathing_pattern_box_title)
}

@Composable
internal fun BreathingPattern.detail(): String = when (this) {
    BreathingPattern.RESONANT -> stringResource(R.string.breathing_pattern_resonant_detail)
    BreathingPattern.BOX -> stringResource(R.string.breathing_pattern_box_detail)
}

@Composable
internal fun BreathingPattern.label(): String = title()

@Composable
internal fun CustomJournalFieldSection(
    enabled: Boolean,
    question: String,
    hint: String,
    onToggle: (Boolean) -> Unit,
    onQuestionChange: (String) -> Unit,
    onHintChange: (String) -> Unit,
) {
    SectionCard(
        title = stringResource(R.string.custom_journal_field_section_title),
        subtitle = stringResource(R.string.custom_journal_field_section_subtitle),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = question,
                onValueChange = {
                    onQuestionChange(
                        it.take(CUSTOM_JOURNAL_FIELD_QUESTION_MAX),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.custom_journal_field_question_label)) },
                placeholder = { Text(stringResource(R.string.custom_journal_field_question_placeholder)) },
                singleLine = false,
                maxLines = 3,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
            )
            OutlinedTextField(
                value = hint,
                onValueChange = {
                    onHintChange(
                        it.take(CUSTOM_JOURNAL_FIELD_HINT_MAX),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.custom_journal_field_hint_label)) },
                placeholder = { Text(stringResource(R.string.custom_journal_field_hint_placeholder)) },
                singleLine = false,
                maxLines = 4,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.custom_journal_field_switch),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
        }
    }
}

@Composable
internal fun DangerZoneSection(
    onRequestWipe: () -> Unit,
) {
    SectionCard(
        title = stringResource(R.string.danger_zone_title),
        subtitle = stringResource(R.string.danger_zone_subtitle),
    ) {
        OutlinedButton(
            onClick = onRequestWipe,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.DeleteSweep,
                contentDescription = stringResource(R.string.cd_wipe_all_history),
                tint = MaterialTheme.colorScheme.error,
            )
            Text(
                text = stringResource(R.string.settings_wipe_all_button),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
internal fun FirstWipeDialog(
    onCancel: () -> Unit,
    onContinue: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.settings_wipe_first_title)) },
        text = {
            Text(text = stringResource(R.string.settings_wipe_first_body))
        },
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text(
                    stringResource(R.string.action_continue),
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

@Composable
internal fun FinalWipeDialog(
    isWiping: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    var typed by rememberSaveable { mutableStateOf("") }
    val wipeWord = stringResource(R.string.settings_wipe_confirm_word)
    val confirmEnabled = typed.trim().equals(wipeWord, ignoreCase = true) && !isWiping

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.settings_wipe_final_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = stringResource(R.string.settings_wipe_final_hint, wipeWord))
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(wipeWord) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.error,
                        cursorColor = MaterialTheme.colorScheme.error,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmEnabled) {
                Text(
                    stringResource(R.string.settings_wipe_action),
                    color = if (confirmEnabled) MaterialTheme.colorScheme.error else Color.Unspecified,
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
