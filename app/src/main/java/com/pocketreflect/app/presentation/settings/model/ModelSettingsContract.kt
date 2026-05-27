// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.settings.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import com.pocketreflect.app.data.ai.EngineBackend
import com.pocketreflect.app.data.model.ModelVariant
import com.pocketreflect.app.data.repository.AttachedModel

/**
 * MVI-контракт экрана выбора и подключения локальной модели.
 *
 * Семантика prosess:
 *  - в фоне идёт **ровно один** attach за раз, состояние сериализуется через
 *    [State.progress];
 *  - выбор варианта (E2B / E4B) живёт в одном поле [State.pickingForVariant]
 *    между моментом тапа на «Подключить файл» и фактическим выбором файла в SAF;
 *  - открытие внешних ссылок вынесено как [Effect.OpenExternalUrl] — UI делает
 *    `ACTION_VIEW` сам, чтобы ViewModel не знала об `Intent`'ах Android.
 *
 * Дополнительно (Sub-PR #3c): держит [State.selectedBackend] для toggle
 * GPU/CPU. Тогл живёт независимо от привязки файла — пользователь может
 * выбрать предпочитаемый бэкенд ещё до подключения первой модели.
 */
object ModelSettingsContract {

    @Immutable
    data class State(
        val variants: List<ModelVariant> = ModelVariant.entries.toList(),
        val attached: AttachedModel? = null,
        val progress: AttachProgress? = null,
        val expandedSourcesFor: ModelVariant? = null,
        val pickingForVariant: ModelVariant? = null,
        val isConfirmingDetach: Boolean = false,
        val selectedBackend: EngineBackend = EngineBackend.GPU,
    )

    sealed interface AttachProgress {
        data class Copying(val bytesCopied: Long, val bytesTotal: Long) : AttachProgress
        data object Verifying : AttachProgress
    }

    sealed interface Intent {
        data class ToggleSources(val variant: ModelVariant) : Intent
        data class OpenSource(val url: String) : Intent
        data class StartAttach(val variant: ModelVariant) : Intent
        data class FilePicked(val uri: Uri) : Intent
        data object FilePickerCancelled : Intent
        data object RequestDetach : Intent
        data object ConfirmDetach : Intent
        data object CancelDetach : Intent
        data class SelectBackend(val backend: EngineBackend) : Intent
    }

    sealed interface Effect {
        data class OpenExternalUrl(val url: String) : Effect
        data class ShowMessage(val message: String) : Effect
    }
}
