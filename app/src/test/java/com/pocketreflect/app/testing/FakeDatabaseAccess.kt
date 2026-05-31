// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.testing

import com.pocketreflect.app.core.security.DatabaseAccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeDatabaseAccess(
    initialReady: Boolean = true,
) : DatabaseAccess {
    private val ready = MutableStateFlow(initialReady)

    override val isReady: Flow<Boolean> = ready

    fun setReady(value: Boolean) {
        ready.value = value
    }

    override suspend fun <T> whenReady(block: suspend () -> T): T = block()

    override fun <T> observeWhenReady(notReadyValue: T, block: () -> Flow<T>): Flow<T> = block()
}
