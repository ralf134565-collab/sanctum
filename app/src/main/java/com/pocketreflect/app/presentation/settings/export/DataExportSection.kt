// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.settings.export

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pocketreflect.app.R
import com.pocketreflect.app.core.security.rememberSecurePasswordState
import com.pocketreflect.app.presentation.journal.components.SectionCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DataExportSection(
    onShowSnackbar: (String) -> Unit,
    viewModel: DataExportViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    val exportMime = if (state.encryptExport) {
        "application/octet-stream"
    } else {
        "application/zip"
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(exportMime),
    ) { uri ->
        uri?.let { viewModel.onIntent(DataExportContract.Intent.ExportRequested(it)) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            if (isVaultImportFile(context, uri)) {
                viewModel.onIntent(DataExportContract.Intent.ImportRequested(uri))
            } else {
                onShowSnackbar(context.getString(R.string.vault_import_error_not_vault))
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DataExportContract.Effect.ShowMessage -> onShowSnackbar(effect.message)
            }
        }
    }

    SectionCard(
        title = stringResource(R.string.vault_export_section_title),
        subtitle = stringResource(R.string.vault_export_section_subtitle),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OptionSwitch(
                label = stringResource(R.string.vault_export_include_weekly),
                checked = state.includeWeeklyProfiles,
                onCheckedChange = {
                    viewModel.onIntent(DataExportContract.Intent.ToggleWeeklyProfiles(it))
                },
            )
            OptionSwitch(
                label = stringResource(R.string.vault_export_include_manifesto),
                checked = state.includeManifesto,
                onCheckedChange = {
                    viewModel.onIntent(DataExportContract.Intent.ToggleManifesto(it))
                },
            )
            OptionSwitch(
                label = stringResource(R.string.vault_export_encrypt),
                checked = state.encryptExport,
                onCheckedChange = {
                    viewModel.onIntent(DataExportContract.Intent.ToggleEncryptExport(it))
                },
            )

            Button(
                onClick = { exportLauncher.launch(suggestedVaultFilename(state.encryptExport)) },
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Outlined.FileUpload, contentDescription = stringResource(R.string.cd_vault_export))
                Text("  " + stringResource(R.string.vault_export_button))
            }

            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Outlined.FileDownload, contentDescription = stringResource(R.string.cd_vault_import))
                Text("  " + stringResource(R.string.vault_import_button))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.vault_export_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    when (val dialog = state.pendingDialog) {
        is DataExportContract.PendingDialog.Export -> {
            if (dialog.encrypt) {
                VaultExportPasswordDialog(
                    isBusy = state.isBusy,
                    onCancel = { viewModel.onIntent(DataExportContract.Intent.DismissDialog) },
                    onConfirm = { password ->
                        viewModel.onIntent(DataExportContract.Intent.ConfirmExport(password))
                    },
                )
            } else {
                LaunchedEffect(dialog.targetUri) {
                    viewModel.onIntent(DataExportContract.Intent.ConfirmExport(null))
                }
            }
        }
        is DataExportContract.PendingDialog.Import -> VaultImportDialog(
            isBusy = state.isBusy,
            onCancel = { viewModel.onIntent(DataExportContract.Intent.DismissDialog) },
            onConfirm = { password, overwrite ->
                viewModel.onIntent(DataExportContract.Intent.ConfirmImport(password, overwrite))
            },
        )
        null -> Unit
    }
}

@Composable
private fun OptionSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

@Composable
private fun VaultExportPasswordDialog(
    isBusy: Boolean,
    onCancel: () -> Unit,
    onConfirm: (CharArray) -> Unit,
) {
    val passwordState = rememberSecurePasswordState()
    val confirmState = rememberSecurePasswordState()
    val mismatch = confirmState.isNotEmpty() && passwordState.text != confirmState.text
    val canConfirm = passwordState.isNotEmpty() && passwordState.text == confirmState.text && !isBusy

    AlertDialog(
        onDismissRequest = { if (!isBusy) onCancel() },
        title = { Text(stringResource(R.string.vault_export_password_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.transfer_password_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = passwordState.text,
                    onValueChange = { passwordState.update(it) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.transfer_password_field_label)) },
                    enabled = !isBusy,
                )
                OutlinedTextField(
                    value = confirmState.text,
                    onValueChange = { confirmState.update(it) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.transfer_password_confirm_label)) },
                    isError = mismatch,
                    enabled = !isBusy,
                    colors = if (mismatch) {
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.error,
                            cursorColor = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        OutlinedTextFieldDefaults.colors()
                    },
                )
                if (mismatch) {
                    Text(
                        text = stringResource(R.string.transfer_password_mismatch),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (isBusy) BusyRow()
            }
        },
        confirmButton = {
            TextButton(
                enabled = canConfirm,
                onClick = {
                    val chars = passwordState.toCharArray()
                    passwordState.clear()
                    confirmState.clear()
                    onConfirm(chars)
                },
            ) { Text(stringResource(R.string.transfer_password_save_button)) }
        },
        dismissButton = {
            TextButton(enabled = !isBusy, onClick = onCancel) {
                Text(stringResource(R.string.transfer_cancel))
            }
        },
    )
}

@Composable
private fun VaultImportDialog(
    isBusy: Boolean,
    onCancel: () -> Unit,
    onConfirm: (CharArray?, Boolean) -> Unit,
) {
    val passwordState = rememberSecurePasswordState()
    var overwrite by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isBusy) onCancel() },
        title = { Text(stringResource(R.string.vault_import_password_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.vault_import_password_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = passwordState.text,
                    onValueChange = { passwordState.update(it) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.transfer_password_field_label)) },
                    enabled = !isBusy,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = overwrite,
                        onCheckedChange = { overwrite = it },
                        enabled = !isBusy,
                    )
                    Text(
                        text = stringResource(R.string.transfer_overwrite_checkbox),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (isBusy) BusyRow()
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isBusy,
                onClick = {
                    val chars = if (passwordState.isNotEmpty()) passwordState.toCharArray() else null
                    passwordState.clear()
                    onConfirm(chars, overwrite)
                },
            ) { Text(stringResource(R.string.transfer_password_restore_button)) }
        },
        dismissButton = {
            TextButton(enabled = !isBusy, onClick = onCancel) {
                Text(stringResource(R.string.transfer_cancel))
            }
        },
    )
}

@Composable
private fun BusyRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CircularProgressIndicator(strokeWidth = 2.dp)
        Text(
            text = stringResource(R.string.transfer_in_progress),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun suggestedVaultFilename(encrypted: Boolean): String {
    val date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    return if (encrypted) {
        "sanctum-export-$date.sanctum-vault"
    } else {
        "sanctum-export-$date.zip"
    }
}

private fun isVaultImportFile(context: android.content.Context, uri: android.net.Uri): Boolean {
    val name = getFileName(context, uri)?.lowercase() ?: return false
    return name.endsWith(".zip") ||
        name.endsWith(".sanctum-vault") ||
        name.endsWith(".md.zip")
}

private fun getFileName(context: android.content.Context, uri: android.net.Uri): String? {
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) return cursor.getString(index)
            }
        }
    }
    return uri.path?.substringAfterLast('/')
}
