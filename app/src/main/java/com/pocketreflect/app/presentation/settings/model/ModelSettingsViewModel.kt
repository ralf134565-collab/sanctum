// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.settings.model

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketreflect.app.R
import com.pocketreflect.app.core.time.Clock
import com.pocketreflect.app.data.ai.EngineBackend
import com.pocketreflect.app.data.model.ModelManifest
import com.pocketreflect.app.data.model.ModelStorage
import com.pocketreflect.app.data.model.ModelVariant
import com.pocketreflect.app.data.repository.AttachedModel
import com.pocketreflect.app.data.repository.ModelSelectionRepository
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
import javax.inject.Inject

/**
 * ViewModel экрана выбора и подключения локальной модели.
 *
 * Жизненный цикл одного attach:
 *  1. UI шлёт `Intent.StartAttach(variant)` → переводим в [State.pickingForVariant].
 *  2. UI сам запускает SAF-лаунчер (`ACTION_OPEN_DOCUMENT`) при изменении этого поля.
 *  3. После выбора файла UI шлёт `Intent.FilePicked(uri)` → начинаем копирование.
 *  4. Прогресс UI получает через `state.progress`. Промежуточные обновления
 *     [ModelStorage.attach] дёргают callback на каждом 4-MB-чанке, и мы
 *     **тротлим** запись в state до [PROGRESS_TICK_BYTES], чтобы не топить
 *     `MutableStateFlow` сотнями обновлений в секунду.
 *  5. После успешной верификации SHA-256 → запись в [ModelSelectionRepository]
 *     и `Effect.ShowMessage(success)`. Подписка на `repository.attached`
 *     автоматически обновит карточку «Модель подключена».
 *  6. На любую ошибку (`IntegrityFailed`, `RenameFailed`, `IOException`,
 *     `SecurityException`) — мягкое сообщение через `Effect.ShowMessage`,
 *     состояние возвращается к `progress == null`.
 *
 * Инференс идёт через публичный `GemmaLocalEngine` (`EngineCoordinator`:
 * LiteRT при подключённой модели, mock — в режиме поддержки).
 */
