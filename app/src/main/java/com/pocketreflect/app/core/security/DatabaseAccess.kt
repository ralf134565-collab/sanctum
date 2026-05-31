// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.core.security

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * Единая точка доступа к зашифрованной Room-БД.
 *
 * Flow- и suspend-методы репозиториев должны использовать только этот слой,
 * чтобы не расходились политика lock/unlock и обработка «БД не готова».
 */
interface DatabaseAccess {
    val isReady: Flow<Boolean>

    suspend fun <T> whenReady(block: suspend () -> T): T

    /**
     * @param notReadyValue эмитится, пока [isReady] = false. Должен быть безопасным
     *   sentinel (пустой список, null), чтобы `combine()` в ViewModel не зависал.
     */
    fun <T> observeWhenReady(notReadyValue: T, block: () -> Flow<T>): Flow<T>
}

@Singleton
class DefaultDatabaseAccess @Inject constructor(
    private val databaseProvider: DatabaseProvider,
    private val authSessionHolder: AuthSessionHolder,
) : DatabaseAccess {

    override val isReady: Flow<Boolean> = combine(
        authSessionHolder.isAuthenticated,
        databaseProvider.isLocked,
        databaseProvider.revision,
    ) { authenticated, locked, _ -> authenticated && !locked }

    override suspend fun <T> whenReady(block: suspend () -> T): T {
        isReady.first { it }
        return block()
    }

    override fun <T> observeWhenReady(notReadyValue: T, block: () -> Flow<T>): Flow<T> =
        isReady.flatMapLatest { ready ->
            if (ready) {
                runCatching { block() }
                    .getOrElse { flowOf(notReadyValue) }
                    .catch { emit(notReadyValue) }
            } else {
                flowOf(notReadyValue)
            }
        }
}
