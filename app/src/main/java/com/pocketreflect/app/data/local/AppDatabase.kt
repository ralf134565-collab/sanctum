// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pocketreflect.app.data.local.converter.JournalConverters
import com.pocketreflect.app.data.local.dao.AITrendProfileDao
import com.pocketreflect.app.data.local.dao.ChatMessageDao
import com.pocketreflect.app.data.local.dao.JournalDao
import com.pocketreflect.app.data.local.entity.AITrendProfile
import com.pocketreflect.app.data.local.entity.ChatMessageEntity
import com.pocketreflect.app.data.local.entity.JournalEntry

/**
 * Корневая Room-БД приложения.
 *
 * Важные архитектурные решения:
 *  - `exportSchema = true` + `room.schemaLocation` в build.gradle.kts:
 *    каждый bump `version` будет коммитить JSON-схему в `app/schemas/`.
 *    Это сделает миграции воспроизводимыми (а не «прошёл локально — поехали»).
 *  - НЕ используем `fallbackToDestructiveMigration` — у пользователя в БД
 *    его глубоко личные данные, мы не имеем права их удалять при апдейте.
 *  - SQLCipher: [net.zetetic.database.sqlcipher.SupportOpenHelperFactory] в
 *    [com.pocketreflect.app.di.DatabaseModule]; bootstrap в [PocketReflectApp].
 */
@Database(
    entities = [
        JournalEntry::class,
        AITrendProfile::class,
        ChatMessageEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(JournalConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun journalDao(): JournalDao
    abstract fun aiTrendProfileDao(): AITrendProfileDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        const val DATABASE_NAME = "pocket_reflect.db"
    }
}
