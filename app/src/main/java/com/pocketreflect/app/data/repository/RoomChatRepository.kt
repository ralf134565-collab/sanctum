// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.repository

import com.pocketreflect.app.core.security.DatabaseAccess
import com.pocketreflect.app.core.security.DatabaseProvider
import com.pocketreflect.app.domain.chat.ChatMessage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomChatRepository @Inject constructor(
    private val databaseProvider: DatabaseProvider,
    private val databaseAccess: DatabaseAccess,
) : ChatRepository {

    override fun observeMessages(): Flow<List<ChatMessage>> =
        databaseAccess.observeWhenReady(emptyList()) {
            databaseProvider.get().chatMessageDao().observeAll().map { entities ->
                entities.mapNotNull { it.toDomain() }
            }
        }

    override fun observeMessages(personaId: String): Flow<List<ChatMessage>> =
        databaseAccess.observeWhenReady(emptyList()) {
            databaseProvider.get().chatMessageDao().observeByPersona(personaId).map { entities ->
                entities.mapNotNull { it.toDomain() }
            }
        }

    override suspend fun insert(message: ChatMessage): Long =
        databaseAccess.whenReady {
            databaseProvider.get().chatMessageDao().insert(message.toEntity())
        }

    override suspend fun clearAll() {
        databaseAccess.whenReady {
            databaseProvider.get().chatMessageDao().deleteAll()
        }
    }

    override suspend fun clearPersonaChat(personaId: String) {
        databaseAccess.whenReady {
            databaseProvider.get().chatMessageDao().deleteByPersona(personaId)
        }
    }

    override suspend fun totalContentLength(): Int =
        databaseAccess.whenReady {
            databaseProvider.get().chatMessageDao().totalContentLength()
        }

    override suspend fun totalContentLength(personaId: String): Int =
        databaseAccess.whenReady {
            databaseProvider.get().chatMessageDao().totalContentLengthByPersona(personaId)
        }

    override suspend fun messageCount(): Int =
        databaseAccess.whenReady {
            databaseProvider.get().chatMessageDao().count()
        }
}
