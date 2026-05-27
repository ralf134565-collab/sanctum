// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.settings.backup

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketreflect.app.R
import com.pocketreflect.app.data.transfer.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Arrays
import javax.inject.Inject

/**
 * ViewModel секции «Перенос на другое устройство».
 *
 * Жизненный цикл операции:
 *  1. SAF-лаунчер из [BackupSection] возвращает `Uri`.
 *  2. UI шлёт `Intent.ExportRequested(uri)` / `Intent.ImportRequested(uri)`.
 *  3. ViewModel переводит `pendingDialog` в `Export(uri)` или `Import(uri)` —
 *     открывается PasswordDialog.
 *  4. Пользователь подтверждает → `Intent.ConfirmExport(password)` →
 *     [BackupRepository.export] / `.import`.
 *  5. После завершения — `Effect.ShowMessage(...)` с локализованным текстом,
 *     диалог закрывается, `isBusy` сбрасывается.
 *
 * Пароль (`CharArray`) хранится только в пределах одного метода
 * и зануляется в `finally`. На уровне диалога UI отдаёт его одним движением
 * и забывает свою копию (см. [BackupSection]).
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val backupRepository: BackupRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BackupContract.State())
    val state: StateFlow<BackupContract.State> = _state.asStateFlow()

    private val _effects = Channel<BackupContract.Effect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onIntent(intent: BackupContract.Intent) {
        when (intent) {
            is BackupContract.Intent.ExportRequested ->
                _state.update {
                    it.copy(pendingDialog = BackupContract.PendingDialog.Export(intent.targetUri))
                }

            is BackupContract.Intent.ImportRequested ->
                _state.update {
                    it.copy(pendingDialog = BackupContract.PendingDialog.Import(intent.sourceUri))
                }

            is BackupContract.Intent.ConfirmExport -> runExport(intent.password)
            is BackupContract.Intent.ConfirmImport -> runImport(intent.password, intent.overwrite)
            BackupContract.Intent.DismissDialog ->
                _state.update { it.copy(pendingDialog = null) }
        }
    }

    private fun runExport(password: CharArray) {
        val pending = _state.value.pendingDialog as? BackupContract.PendingDialog.Export ?: return
        _state.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            val resolver: ContentResolver = appContext.contentResolver
            try {
                val out = resolver.openOutputStream(pending.targetUri, "wt")
                if (out == null) {
                    emitError(R.string.transfer_error_io)
                    return@launch
                }
                val summary = out.use { backupRepository.export(it, password) }
                emitMessage(
                    appContext.getString(
                        R.string.transfer_success_export,
                        summary.entries,
                    )
                )
            } catch (_: IllegalArgumentException) {
                // Encoder сам не должен бросать IllegalArgumentException, но
                // подстрахуемся — если случится, не покажем stacktrace.
                emitError(R.string.transfer_error_io)
            } catch (_: IOException) {
                emitError(R.string.transfer_error_io)
            } catch (_: SecurityException) {
                emitError(R.string.transfer_error_io)
            } finally {
                Arrays.fill(password, '\u0000')
                _state.update { it.copy(isBusy = false, pendingDialog = null) }
            }
        }
    }

    private fun runImport(password: CharArray, overwrite: Boolean) {
        val pending = _state.value.pendingDialog as? BackupContract.PendingDialog.Import ?: return
        _state.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            val resolver: ContentResolver = appContext.contentResolver
            try {
                val input = resolver.openInputStream(pending.sourceUri)
                if (input == null) {
                    emitError(R.string.transfer_error_io)
                    return@launch
                }
                val report = input.use {
                    backupRepository.import(it, password, overwrite)
                }
                emitMessage(
                    appContext.getString(
                        R.string.transfer_success_import,
                        report.insertedEntries,
                        report.skippedEntries,
                    )
                )
            } catch (e: com.pocketreflect.app.data.transfer.ImportError) {
                val resId = when (e) {
                    is com.pocketreflect.app.data.transfer.ImportError.WrongPasswordOrCorrupt -> R.string.transfer_error_wrong_password
                    is com.pocketreflect.app.data.transfer.ImportError.NotABackup -> R.string.transfer_error_not_a_backup
                    is com.pocketreflect.app.data.transfer.ImportError.UnsupportedFormat -> R.string.transfer_error_unsupported_format
                }
                emitError(resId)
            } catch (_: IOException) {
                emitError(R.string.transfer_error_io)
            } catch (_: SecurityException) {
                emitError(R.string.transfer_error_io)
            } finally {
                Arrays.fill(password, '\u0000')
                _state.update { it.copy(isBusy = false, pendingDialog = null) }
            }
        }
    }

    private fun emitMessage(message: String) {
        _effects.trySend(BackupContract.Effect.ShowMessage(message))
    }

    private fun emitError(resId: Int) {
        emitMessage(appContext.getString(resId))
    }
}
