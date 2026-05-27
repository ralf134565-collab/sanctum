// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.history.detail

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.pocketreflect.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketreflect.app.data.repository.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel экрана детали записи.
 *
 * Двойное подтверждение удаления — намеренное архитектурное решение:
 *  - Запись в дневнике невосстановима (export/import пока нет).
 *  - Пользователь часто пролистывает старые записи в состоянии «эх, не хочу»;
 *    в это время одно касание не должно безвозвратно стирать день.
 */
@HiltViewModel
class EntryDetailViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val journalRepository: JournalRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val entryId: Long = checkNotNull(savedStateHandle.get<Long>(ARG_ENTRY_ID)) {
        "EntryDetailViewModel requires `$ARG_ENTRY_ID` argument"
    }

    private val _state = MutableStateFlow(EntryDetailContract.State())
    val state: StateFlow<EntryDetailContract.State> = _state.asStateFlow()

    private val _effects = Channel<EntryDetailContract.Effect>(capacity = Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            // Реактивная подписка: если запись удалят (например, через wipeAll),
            // экран сам перейдёт в empty-состояние без ручной инвалидации.
            journalRepository.observeById(entryId).collect { entry ->
                _state.update { it.copy(isLoading = false, entry = entry) }
            }
        }
    }

    fun onIntent(intent: EntryDetailContract.Intent) {
        when (intent) {
            EntryDetailContract.Intent.RequestDelete ->
                _state.update { it.copy(isConfirmingDelete = true) }

            EntryDetailContract.Intent.ConfirmFirstStep -> performDelete()

            EntryDetailContract.Intent.CancelDelete ->
                _state.update {
                    it.copy(isConfirmingDelete = false, isFinalConfirmingDelete = false)
                }

            EntryDetailContract.Intent.ConfirmFinalDelete -> performDelete()
        }
    }

    private fun performDelete() {
        val snapshot = _state.value.entry ?: return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            try {
                journalRepository.delete(snapshot)
                _effects.trySend(EntryDetailContract.Effect.EntryDeleted)
            } catch (t: Throwable) {
                _state.update { it.copy(isDeleting = false, isConfirmingDelete = false, isFinalConfirmingDelete = false) }
                _effects.trySend(
                    EntryDetailContract.Effect.ShowError(
                        appContext.getString(R.string.entry_detail_delete_error),
                    ),
                )
            }
        }
    }

    companion object {
        const val ARG_ENTRY_ID: String = "entryId"
    }
}
