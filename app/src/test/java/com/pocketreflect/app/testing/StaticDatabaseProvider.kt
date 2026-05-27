// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.testing

import com.pocketreflect.app.core.security.DatabaseProvider
import com.pocketreflect.app.data.local.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** In-memory [AppDatabase] for unit / Robolectric tests. */
class StaticDatabaseProvider(
    private val database: AppDatabase,
) : DatabaseProvider {

    private val _revision = MutableStateFlow(0L)
    override val revision: StateFlow<Long> = _revision.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    override val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    override fun get(): AppDatabase = database

    override fun lock() = Unit

    override fun unlock() {
        _revision.update { it + 1L }
    }
}
