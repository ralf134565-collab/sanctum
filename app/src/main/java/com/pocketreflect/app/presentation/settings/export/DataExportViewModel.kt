// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.settings.export

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketreflect.app.R
import com.pocketreflect.app.data.export.vault.VaultExportOptions
import com.pocketreflect.app.data.export.vault.VaultExportRepository
import com.pocketreflect.app.data.export.vault.VaultImportError
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Arrays
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DataExportViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val vaultExportRepository: VaultExportRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DataExportContract.State())
    val state: StateFlow<DataExportContract.State> = _state.asStateFlow()

    private val _effects = Channel<DataExportContract.Effect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onIntent(intent: DataExportContract.Intent) {
        when (intent) {
            is DataExportContract.Intent.ExportRequested -> {
                val encrypt = _state.value.encryptExport
                _state.update {
                    it.copy(pendingDialog = DataExportContract.PendingDialog.Export(intent.targetUri, encrypt))
                }
            }
            is DataExportContract.Intent.ImportRequested ->
                _state.update {
                    it.copy(pendingDialog = DataExportContract.PendingDialog.Import(intent.sourceUri))
                }
            is DataExportContract.Intent.ConfirmExport -> runExport(intent.password)
            is DataExportContract.Intent.ConfirmImport -> runImport(intent.password, intent.overwrite)
            DataExportContract.Intent.DismissDialog ->
                _state.update { it.copy(pendingDialog = null) }
            is DataExportContract.Intent.ToggleWeeklyProfiles ->
                _state.update { it.copy(includeWeeklyProfiles = intent.enabled) }
            is DataExportContract.Intent.ToggleManifesto ->
                _state.update { it.copy(includeManifesto = intent.enabled) }
            is DataExportContract.Intent.ToggleEncryptExport ->
                _state.update { it.copy(encryptExport = intent.enabled) }
        }
    }

    private fun runExport(password: CharArray?) {
        val pending = _state.value.pendingDialog as? DataExportContract.PendingDialog.Export ?: return
        if (pending.encrypt && password == null) return

        _state.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            try {
                val out = appContext.contentResolver.openOutputStream(pending.targetUri, "wt")
                if (out == null) {
                    emitError(R.string.vault_export_error_io)
                    return@launch
                }
                val summary = out.use {
                    vaultExportRepository.export(
                        out = it,
                        options = VaultExportOptions(
                            includeWeeklyProfiles = _state.value.includeWeeklyProfiles,
                            includeManifesto = _state.value.includeManifesto,
                            encrypt = _state.value.encryptExport,
                        ),
                        password = password,
                    )
                }
                emitMessage(
                    appContext.getString(
                        R.string.vault_export_success,
                        summary.entries,
                        summary.weeklyProfiles,
                    ),
                )
            } catch (_: Exception) {
                emitError(R.string.vault_export_error_io)
            } finally {
                password?.let { Arrays.fill(it, '\u0000') }
                _state.update { it.copy(isBusy = false, pendingDialog = null) }
            }
        }
    }

    private fun runImport(password: CharArray?, overwrite: Boolean) {
        val pending = _state.value.pendingDialog as? DataExportContract.PendingDialog.Import ?: return

        _state.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            try {
                val input = appContext.contentResolver.openInputStream(pending.sourceUri)
                if (input == null) {
                    emitError(R.string.vault_export_error_io)
                    return@launch
                }
                val report = input.use {
                    vaultExportRepository.importFromStream(
                        input = it,
                        password = password,
                        overwrite = overwrite,
                    )
                }
                emitMessage(
                    appContext.getString(
                        R.string.vault_import_success,
                        report.insertedEntries,
                        report.skippedEntries,
                    ),
                )
            } catch (_: VaultImportError.WrongPasswordOrCorrupt) {
                emitError(R.string.transfer_error_wrong_password)
            } catch (_: VaultImportError.NotAVault) {
                emitError(R.string.vault_import_error_not_vault)
            } catch (_: VaultImportError.UnsupportedFormat) {
                emitError(R.string.transfer_error_unsupported_format)
            } catch (_: VaultImportError.NoEntries) {
                emitError(R.string.vault_import_error_no_entries)
            } catch (_: Exception) {
                emitError(R.string.vault_export_error_io)
            } finally {
                password?.let { Arrays.fill(it, '\u0000') }
                _state.update { it.copy(isBusy = false, pendingDialog = null) }
            }
        }
    }

    private fun emitMessage(message: String) {
        _effects.trySend(DataExportContract.Effect.ShowMessage(message))
    }

    private fun emitError(stringRes: Int) {
        _effects.trySend(
            DataExportContract.Effect.ShowMessage(appContext.getString(stringRes)),
        )
    }
}
