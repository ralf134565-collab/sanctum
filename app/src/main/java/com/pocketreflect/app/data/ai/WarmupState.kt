// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.ai

import androidx.work.WorkInfo

/**
 * Состояние прогрева локальной модели для UI-gating (Sub-PR #4).
 *
 * Конечный автомат сознательно плоский (без вложенных sealed-классов), чтобы
 * Compose-side switch over `state` оставался однократным `when`-выражением.
 *
 *  - [Unknown] — изначальное состояние до того, как мы получили первый снимок
 *    `attached` из DataStore. Рендерится как пустой тёмный фон — задержка
 *    обычно микро-секундная, специальный UI не нужен.
 *  - [NoModel] — модель не привязана. Bootstrap-экран **не нужен**: журнал
 *    работает на mock-движке, никакого native warmup'а делать не из чего.
 *  - [Idle] — модель привязана, прогрев при запуске выключен, WorkManager
 *    не активен. UI-gate пропускает в основной экран; инференс греет движок
 *    лениво по первому запросу.
 *  - [Warming] — модель привязана, WorkManager-задача в полёте. Показываем
 *    [com.pocketreflect.app.presentation.bootstrap.ModelBootstrapScreen].
 *  - [Ready] — warmup завершился успешно. Следующий пользовательский инференс
 *    отзовётся без cold-start задержки.
 *  - [Failed] — warmup-задача упала. UX обещает пользователю «работаем в
 *    режиме поддержки» (mock-fallback в `EngineCoordinator` уже включён),
 *    и мы тоже пропускаем дальше — экран не должен висеть вечно.
 */
enum class WarmupState {
    Unknown,
    NoModel,
    Idle,
    Warming,
    Ready,
    Failed,
}

/**
 * Чистая reducer-функция перехода. Вынесена отдельно от
 * [WarmupCoordinator] ради юнит-тестируемости без WorkManager и без
 * корутин — все ветви покрываются обычными `assertEquals`.
 *
 * @param hasAttachedModel `null` означает «ещё не успели прочитать
 *   DataStore» (рендерим Unknown), `false` — «модель отсутствует»,
 *   `true` — «модель привязана».
 * @param workInfoState текущее состояние уникальной задачи warmup'а.
 *   `null` означает «задача ещё не enqueue-ена» (только что увидели модель).
 * @param launchWarmupEnabled включён ли прогрев при запуске приложения.
 */
fun reduceWarmupState(
    hasAttachedModel: Boolean?,
    workInfoState: WorkInfo.State?,
    launchWarmupEnabled: Boolean,
): WarmupState {
    if (hasAttachedModel == null) return WarmupState.Unknown
    if (!hasAttachedModel) return WarmupState.NoModel
    if (!launchWarmupEnabled) {
        return when (workInfoState) {
            WorkInfo.State.ENQUEUED,
            WorkInfo.State.RUNNING,
            WorkInfo.State.BLOCKED -> WarmupState.Warming
            else -> WarmupState.Idle
        }
    }
    return when (workInfoState) {
        null,
        WorkInfo.State.ENQUEUED,
        WorkInfo.State.RUNNING,
        WorkInfo.State.BLOCKED -> WarmupState.Warming
        WorkInfo.State.SUCCEEDED -> WarmupState.Ready
        WorkInfo.State.FAILED,
        WorkInfo.State.CANCELLED -> WarmupState.Failed
    }
}
