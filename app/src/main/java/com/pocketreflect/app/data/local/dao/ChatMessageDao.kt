// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pocketreflect.app.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC, id ASC")
    fun observeAll(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE personaId = :personaId ORDER BY timestamp ASC, id ASC")
    fun observeByPersona(personaId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC, id ASC")
    suspend fun findAll(): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE personaId = :personaId ORDER BY timestamp ASC, id ASC")
    suspend fun findAllByPersona(personaId: String): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAll()

    @Query("DELETE FROM chat_messages WHERE personaId = :personaId")
    suspend fun deleteByPersona(personaId: String)

    @Query("SELECT COALESCE(SUM(LENGTH(content)), 0) FROM chat_messages")
    suspend fun totalContentLength(): Int

    @Query("SELECT COALESCE(SUM(LENGTH(content)), 0) FROM chat_messages WHERE personaId = :personaId")
    suspend fun totalContentLengthByPersona(personaId: String): Int

    @Query("SELECT COUNT(*) FROM chat_messages")
    suspend fun count(): Int
}
