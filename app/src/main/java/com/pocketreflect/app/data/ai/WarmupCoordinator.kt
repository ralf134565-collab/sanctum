// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.ai

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.pocketreflect.app.core.work.ModelWarmupWorker
import com.pocketreflect.app.data.repository.ModelSelectionRepository
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

/**
 * Координатор прогрева модели — мост между `ModelSelectionRepository`,
 * `WorkManager` и UI-gating (`WarmupGate`/`ModelBootstrapScreen`).
 *
 * ### Зачем
 * `LiteRtGemmaEngine.warmUp()` греется 10–30 с. Если этот вызов сделать
 * лениво на первом `generatePromptResponse`, пользователь увидит немой UI на
 * `JournalScreen` после нажатия «Завершить день». Мы хотим, чтобы прогрев
 * случился ОДИН раз на cold-start процесса (когда модель уже привязана),
 * чтобы дальше любой инференс шёл «по горячему».
 *
 * ### Архитектура потока
 *  1. Подписываемся на [ModelSelectionRepository.attached]: как только видим
 *     первое не-null значение, делаем `enqueueUniqueWork(..., KEEP, ...)`.
 *     `KEEP` нужен, чтобы при rotation/повторных входах в gate не плодить
 *     дубликаты.
 *  2. Параллельно подписываемся на `WorkManager.getWorkInfosForUniqueWorkFlow`.
 *     Из самого свежего `WorkInfo` берём `.state` и пропускаем через
 *     [reduceWarmupState] вместе со снимком `attached`.
 *  3. Результат — горячий [StateFlow]&lt;[WarmupState]&gt;, который читает VM.
 *
 * ### Lifetime
 * `@Singleton` — координатор должен переживать rotation Activity и navigation;
 * SharedFlow собран `SharingStarted.WhileSubscribed(5_000)` для деликатного
 * unsubscribe (5 секунд буфера, чтобы быстрое recomposition не дёргало
 * WorkManager-подписку).
 *
 * Зависимость на `WorkModule.provideWorkManager` — Singleton WorkManager
 * (тоже инжектится Hilt'ом), что даёт нам инвариант «один WorkManager на
 * процесс».
 */
@Singleton
class WarmupCoordinator @Inject constructor(
    private val workManager: WorkManager,
    private val modelSelectionRepo: ModelSelectionRepository,
) {

    /**
     * Свой scope живёт всю жизнь процесса. `SupervisorJob` — чтобы падение
     * одной подписки не уронило всю координацию; `Dispatchers.Default` —
     * combining flows и enqueue WorkManager-задачи compute-bound.
     */
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Только факт наличия модели важен reducer'у — не содержимое
     * `AttachedModel`. State `Unknown` отражается в [state] через
     * `initialValue` ниже, до первого emit'а из combine.
     */
    private val hasAttachedFlow = modelSelectionRepo.attached
        .map { it != null }
        .distinctUntilChanged()
        .onEach { isAttached ->
            if (isAttached) enqueueWarmupIfNeeded()
        }

    private val workInfoStateFlow = workManager
        .getWorkInfosForUniqueWorkFlow(ModelWarmupWorker.UNIQUE_WORK_NAME)
        .map { infos -> infos.firstOrNull()?.state }
        .distinctUntilChanged()

    val state: StateFlow<WarmupState> = combine(
        hasAttachedFlow,
        workInfoStateFlow,
    ) { hasAttached: Boolean, workState: WorkInfo.State? ->
        reduceWarmupState(hasAttachedModel = hasAttached, workInfoState = workState)
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = WarmupState.Unknown,
    )

    /**
     * Enqueue с `ExistingWorkPolicy.KEEP`: если задача уже в очереди или
     * выполняется — повторный enqueue ничего не делает. Это критично,
     * потому что наш `hasAttachedFlow.onEach` будет дёргаться при каждом
     * пересоздании Activity (получает новое значение из cold cache).
     */
    private fun enqueueWarmupIfNeeded() {
        val request = OneTimeWorkRequestBuilder<ModelWarmupWorker>().build()
        workManager.enqueueUniqueWork(
            ModelWarmupWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
