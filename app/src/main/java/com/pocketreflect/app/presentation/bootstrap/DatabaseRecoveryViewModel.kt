// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.bootstrap

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketreflect.app.R
import com.pocketreflect.app.core.security.DatabaseBootstrap
import com.pocketreflect.app.data.transfer.ImportError
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DatabaseRecoveryViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val databaseBootstrap: DatabaseBootstrap,
) : ViewModel() {

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun startFresh() {
        viewModelScope.launch {
            _busy.value = true
            _message.value = null
            runCatching { databaseBootstrap.startFresh() }
                .onFailure { _message.value = it.message ?: it.toString() }
            _busy.value = false
        }
    }

    fun importFrom(uri: Uri, password: CharArray, overwrite: Boolean) {
        viewModelScope.launch {
            _busy.value = true
            _message.value = null
            try {
                databaseBootstrap.importAndUnblock(uri, password, overwrite)
            } catch (e: ImportError) {
                val resId = when (e) {
                    is ImportError.WrongPasswordOrCorrupt -> R.string.transfer_error_wrong_password
                    is ImportError.NotABackup -> R.string.transfer_error_not_a_backup
                    is ImportError.UnsupportedFormat -> R.string.transfer_error_unsupported_format
                }
                _message.value = appContext.getString(resId)
            } catch (e: Exception) {
                _message.value = appContext.getString(R.string.transfer_error_io)
            }
            _busy.value = false
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun setErrorMessage(msg: String) {
        _message.value = msg
    }
}
