// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Миграции [com.pocketreflect.app.data.local.AppDatabase].
 * Каждый bump version — новый объект здесь + JSON в `app/schemas/`.
 */
object AppDatabaseMigrations {

    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS chat_messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    role TEXT NOT NULL,
                    content TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    personaId TEXT
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_chat_messages_timestamp ON chat_messages(timestamp)",
            )
        }
    }

    /** Дедупликация по dayBucket + UNIQUE INDEX — одна запись на календарный день. */
    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                DELETE FROM journal_entries
                WHERE id NOT IN (
                    SELECT MAX(id) FROM journal_entries GROUP BY dayBucket
                )
                """.trimIndent(),
            )
            db.execSQL("DROP INDEX IF EXISTS index_journal_entries_dayBucket")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_journal_entries_dayBucket ON journal_entries(dayBucket)",
            )
        }
    }

    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE journal_entries ADD COLUMN customFieldAnswer TEXT NOT NULL DEFAULT ''",
            )
            db.execSQL(
                "ALTER TABLE journal_entries ADD COLUMN customFieldQuestion TEXT NOT NULL DEFAULT ''",
            )
        }
    }
}
