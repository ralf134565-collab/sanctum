// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.settings.export

import android.net.Uri
import androidx.compose.runtime.Immutable
import com.pocketreflect.app.data.export.vault.VaultExportOptions

object DataExportContract {

    @Immutable
    data class State(
        val isBusy: Boolean = false,
        val includeWeeklyProfiles: Boolean = true,
        val includeManifesto: Boolean = true,
        val encryptExport: Boolean = false,
        val pendingDialog: PendingDialog? = null,
    )

    sealed interface PendingDialog {
        data class Export(val targetUri: Uri, val encrypt: Boolean) : PendingDialog
        data class Import(val sourceUri: Uri) : PendingDialog
    }

    sealed interface Intent {
        data class ExportRequested(val targetUri: Uri) : Intent
        data class ImportRequested(val sourceUri: Uri) : Intent
        data class ConfirmExport(val password: CharArray?) : Intent
        data class ConfirmImport(val password: CharArray?, val overwrite: Boolean) : Intent
        data object DismissDialog : Intent
        data class ToggleWeeklyProfiles(val enabled: Boolean) : Intent
        data class ToggleManifesto(val enabled: Boolean) : Intent
        data class ToggleEncryptExport(val enabled: Boolean) : Intent
    }

    sealed interface Effect {
        data class ShowMessage(val message: String) : Effect
    }

    fun State.toExportOptions(): VaultExportOptions = VaultExportOptions(
        includeWeeklyProfiles = includeWeeklyProfiles,
        includeManifesto = includeManifesto,
        encrypt = encryptExport,
    )
}
