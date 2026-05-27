// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.chat

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.pocketreflect.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.lifecycle.viewModelScope
import com.pocketreflect.app.core.locale.AppLanguageResolver
import com.pocketreflect.app.core.time.Clock
import com.pocketreflect.app.data.repository.ChatRepository
import com.pocketreflect.app.data.repository.JournalRepository
import com.pocketreflect.app.data.repository.UserPreferencesRepository
import com.pocketreflect.app.domain.ai.AiEngineStatusSource
import com.pocketreflect.app.domain.ai.GemmaLocalEngine
import com.pocketreflect.app.domain.ai.prompts.JournalPrompts
import com.pocketreflect.app.domain.chat.ChatContextPolicy
import com.pocketreflect.app.domain.chat.ChatJournalSnippetBuilder
import com.pocketreflect.app.domain.chat.ChatMessage
import com.pocketreflect.app.domain.chat.ChatPersona
import com.pocketreflect.app.domain.chat.ChatRole
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val chatRepository: ChatRepository,
    private val journalRepository: JournalRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val gemmaEngine: GemmaLocalEngine,
    private val clock: Clock,
    private val aiEngineStatusSource: AiEngineStatusSource,
    private val appLanguageResolver: AppLanguageResolver,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatContract.State())
    val state: StateFlow<ChatContract.State> = _state.asStateFlow()

    private val _effects = Channel<ChatContract.Effect>(capacity = Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var streamJob: Job? = null
    private var journalSnippetCache: String = ""
    private var manifestoSnippetCache: String = ""

    init {
        viewModelScope.launch {
            aiEngineStatusSource.status.collect { status ->
                _state.update { it.copy(aiEngineStatus = status) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.chatPersona.flatMapLatest { persona ->
                combine(
                    chatRepository.observeMessages(persona.storageKey),
                    userPreferencesRepository.chatDisclaimerAccepted,
                    userPreferencesRepository.chatJournalContextEnabled,
                    userPreferencesRepository.chatJournalContextDays,
                    combine(
                        userPreferencesRepository.chatManifestoContextEnabled,
                        userPreferencesRepository.personalManifesto,
                    ) { manifestoOn, manifestoText -> manifestoOn to manifestoText },
                ) { messages, disclaimer, journalOn, journalDays, manifestoPrefs ->
                    val (manifestoOn, manifestoText) = manifestoPrefs
                    ChatSnapshot(
                        messages = messages,
                        disclaimerAccepted = disclaimer,
                        persona = persona,
                        journalContextEnabled = journalOn,
                        journalContextDays = journalDays,
                        manifestoContextEnabled = manifestoOn,
                        personalManifesto = manifestoText,
                    )
                }
            }.collect { snapshot ->
                runCatching { applySnapshot(snapshot) }
                    .onFailure { error ->
                        Log.e(TAG, "Chat snapshot apply failed", error)
                    }
            }
        }
    }

    private var lastJournalOn: Boolean? = null
    private var lastJournalDays: Int? = null
    private var lastManifestoOn: Boolean? = null
    private var lastManifestoText: String? = null

    private suspend fun applySnapshot(snapshot: ChatSnapshot) {
        if (snapshot.journalContextEnabled != lastJournalOn || snapshot.journalContextDays != lastJournalDays) {
            lastJournalOn = snapshot.journalContextEnabled
            lastJournalDays = snapshot.journalContextDays
            refreshJournalSnippet(snapshot.journalContextEnabled, snapshot.journalContextDays)
        }
        if (snapshot.manifestoContextEnabled != lastManifestoOn || snapshot.personalManifesto != lastManifestoText) {
            lastManifestoOn = snapshot.manifestoContextEnabled
            lastManifestoText = snapshot.personalManifesto
            refreshManifestoSnippet(snapshot.manifestoContextEnabled, snapshot.personalManifesto)
        }
        val journalPart = journalSnippetCache.takeIf { snapshot.journalContextEnabled }
        val manifestoPart = manifestoSnippetCache.takeIf { snapshot.manifestoContextEnabled }
        val usage = ChatContextPolicy.computeUsage(snapshot.messages, journalPart, manifestoPart)
        _state.update { current ->
            current.copy(
                disclaimerAccepted = snapshot.disclaimerAccepted,
                messages = snapshot.messages,
                persona = snapshot.persona,
                journalContextEnabled = snapshot.journalContextEnabled,
                journalContextDays = snapshot.journalContextDays,
                manifestoContextEnabled = snapshot.manifestoContextEnabled,
                contextPercent = usage.percent,
                isContextFull = usage.isFull,
            )
        }
    }

    private data class ChatSnapshot(
        val messages: List<ChatMessage>,
        val disclaimerAccepted: Boolean,
        val persona: ChatPersona,
        val journalContextEnabled: Boolean,
        val journalContextDays: Int,
        val manifestoContextEnabled: Boolean,
        val personalManifesto: String,
    )

    fun onIntent(intent: ChatContract.Intent) {
        when (intent) {
            is ChatContract.Intent.UpdateInput -> _state.update { it.copy(inputText = intent.text) }
            ChatContract.Intent.SendMessage -> sendMessage()
            ChatContract.Intent.CancelStreaming -> streamJob?.cancel()
            is ChatContract.Intent.SelectPersona -> {
                viewModelScope.launch {
                    userPreferencesRepository.setChatPersona(intent.persona)
                }
                _state.update { it.copy(showPersonaSheet = false) }
            }
            ChatContract.Intent.OpenPersonaSheet -> _state.update { it.copy(showPersonaSheet = true) }
            ChatContract.Intent.ClosePersonaSheet -> _state.update { it.copy(showPersonaSheet = false) }
            is ChatContract.Intent.SetJournalContextEnabled -> viewModelScope.launch {
                userPreferencesRepository.setChatJournalContextEnabled(intent.enabled)
            }
            is ChatContract.Intent.SetJournalContextDays -> viewModelScope.launch {
                userPreferencesRepository.setChatJournalContextDays(intent.days)
            }
            is ChatContract.Intent.SetManifestoContextEnabled -> viewModelScope.launch {
                userPreferencesRepository.setChatManifestoContextEnabled(intent.enabled)
            }
            ChatContract.Intent.RequestClearChat -> _state.update { it.copy(showClearConfirm = true) }
            ChatContract.Intent.ConfirmClearChat -> clearChat()
            ChatContract.Intent.DismissClearChat -> _state.update { it.copy(showClearConfirm = false) }
            ChatContract.Intent.CompactChat -> compactChat()
            ChatContract.Intent.AcceptDisclaimer -> {
                // Сразу переключаем UI — не ждём emit из DataStore (иначе кажется,
                // что кнопка «не работает»).
                _state.update { it.copy(disclaimerAccepted = true) }
                viewModelScope.launch {
                    userPreferencesRepository.setChatDisclaimerAccepted(true)
                }
            }
        }
    }

    private suspend fun refreshManifestoSnippet(enabled: Boolean, manifesto: String) {
        manifestoSnippetCache = if (!enabled) {
            ""
        } else {
            JournalPrompts.manifestoForPrompt(manifesto).orEmpty()
        }
    }

    private suspend fun refreshJournalSnippet(enabled: Boolean, days: Int) {
        journalSnippetCache = if (!enabled) {
            ""
        } else {
            runCatching {
                val entries = journalRepository.entriesForLastDays(days)
                val language = appLanguageResolver.resolvedNow()
                ChatJournalSnippetBuilder.build(entries, language)
            }.getOrElse { "" }
        }
    }

    private fun sendMessage() {
        val snapshot = _state.value
        if (!snapshot.canSend) {
            if (snapshot.isContextFull) {
                _effects.trySend(
                    ChatContract.Effect.ShowSnackbar(
                        appContext.getString(R.string.chat_context_full),
                    ),
                )
            }
            return
        }
        val text = snapshot.inputText.trim()
        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            _state.update { it.copy(inputText = "", isStreaming = true, streamingPreview = "") }
            val userMessage = ChatMessage(
                role = ChatRole.USER,
                content = text,
                timestamp = clock.nowMillis(),
                personaId = snapshot.persona.storageKey,
            )
            chatRepository.insert(userMessage)
            val history = chatRepository.observeMessages(snapshot.persona.storageKey).first()
            val journalSnippet = if (snapshot.journalContextEnabled) journalSnippetCache else null
            val manifestoSnippet = if (snapshot.manifestoContextEnabled) {
                manifestoSnippetCache.takeIf { it.isNotBlank() }
            } else {
                null
            }
            val trimmed = ChatContextPolicy.trimHistoryForInference(
                messages = history,
                journalSnippetLength = journalSnippet?.length ?: 0,
                manifestoSnippetLength = manifestoSnippet?.length ?: 0,
            )
            val buffer = StringBuilder()
            try {
                gemmaEngine.streamChat(
                    history = trimmed,
                    persona = snapshot.persona,
                    journalSnippet = journalSnippet,
                    manifestoSnippet = manifestoSnippet,
                ).collect { chunk ->
                    buffer.append(chunk)
                    _state.update { it.copy(streamingPreview = buffer.toString()) }
                }
                val assistant = ChatMessage(
                    role = ChatRole.ASSISTANT,
                    content = buffer.toString().trim().ifEmpty { "…" },
                    timestamp = clock.nowMillis(),
                    personaId = snapshot.persona.storageKey,
                )
                chatRepository.insert(assistant)
            } catch (_: kotlinx.coroutines.CancellationException) {
                if (buffer.isNotBlank()) {
                    chatRepository.insert(
                        ChatMessage(
                            role = ChatRole.ASSISTANT,
                            content = buffer.toString().trim(),
                            timestamp = clock.nowMillis(),
                            personaId = snapshot.persona.storageKey,
                        ),
                    )
                }
            } finally {
                _state.update { it.copy(isStreaming = false, streamingPreview = null) }
            }
        }
    }

    private fun clearChat() {
        viewModelScope.launch {
            val snapshot = _state.value
            chatRepository.clearPersonaChat(snapshot.persona.storageKey)
            _state.update { it.copy(showClearConfirm = false) }
        }
    }

    private fun compactChat() {
        val snapshot = _state.value
        if (snapshot.isCompacting || snapshot.messages.isEmpty()) return

        _state.update { it.copy(isCompacting = true) }
        viewModelScope.launch {
            try {
                val summaryText = gemmaEngine.summarizeChat(snapshot.messages)
                chatRepository.clearPersonaChat(snapshot.persona.storageKey)

                val systemSummaryMessage = ChatMessage(
                    role = ChatRole.ASSISTANT,
                    content = summaryText.trim(),
                    timestamp = clock.nowMillis(),
                    personaId = snapshot.persona.storageKey,
                )
                chatRepository.insert(systemSummaryMessage)

                _effects.trySend(
                    ChatContract.Effect.ShowSnackbar(
                        appContext.getString(R.string.chat_compact_success)
                    )
                )
            } catch (e: Exception) {
                _effects.trySend(
                    ChatContract.Effect.ShowSnackbar(
                        appContext.getString(R.string.chat_compact_error)
                    )
                )
            } finally {
                _state.update { it.copy(isCompacting = false) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        streamJob?.cancel()
    }

    private companion object {
        const val TAG = "ChatViewModel"
    }
}
