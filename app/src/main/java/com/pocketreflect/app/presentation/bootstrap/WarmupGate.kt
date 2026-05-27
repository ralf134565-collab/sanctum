// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.bootstrap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pocketreflect.app.data.ai.WarmupState

/**
 * Composable-gate, который решает: показать [ModelBootstrapScreen] или
 * пропустить пользователя в основной UI (Sub-PR #4).
 *
 * Семантика как у [com.pocketreflect.app.core.security.BiometricGate]:
 *  - `Unknown` — пустой тёмный фон (анти-flash). Это «успели запустить
 *    composition раньше первого emit из StateFlow». Обычно микросекунды.
 *  - `Warming` — `ModelBootstrapScreen`.
 *  - Любое финальное состояние (`Ready`/`Failed`/`NoModel`) — рендерим
 *    [content]. В случае `Failed` мы НЕ блокируем UI: координатор/
 *    coordinator engine уже сам обеспечит mock-fallback. Bootstrap-экран
 *    в Failed-режиме показывается только на короткий tick, пока state
 *    не перетечёт дальше — но на практике мы туда не залипаем, потому что
 *    Compose-side `isFinished == true` для Failed.
 *
 * Wait, корректный flow: в Failed `isFinished` — true, значит этот гейт
 * сразу пропустит в content, минуя экран. Это и есть «не залипаем».
 * Экран в Failed-режиме виден только если кто-то откроет его напрямую
 * (сейчас никто не открывает).
 */
@Composable
fun WarmupGate(
    viewModel: ModelBootstrapViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (state.warmup) {
        WarmupState.Unknown -> EmptyTintedBackground()
        WarmupState.Warming -> ModelBootstrapScreen(state = state)
        WarmupState.Ready,
        WarmupState.Failed,
        WarmupState.NoModel -> content()
    }
}

@Composable
private fun EmptyTintedBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    )
}
