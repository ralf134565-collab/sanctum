// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.bootstrap

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pocketreflect.app.R
import com.pocketreflect.app.core.security.SecurePasswordState
import com.pocketreflect.app.core.security.rememberSecurePasswordState

@Composable
fun DatabaseUnavailableScreen(
    viewModel: DatabaseRecoveryViewModel = hiltViewModel(),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val errorMessage by viewModel.message.collectAsStateWithLifecycle()
    var showImportPassword by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            if (isBackupFile(context, uri)) {
                pendingImportUri = uri
                showImportPassword = true
            } else {
                viewModel.setErrorMessage(context.getString(R.string.transfer_error_not_a_backup))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.db_unavailable_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.db_unavailable_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            if (busy) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { importLauncher.launch(arrayOf("application/octet-stream")) },
                    enabled = !busy,
                ) {
                    Text(stringResource(R.string.db_unavailable_restore))
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { viewModel.startFresh() },
                    enabled = !busy,
                ) {
                    Text(stringResource(R.string.db_unavailable_start_fresh))
                }
            }
        }
    }

    if (showImportPassword && pendingImportUri != null) {
        DatabaseImportPasswordDialog(
            isBusy = busy,
            onCancel = {
                showImportPassword = false
                pendingImportUri = null
                viewModel.clearMessage()
            },
            onConfirm = { password, overwrite ->
                viewModel.importFrom(pendingImportUri!!, password, overwrite)
                showImportPassword = false
                pendingImportUri = null
            },
        )
    }
}

@Composable
private fun DatabaseImportPasswordDialog(
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
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canConfirm,
                onClick = {
                    val chars = passwordState.toCharArray()
                    passwordState.clear()
                    onConfirm(chars, overwrite)
                },
            ) {
                Text(stringResource(R.string.transfer_password_restore_button))
            }
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
    )
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
