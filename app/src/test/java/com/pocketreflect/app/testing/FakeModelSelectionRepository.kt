// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.testing

import com.pocketreflect.app.data.ai.EngineBackend
import com.pocketreflect.app.data.repository.AttachedModel
import com.pocketreflect.app.data.repository.ModelSelectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeModelSelectionRepository(
    initialAttached: AttachedModel? = null,
    initialBackend: EngineBackend = EngineBackend.GPU,
) : ModelSelectionRepository {
    private val attachedFlow = MutableStateFlow(initialAttached)
    private val backendFlow = MutableStateFlow(initialBackend)

    override val attached = attachedFlow.asStateFlow()
    override val selectedBackend = backendFlow.asStateFlow()

    override suspend fun setAttached(attached: AttachedModel) {
        attachedFlow.value = attached
    }

    override suspend fun clearAttached() {
        attachedFlow.value = null
    }

    override suspend fun setBackend(backend: EngineBackend) {
        backendFlow.value = backend
    }
}
