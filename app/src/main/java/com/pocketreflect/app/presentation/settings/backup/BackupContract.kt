// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.settings.backup

import android.net.Uri
import androidx.compose.runtime.Immutable

object BackupContract {

    @Immutable
    data class State(
        /** В процессе шифрования/расшифровки — блокируем UI кнопок. */
        val isBusy: Boolean = false,
        /**
         * Тип диалога, который сейчас открыт.
         * `null` — диалоги закрыты, показаны только кнопки.
         */
        val pendingDialog: PendingDialog? = null,
    )

    /**
     * Состояние «ждём пароль от пользователя» после того, как SAF
     * вернул `Uri` для записи (export) или чтения (import).
     */
    sealed interface PendingDialog {
        data class Export(val targetUri: Uri) : PendingDialog
        data class Import(val sourceUri: Uri) : PendingDialog
    }

    sealed interface Intent {
        data class ExportRequested(val targetUri: Uri) : Intent
        data class ImportRequested(val sourceUri: Uri) : Intent

        /** Пользователь ввёл пароль и подтвердил экспорт. */
        data class ConfirmExport(val password: CharArray) : Intent

        /** Пользователь ввёл пароль и подтвердил импорт. */
        data class ConfirmImport(val password: CharArray, val overwrite: Boolean) : Intent

        data object DismissDialog : Intent
    }

    sealed interface Effect {
        /** Показать snackbar с готовым текстом (локализован в VM). */
        data class ShowMessage(val message: String) : Effect
    }
}