@HiltViewModel
class ModelSettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val modelStorage: ModelStorage,
    private val repository: ModelSelectionRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(ModelSettingsContract.State())
    val state: StateFlow<ModelSettingsContract.State> = _state.asStateFlow()

    private val _effects = Channel<ModelSettingsContract.Effect>(capacity = Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.attached.collect { attached ->
                _state.update { it.copy(attached = attached) }
            }
        }
        viewModelScope.launch {
            repository.selectedBackend.collect { backend ->
                _state.update { it.copy(selectedBackend = backend) }
            }
        }
    }

    fun onIntent(intent: ModelSettingsContract.Intent) {
        when (intent) {
            is ModelSettingsContract.Intent.ToggleSources -> _state.update {
                val current = it.expandedSourcesFor
                it.copy(expandedSourcesFor = if (current == intent.variant) null else intent.variant)
            }

            is ModelSettingsContract.Intent.OpenSource ->
                _effects.trySend(ModelSettingsContract.Effect.OpenExternalUrl(intent.url))

            is ModelSettingsContract.Intent.StartAttach -> {
                if (_state.value.progress != null) return
                _state.update { it.copy(pickingForVariant = intent.variant) }
            }

            is ModelSettingsContract.Intent.FilePicked -> runAttach(intent.uri)

            ModelSettingsContract.Intent.FilePickerCancelled ->
                _state.update { it.copy(pickingForVariant = null) }

            ModelSettingsContract.Intent.RequestDetach ->
                _state.update { it.copy(isConfirmingDetach = true) }

            ModelSettingsContract.Intent.ConfirmDetach -> runDetach()

            ModelSettingsContract.Intent.CancelDetach ->
                _state.update { it.copy(isConfirmingDetach = false) }

            is ModelSettingsContract.Intent.SelectBackend -> persistBackend(intent.backend)
        }
    }

    /**
     * Запись backend'а в DataStore. UI получит обновление через подписку
     * на `repository.selectedBackend` — мы не делаем `_state.update` напрямую,
     * чтобы DataStore оставался единственным источником истины и не было
     * рассинхрона между in-memory state и persisted-значением (например,
     * если запись упадёт — UI не «солжёт», что переключение прошло).
     */
    private fun persistBackend(backend: EngineBackend) {
        if (_state.value.selectedBackend == backend) return
        viewModelScope.launch { repository.setBackend(backend) }
    }

    private fun runAttach(uri: Uri) {
        val variant = _state.value.pickingForVariant ?: return
        val entry = ModelManifest.entryOf(variant)
        _state.update {
            it.copy(
                progress = ModelSettingsContract.AttachProgress.Copying(0L, entry.expectedSizeBytes),
                pickingForVariant = null,
            )
        }
        viewModelScope.launch {
            try {
                val input = appContext.contentResolver.openInputStream(uri)
                if (input == null) {
                    emitMessage(R.string.model_attach_fail_io)
                    return@launch
                }
                val outcome = input.use { stream ->
                    modelStorage.attach(variant, stream) { bytes, total ->
                        // Throttle: апдейтим state только когда сделан очередной "тик".
                        // Без тротлинга 2.6 GB файл дёргает callback ~640 раз — UI
                        // успевает, но это бессмысленная нагрузка на recomposition.
                        val current = _state.value.progress
                            as? ModelSettingsContract.AttachProgress.Copying
                        val shouldEmit = current == null ||
                            bytes - current.bytesCopied >= PROGRESS_TICK_BYTES ||
                            bytes == total
                        if (shouldEmit) {
                            _state.update {
                                it.copy(
                                    progress = ModelSettingsContract.AttachProgress.Copying(
                                        bytesCopied = bytes,
                                        bytesTotal = total,
                                    )
                                )
                            }
                        }
                    }
                }
                _state.update { it.copy(progress = ModelSettingsContract.AttachProgress.Verifying) }
                when (outcome) {
                    is ModelStorage.CopyOutcome.Success -> {
                        repository.setAttached(
                            AttachedModel(
                                variant = variant,
                                absolutePath = outcome.file.absolutePath,
                                sizeBytes = outcome.sizeBytes,
                                sha256Hex = outcome.sha256Hex,
                                attachedAtEpochMs = clock.nowMillis(),
                            )
                        )
                        _effects.trySend(
                            ModelSettingsContract.Effect.ShowMessage(
                                appContext.getString(
                                    R.string.model_attach_success_template,
                                    entry.displayName,
                                )
                            )
                        )
                    }
                    is ModelStorage.CopyOutcome.IntegrityFailed ->
                        emitMessage(R.string.model_attach_fail_sha)
                    ModelStorage.CopyOutcome.RenameFailed ->
                        emitMessage(R.string.model_attach_fail_io)
                }
            } catch (_: IOException) {
                emitMessage(R.string.model_attach_fail_io)
            } catch (_: SecurityException) {
                emitMessage(R.string.model_attach_fail_io)
            } finally {
                _state.update { it.copy(progress = null) }
            }
        }
    }

    private fun runDetach() {
        val attached = _state.value.attached ?: run {
            _state.update { it.copy(isConfirmingDetach = false) }
            return
        }
        viewModelScope.launch {
            modelStorage.delete(attached.variant)
            repository.clearAttached()
            _state.update { it.copy(isConfirmingDetach = false) }
        }
    }

    private fun emitMessage(resId: Int) {
        _effects.trySend(
            ModelSettingsContract.Effect.ShowMessage(appContext.getString(resId))
        )
    }

    private companion object {
        // 16 MB на тик — UI получает ~150 обновлений на E2B-файл, что более чем
        // достаточно для плавности и совершенно не нагружает recomposition.
        const val PROGRESS_TICK_BYTES = 16L * 1024 * 1024
    }
}
