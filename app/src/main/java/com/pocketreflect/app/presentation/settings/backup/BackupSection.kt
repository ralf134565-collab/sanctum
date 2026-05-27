// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.settings.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pocketreflect.app.R
import com.pocketreflect.app.presentation.journal.components.SectionCard
import com.pocketreflect.app.core.security.SecurePasswordState
import com.pocketreflect.app.core.security.rememberSecurePasswordState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Секция «Перенос на другое устройство».
 *
 * Self-contained:
 *  - регистрирует свои `ActivityResultContracts` для SAF (export/import);
 *  - хостит [PasswordDialog] поверх Settings-экрана;
 *  - подписывается на [BackupViewModel.effects] и проксирует сообщения
 *    в [onShowSnackbar] (родитель — `SettingsScreen` — владеет общим snackbarHost'ом).
 *
 * Никакого хранения пароля в state composable'а: каждый PasswordDialog
 * локально держит `String` в `remember` (нельзя `rememberSaveable` —
 * пароль не должен пережить process death) и отдаёт его как `CharArray`
 * в ViewModel по нажатию «Подтвердить», после чего сам сразу очищается.
 */
@Composable
fun BackupSection(
    onShowSnackbar: (String) -> Unit,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        uri?.let { viewModel.onIntent(BackupContract.Intent.ExportRequested(it)) }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            if (isBackupFile(context, uri)) {
                viewModel.onIntent(BackupContract.Intent.ImportRequested(uri))
            } else {
                onShowSnackbar(context.getString(R.string.transfer_error_not_a_backup))
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BackupContract.Effect.ShowMessage -> onShowSnackbar(effect.message)
            }
        }
    }

    SectionCard(
        title = stringResource(R.string.transfer_section_title),
        subtitle = stringResource(R.string.transfer_section_subtitle),
    ) {
        Button(
            onClick = { exportLauncher.launch(suggestedExportFilename()) },
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudUpload,
                contentDescription = stringResource(R.string.cd_backup_export),
            )
            Text(
                text = "  " + stringResource(R.string.transfer_export_button),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        OutlinedButton(
            onClick = { importLauncher.launch(arrayOf("application/octet-stream")) },
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudDownload,
                contentDescription = stringResource(R.string.cd_backup_import),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "  " + stringResource(R.string.transfer_import_button),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }

    when (state.pendingDialog) {
        is BackupContract.PendingDialog.Export -> ExportPasswordDialog(
            isBusy = state.isBusy,
            onCancel = { viewModel.onIntent(BackupContract.Intent.DismissDialog) },
            onConfirm = { password ->
                viewModel.onIntent(BackupContract.Intent.ConfirmExport(password))
            },
        )

        is BackupContract.PendingDialog.Import -> ImportPasswordDialog(
            isBusy = state.isBusy,
            onCancel = { viewModel.onIntent(BackupContract.Intent.DismissDialog) },
            onConfirm = { password, overwrite ->
                viewModel.onIntent(BackupContract.Intent.ConfirmImport(password, overwrite))
            },
        )

        null -> Unit
    }
}

/**
 * Рекомендуемое имя файла — `sanctum-2026-05-19.sanctum`.
 * Дата помогает пользователю различать несколько копий, расширение
 * `.sanctum` — наш собственный тип файла.
 */
private fun suggestedExportFilename(): String {
    val date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    return "sanctum-$date.sanctum"
}

@Composable
private fun ExportPasswordDialog(
    isBusy: Boolean,
    onCancel: () -> Unit,
    onConfirm: (CharArray) -> Unit,
) {
    val passwordState = rememberSecurePasswordState()
    val confirmState = rememberSecurePasswordState()

    val mismatch = confirmState.isNotEmpty() && passwordState.text != confirmState.text
    val canConfirm = passwordState.isNotEmpty() && passwordState.text == confirmState.text && !isBusy

    AlertDialog(
        onDismissRequest = {
            if (!isBusy) {
                passwordState.clear()
                confirmState.clear()
                onCancel()
            }
        },
        title = { Text(stringResource(R.string.transfer_password_title_export)) },
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
                    } else OutlinedTextFieldDefaults.colors(),
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
                    val charArr = passwordState.toCharArray()
                    passwordState.clear()
                    confirmState.clear()
                    onConfirm(charArr)
                },
            ) { Text(stringResource(R.string.transfer_password_save_button)) }
        },
        dismissButton = {
            TextButton(
                enabled = !isBusy,
                onClick = {
                    passwordState.clear()
                    confirmState.clear()
                    onCancel()
                }
            ) {
                Text(stringResource(R.string.transfer_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ImportPasswordDialog(
    isBusy: Boolean,
    onCancel: () -> Unit,
    onConfirm: (CharArray, Boolean) -> Unit,
) {
    val passwordState = rememberSecurePasswordState()
    var overwrite by remember { mutableStateOf(false) }

    val canConfirm = passwordState.isNotEmpty() && !isBusy

    AlertDialog(
        onDismissRequest = {
            if (!isBusy) {
                passwordState.clear()
                onCancel()
            }
        },
        title = { Text(stringResource(R.string.transfer_password_title_import)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (isBusy) BusyRow()
            }
        },
        confirmButton = {
            TextButton(
                enabled = canConfirm,
                onClick = {
                    val charArr = passwordState.toCharArray()
                    passwordState.clear()
                    onConfirm(charArr, overwrite)
                },
            ) { Text(stringResource(R.string.transfer_password_restore_button)) }
        },
        dismissButton = {
            TextButton(
                enabled = !isBusy,
                onClick = {
                    passwordState.clear()
                    onCancel()
                }
            ) {
                Text(stringResource(R.string.transfer_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun BusyRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier, strokeWidth = 2.dp)
        Text(
            text = stringResource(R.string.transfer_in_progress),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun isBackupFile(context: android.content.Context, uri: android.net.Uri): Boolean {
    val name = getFileName(context, uri) ?: return false
    return name.endsWith(".sanctum", ignoreCase = true) || name.endsWith(".pocketreflect", ignoreCase = true)
}

private fun getFileName(context: android.content.Context, uri: android.net.Uri): String? {
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    return cursor.getString(index)
                }
            }
        }
    }
    return uri.path?.substringAfterLast('/')
}
