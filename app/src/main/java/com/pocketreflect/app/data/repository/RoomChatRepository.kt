// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.repository

import com.pocketreflect.app.core.security.AuthSessionHolder
import com.pocketreflect.app.core.security.DatabaseProvider
import com.pocketreflect.app.domain.chat.ChatMessage
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Singleton
class RoomChatRepository @Inject constructor(
    private val databaseProvider: DatabaseProvider,
    private val authSessionHolder: Lazy<AuthSessionHolder>,
) : ChatRepository {

    private fun chatMessageDao() = databaseProvider.get().chatMessageDao()

    private fun dbReadyFlow(): Flow<Boolean> = combine(
        authSessionHolder.get().isAuthenticated,
        databaseProvider.isLocked,
        databaseProvider.revision,
    ) { authenticated, locked, _ -> authenticated && !locked }

    override fun observeMessages(): Flow<List<ChatMessage>> =
        dbReadyFlow().flatMapLatest { ready ->
            if (ready) {
                runCatching {
                    chatMessageDao().observeAll().map { entities ->
                        entities.mapNotNull { it.toDomain() }
                    }
                }.getOrElse { emptyFlow() }
                .catch { emit(emptyList()) }
            } else {
                emptyFlow()
            }
        }

    override fun observeMessages(personaId: String): Flow<List<ChatMessage>> =
        dbReadyFlow().flatMapLatest { ready ->
            if (ready) {
                runCatching {
                    chatMessageDao().observeByPersona(personaId).map { entities ->
                        entities.mapNotNull { it.toDomain() }
                    }
                }.getOrElse { emptyFlow() }
                .catch { emit(emptyList()) }
            } else {
                emptyFlow()
            }
        }

    override suspend fun insert(message: ChatMessage): Long =
        chatMessageDao().insert(message.toEntity())

    override suspend fun clearAll() {
        chatMessageDao().deleteAll()
    }

    override suspend fun clearPersonaChat(personaId: String) {
        chatMessageDao().deleteByPersona(personaId)
    }

    override suspend fun totalContentLength(): Int =
        chatMessageDao().totalContentLength()

    override suspend fun totalContentLength(personaId: String): Int =
        chatMessageDao().totalContentLengthByPersona(personaId)

    override suspend fun messageCount(): Int =
        chatMessageDao().count()
}
