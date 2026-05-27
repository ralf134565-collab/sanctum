// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.ai

import com.pocketreflect.app.data.repository.ModelSelectionRepository
import com.pocketreflect.app.domain.ai.AiEngineStatus
import com.pocketreflect.app.domain.ai.AiEngineStatusSource
import com.pocketreflect.app.domain.ai.GemmaLocalEngine
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive

/**
 * Единый источник статуса ИИ для presentation-слоя.
 *
 * WorkManager-прогрев может завершиться с ошибкой, пока движок поднимется
 * лениво при первом запросе — поэтому учитываем [GemmaLocalEngine.isReady]
 * у real-движка, а не только [WarmupState].
 */
@Singleton
class AiEngineStatusProvider @Inject constructor(
    modelSelectionRepo: ModelSelectionRepository,
    warmupCoordinator: WarmupCoordinator,
    @Named("real") private val realEngine: GemmaLocalEngine,
) : AiEngineStatusSource {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val hasAttachedFlow = modelSelectionRepo.attached
        .map { it != null }
        .distinctUntilChanged()

    private val runtimeFailureFlow = MutableStateFlow(false)

    override fun notifyRuntimeFailure() {
        runtimeFailureFlow.value = true
    }

    private val realReadyFlow = hasAttachedFlow.flatMapLatest { attached ->
        if (!attached) {
            flowOf(false)
        } else {
            flow {
                while (currentCoroutineContext().isActive) {
                    emit(runCatching { realEngine.isReady() }.getOrDefault(false))
                    delay(ENGINE_READY_POLL_MS)
                }
            }
        }
    }.distinctUntilChanged()

    override val status: StateFlow<AiEngineStatus> = combine(
        hasAttachedFlow,
        warmupCoordinator.state,
        realReadyFlow,
        runtimeFailureFlow,
    ) { hasAttached, warmup, realReady, runtimeFailure ->
        if (runtimeFailure) {
            AiEngineStatus.FALLBACK
        } else {
            resolveAiEngineStatus(hasAttached, warmup, realReady)
        }
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = AiEngineStatus.MODEL_OFFLINE,
    )

    private companion object {
        const val ENGINE_READY_POLL_MS = 2_000L
    }
}
