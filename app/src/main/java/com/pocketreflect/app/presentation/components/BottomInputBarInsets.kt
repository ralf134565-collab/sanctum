// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Отступы для нижней панели ввода / CTA во вложенном [Scaffold].
 *
 * - [imePadding] — единственный подъём над клавиатурой (в манифесте [adjustNothing]).
 * - [navigationBarsPadding] только при открытой IME: [RootScaffold] скрывает
 *   [BottomNavBar], иначе на gesture-nav (OnePlus и др.) поле уезжает под
 *   системные кнопки или получает лишний зазор, когда бар ещё виден.
 * - [keyboardClosedBottomPadding] — отступ снизу при закрытой клавиатуре (для отделения от BottomNavBar).
 */
fun Modifier.bottomInputBarInsets(keyboardClosedBottomPadding: Dp = 0.dp): Modifier = composed {
    val density = LocalDensity.current
    val imeOpen = WindowInsets.ime.getBottom(density) > 0
    Modifier
        .imePadding()
        .then(
            if (imeOpen) {
                Modifier.navigationBarsPadding()
            } else {
                Modifier.padding(bottom = keyboardClosedBottomPadding)
            }
        )
}
