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
    Warming,
    Ready,
    Failed,
}

/**
 * Чистая reducer-функция перехода. Вынесена отдельно от
 * [WarmupCoordinator] ради юнит-тестируемости без WorkManager и без
 * корутин — все 5 переходов покрываются обычными `assertEquals`.
 *
 * @param hasAttachedModel `null` означает «ещё не успели прочитать
 *   DataStore» (рендерим Unknown), `false` — «модель отсутствует»,
 *   `true` — «модель привязана».
 * @param workInfoState текущее состояние уникальной задачи warmup'а.
 *   `null` означает «задача ещё не enqueue-ена» (только что увидели модель).
 *
 * Решения, заложенные в логику:
 *  - Если модель не привязана — игнорируем любое `workInfoState`. Это
 *    защищает от ситуации «модель удалили, пока worker крутился».
 *  - `WorkInfo.State.ENQUEUED`, `RUNNING`, `BLOCKED` все мапятся в
 *    [WarmupState.Warming] — пользователю всё равно, висим мы в очереди
 *    или уже грузим веса; UI одинаков.
 *  - `CANCELLED` мапится в [WarmupState.Failed] — отмена с точки зрения
 *    бутстрапа неотличима от падения; не хотим залипать.
 */
fun reduceWarmupState(
    hasAttachedModel: Boolean?,
    workInfoState: WorkInfo.State?,
): WarmupState {
    if (hasAttachedModel == null) return WarmupState.Unknown
    if (!hasAttachedModel) return WarmupState.NoModel
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
