// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.ai

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.pocketreflect.app.core.work.ModelWarmupWorker
import com.pocketreflect.app.data.repository.ModelSelectionRepository
import com.pocketreflect.app.data.repository.UserPreferencesRepository
import com.pocketreflect.app.domain.ai.GemmaLocalEngine
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Координатор прогрева модели — мост между `ModelSelectionRepository`,
 * `WorkManager` и UI-gating (`WarmupGate`/`ModelBootstrapScreen`).
 *
 * Enqueue при cold-start происходит только если [UserPreferencesRepository.warmupOnLaunchEnabled].
 * В on-demand режиме движок греется лениво при первом инференсе.
 */
@Singleton
class WarmupCoordinator @Inject constructor(
    private val workManager: WorkManager,
    private val modelSelectionRepo: ModelSelectionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val gemmaLocalEngine: Lazy<GemmaLocalEngine>,
) {

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val hasAttachedFlow = modelSelectionRepo.attached
        .map { it != null }
        .distinctUntilChanged()

    private val launchWarmupFlow = userPreferencesRepository.warmupOnLaunchEnabled
        .distinctUntilChanged()

    init {
        combine(hasAttachedFlow, launchWarmupFlow) { isAttached, launchWarmup ->
            isAttached to launchWarmup
        }
            .distinctUntilChanged()
            .onEach { (isAttached, launchWarmup) ->
                when {
                    isAttached && launchWarmup -> enqueueWarmupIfNeeded()
                    !launchWarmup -> releaseWarmupResources()
                }
            }
            .stateIn(scope, SharingStarted.Eagerly, false to false)
    }

    private val workInfoStateFlow = workManager
        .getWorkInfosForUniqueWorkFlow(ModelWarmupWorker.UNIQUE_WORK_NAME)
        .map { infos -> infos.firstOrNull()?.state }
        .distinctUntilChanged()

    val state: StateFlow<WarmupState> = combine(
        hasAttachedFlow,
        workInfoStateFlow,
        launchWarmupFlow,
    ) { hasAttached: Boolean, workState: WorkInfo.State?, launchWarmup: Boolean ->
        reduceWarmupState(
            hasAttachedModel = hasAttached,
            workInfoState = workState,
            launchWarmupEnabled = launchWarmup,
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = WarmupState.Unknown,
    )

    private fun enqueueWarmupIfNeeded() {
        val request = OneTimeWorkRequestBuilder<ModelWarmupWorker>().build()
        workManager.enqueueUniqueWork(
            ModelWarmupWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    private fun releaseWarmupResources() {
        workManager.cancelUniqueWork(ModelWarmupWorker.UNIQUE_WORK_NAME)
        scope.launch {
            runCatching { gemmaLocalEngine.get().release() }
        }
    }
}
