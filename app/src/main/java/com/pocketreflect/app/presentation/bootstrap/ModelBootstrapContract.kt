// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.bootstrap

import androidx.compose.runtime.Immutable
import com.pocketreflect.app.data.ai.WarmupState

/**
 * Минимальный MVI-контракт `ModelBootstrapScreen` (Sub-PR #4).
 *
 * Экран реактивный и без пользовательских действий: единственная задача —
 * показать «готовим ИИ-ментор» пока [WarmupCoordinator] не отдаст финальное
 * состояние. Поэтому `Intent` намеренно отсутствует, а `State` — тонкая
 * обёртка над `WarmupState` с готовыми удобными флагами для UI.
 */
object ModelBootstrapContract {

    @Immutable
    data class State(
        val warmup: WarmupState = WarmupState.Unknown,
    ) {
        /** Завершилось — успешно или нет, пора пропускать в основной UI. */
        val isFinished: Boolean
            get() = warmup == WarmupState.Ready ||
                warmup == WarmupState.Failed ||
                warmup == WarmupState.NoModel

        /** Показывать ли «работаем в режиме поддержки»-сообщение. */
        val hasFailed: Boolean
            get() = warmup == WarmupState.Failed
    }
}
