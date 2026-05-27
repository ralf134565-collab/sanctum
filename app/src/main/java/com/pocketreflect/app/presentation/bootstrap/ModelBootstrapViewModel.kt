// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.bootstrap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketreflect.app.data.ai.WarmupCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * VM-обёртка над [WarmupCoordinator].
 *
 * Сам coordinator — `@Singleton`, его `state` уже горячий и переживает
 * rotation. VM нужна нам только чтобы Compose-side имел стандартный
 * `hiltViewModel()` entry point и `collectAsStateWithLifecycle` без
 * прямой инжекции data-слоя в UI.
 *
 * Мапим `coordinator.state` в [ModelBootstrapContract.State] через `map`,
 * чтобы держать UI-слой в неведении про `WarmupState` enum'ы (он
 * вообще-то data-слойный, и контракт даёт нам производные `isFinished`/
 * `hasFailed` для switch'а в Composable).
 */
@HiltViewModel
class ModelBootstrapViewModel @Inject constructor(
    coordinator: WarmupCoordinator,
) : ViewModel() {

    val state: StateFlow<ModelBootstrapContract.State> = coordinator.state
        .map { ModelBootstrapContract.State(warmup = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = ModelBootstrapContract.State(),
        )
}
