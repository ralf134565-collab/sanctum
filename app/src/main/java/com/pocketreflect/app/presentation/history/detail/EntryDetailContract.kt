// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.history.detail

import androidx.compose.runtime.Immutable
import com.pocketreflect.app.data.local.entity.JournalEntry

object EntryDetailContract {

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val entry: JournalEntry? = null,
        /** Показывает первый AlertDialog «вы уверены?» */
        val isConfirmingDelete: Boolean = false,
        /** Показывает второй AlertDialog с явной подтверждающей фразой. */
        val isFinalConfirmingDelete: Boolean = false,
        val isDeleting: Boolean = false,
    )

    sealed interface Intent {
        data object RequestDelete : Intent
        data object ConfirmFirstStep : Intent
        data object CancelDelete : Intent
        data object ConfirmFinalDelete : Intent
    }

    sealed interface Effect {
        data object EntryDeleted : Effect
        data class ShowError(val message: String) : Effect
    }
}
