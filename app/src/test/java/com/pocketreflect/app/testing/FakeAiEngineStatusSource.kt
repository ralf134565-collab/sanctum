// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.testing

import com.pocketreflect.app.domain.ai.AiEngineStatus
import com.pocketreflect.app.domain.ai.AiEngineStatusSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAiEngineStatusSource(
    initial: AiEngineStatus = AiEngineStatus.MODEL_OFFLINE,
) : AiEngineStatusSource {
    private val _status = MutableStateFlow(initial)
    override val status: StateFlow<AiEngineStatus> = _status.asStateFlow()

    fun setStatus(value: AiEngineStatus) {
        _status.value = value
    }

    override fun notifyRuntimeFailure() {
        _status.value = AiEngineStatus.FALLBACK
    }
}
