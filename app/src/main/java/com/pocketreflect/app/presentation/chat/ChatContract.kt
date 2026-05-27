// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.chat

import androidx.compose.runtime.Immutable
import com.pocketreflect.app.domain.ai.AiEngineStatus
import com.pocketreflect.app.domain.chat.ChatMessage
import com.pocketreflect.app.domain.chat.ChatPersona

object ChatContract {

    @Immutable
    data class State(
        val disclaimerAccepted: Boolean = false,
        val messages: List<ChatMessage> = emptyList(),
        val inputText: String = "",
        val persona: ChatPersona = ChatPersona.DEFAULT,
        val journalContextEnabled: Boolean = false,
        val journalContextDays: Int = 3,
        val manifestoContextEnabled: Boolean = false,
        val contextPercent: Int = 0,
        val isContextFull: Boolean = false,
        val isStreaming: Boolean = false,
        val streamingPreview: String? = null,
        val showPersonaSheet: Boolean = false,
        val showClearConfirm: Boolean = false,
        val isCompacting: Boolean = false,
        val aiEngineStatus: AiEngineStatus = AiEngineStatus.MODEL_OFFLINE,
    ) {
        val canSend: Boolean
            get() = disclaimerAccepted &&
                !isContextFull &&
                !isStreaming &&
                inputText.isNotBlank()
    }

    sealed interface Intent {
        data class UpdateInput(val text: String) : Intent
        data object SendMessage : Intent
        data object CancelStreaming : Intent
        data class SelectPersona(val persona: ChatPersona) : Intent
        data object OpenPersonaSheet : Intent
        data object ClosePersonaSheet : Intent
        data class SetJournalContextEnabled(val enabled: Boolean) : Intent
        data class SetJournalContextDays(val days: Int) : Intent
        data class SetManifestoContextEnabled(val enabled: Boolean) : Intent
        data object RequestClearChat : Intent
        data object ConfirmClearChat : Intent
        data object DismissClearChat : Intent
        data object CompactChat : Intent
        data object AcceptDisclaimer : Intent
    }

    sealed interface Effect {
        data class ShowSnackbar(val message: String) : Effect
    }
}
