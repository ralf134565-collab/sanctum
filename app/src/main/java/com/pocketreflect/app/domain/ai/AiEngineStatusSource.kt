// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.ai

import kotlinx.coroutines.flow.StateFlow

/** Источник статуса локального ИИ для UI (подменяется в тестах). */
interface AiEngineStatusSource {
    val status: StateFlow<AiEngineStatus>
    fun notifyRuntimeFailure()
}
