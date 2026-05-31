// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.core.security

import com.pocketreflect.app.data.local.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Tracks lock/unlock calls for [AuthSessionHolder] unit tests. */
class TrackingDatabaseProvider(
    private val database: AppDatabase? = null,
) : DatabaseProvider {

    private val _revision = MutableStateFlow(0L)
    override val revision: StateFlow<Long> = _revision.asStateFlow()

    private val _isLocked = MutableStateFlow(true)
    override val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    var lockInvocations: Int = 0

    var unlockInvocations: Int = 0

    override fun get(): AppDatabase =
        checkNotNull(database) { "Database not configured for this test" }

    override fun lock() {
        lockInvocations++
        _isLocked.value = true
    }

    override fun unlock() {
        unlockInvocations++
        _isLocked.value = false
        _revision.update { it + 1L }
    }
}
