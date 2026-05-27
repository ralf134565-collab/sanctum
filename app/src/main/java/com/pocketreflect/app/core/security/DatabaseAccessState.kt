// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.core.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Глобальный статус доступа к Room после bootstrap (миграция / открытие).
 */
sealed interface DatabaseAccessStatus {
    data object Ready : DatabaseAccessStatus
    data class Blocked(val reason: BlockReason) : DatabaseAccessStatus
}

enum class BlockReason {
    MIGRATION_FAILED,
    OPEN_FAILED,
}

@Singleton
class DatabaseAccessState @Inject constructor() {
    private val _status = MutableStateFlow<DatabaseAccessStatus>(DatabaseAccessStatus.Ready)
    val status: StateFlow<DatabaseAccessStatus> = _status.asStateFlow()

    fun markReady() {
        _status.value = DatabaseAccessStatus.Ready
    }

    fun markBlocked(reason: BlockReason) {
        _status.value = DatabaseAccessStatus.Blocked(reason)
    }

    val isBlocked: Boolean
        get() = _status.value is DatabaseAccessStatus.Blocked
}
