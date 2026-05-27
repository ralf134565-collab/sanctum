// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.local.migration

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.pocketreflect.app.data.local.AppDatabase
import com.pocketreflect.app.testing.RobolectricRoomTestSupport
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Проверка [AppDatabaseMigrations.MIGRATION_1_2] без MigrationTestHelper:
 * JVM unit-тесты не всегда упаковывают `app/schemas` в assets (в отличие от androidTest).
 * v1 поднимаем вручную по экспортированной схеме `schemas/.../1.json`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class AppDatabaseMigrationTest {

    @Test
    fun migration_1_to_2_versions() {
        assertEquals(1, AppDatabaseMigrations.MIGRATION_1_2.startVersion)
        assertEquals(2, AppDatabaseMigrations.MIGRATION_1_2.endVersion)
    }

    @Test
    fun migration_2_to_3_versions() {
        assertEquals(2, AppDatabaseMigrations.MIGRATION_2_3.startVersion)
        assertEquals(3, AppDatabaseMigrations.MIGRATION_2_3.endVersion)
    }

    @Test
    fun migrate1To2_preservesJournalAndCreatesChatTable() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val dbName = "migration-manual-test.db"
        context.deleteDatabase(dbName)

        bootstrapVersion1Database(context, dbName)

        val queryDispatcher = StandardTestDispatcher()
        val roomDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabaseMigrations.MIGRATION_1_2)
            .allowMainThreadQueries()
            .setQueryCoroutineContext(queryDispatcher)
            .build()

        val sqlite = roomDb.openHelper.writableDatabase
        sqlite.query("SELECT dayBucket, reflection FROM journal_entries").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("2026-05-01", cursor.getString(0))
            assertEquals("reflection text", cursor.getString(1))
        }
        sqlite.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'chat_messages'",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
        }
        sqlite.query(
            "SELECT name FROM sqlite_master WHERE type = 'index' AND name = 'index_chat_messages_timestamp'",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
        }

        runBlocking { queryDispatcher.scheduler.advanceUntilIdle() }
        RobolectricRoomTestSupport.shutdownWorkManager()
        roomDb.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun migration_3_to_4_versions() {
        assertEquals(3, AppDatabaseMigrations.MIGRATION_3_4.startVersion)
        assertEquals(4, AppDatabaseMigrations.MIGRATION_3_4.endVersion)
    }

    @Test
    fun migrate3To4_addsCustomFieldColumnsWithDefaults() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val dbName = "migration-3-4-test.db"
        context.deleteDatabase(dbName)

        bootstrapVersion3Database(context, dbName)

        val queryDispatcher = StandardTestDispatcher()
        val roomDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabaseMigrations.MIGRATION_3_4)
            .allowMainThreadQueries()
            .setQueryCoroutineContext(queryDispatcher)
            .build()

        val sqlite = roomDb.openHelper.writableDatabase
        sqlite.query(
            "SELECT customFieldAnswer, customFieldQuestion FROM journal_entries WHERE dayBucket = '2026-05-01'",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("", cursor.getString(0))
            assertEquals("", cursor.getString(1))
        }

        runBlocking { queryDispatcher.scheduler.advanceUntilIdle() }
        roomDb.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun migrate2To3_deduplicatesDayBucketAndEnforcesUniqueIndex() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val dbName = "migration-2-3-test.db"
        context.deleteDatabase(dbName)

        bootstrapVersion2Database(context, dbName)

        val queryDispatcher = StandardTestDispatcher()
        val roomDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabaseMigrations.MIGRATION_2_3)
            .allowMainThreadQueries()
            .setQueryCoroutineContext(queryDispatcher)
            .build()

        val sqlite = roomDb.openHelper.writableDatabase
        sqlite.query("SELECT COUNT(*) FROM journal_entries WHERE dayBucket = '2026-05-01'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        sqlite.query(
            "SELECT sql FROM sqlite_master WHERE type = 'index' AND name = 'index_journal_entries_dayBucket'",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.getString(0).contains("UNIQUE"))
        }

        runBlocking { queryDispatcher.scheduler.advanceUntilIdle() }
        roomDb.close()
        context.deleteDatabase(dbName)
    }

    private fun bootstrapVersion2Database(context: Context, dbName: String) {
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(Version2SchemaCallback())
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase
        db.execSQL(
            """
            INSERT INTO journal_entries (
                timestamp, dayBucket, moodTags, microWins, tomorrowTasks,
                reflection, promptShown, aiReflection
            ) VALUES (
                1000, '2026-05-01', 'calm|focused', '', '',
                'older duplicate', 'daily prompt', NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO journal_entries (
                timestamp, dayBucket, moodTags, microWins, tomorrowTasks,
                reflection, promptShown, aiReflection
            ) VALUES (
                2000, '2026-05-01', 'calm|focused', '', '',
                'newer duplicate', 'daily prompt', NULL
            )
            """.trimIndent(),
        )
        helper.close()
    }

    private fun bootstrapVersion3Database(context: Context, dbName: String) {
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(Version3SchemaCallback())
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase
        db.execSQL(
            """
            INSERT INTO journal_entries (
                timestamp, dayBucket, moodTags, microWins, tomorrowTasks,
                reflection, promptShown, aiReflection
            ) VALUES (
                1000, '2026-05-01', 'calm|focused', '', '',
                'reflection text', 'daily prompt', NULL
            )
            """.trimIndent(),
        )
        helper.close()
    }

    private class Version3SchemaCallback : SupportSQLiteOpenHelper.Callback(3) {
        override fun onCreate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS journal_entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    dayBucket TEXT NOT NULL,
                    moodTags TEXT NOT NULL,
                    microWins TEXT NOT NULL,
                    tomorrowTasks TEXT NOT NULL,
                    reflection TEXT NOT NULL,
                    promptShown TEXT NOT NULL,
                    aiReflection TEXT
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_journal_entries_dayBucket ON journal_entries(dayBucket)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_journal_entries_timestamp ON journal_entries(timestamp)",
            )
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
                "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)",
            )
            db.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, 'placeholder')",
            )
        }

        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
            error("v3 bootstrap only")
        }
    }

    private class Version2SchemaCallback : SupportSQLiteOpenHelper.Callback(2) {
        override fun onCreate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS journal_entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    dayBucket TEXT NOT NULL,
                    moodTags TEXT NOT NULL,
                    microWins TEXT NOT NULL,
                    tomorrowTasks TEXT NOT NULL,
                    reflection TEXT NOT NULL,
                    promptShown TEXT NOT NULL,
                    aiReflection TEXT
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_journal_entries_dayBucket ON journal_entries(dayBucket)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_journal_entries_timestamp ON journal_entries(timestamp)",
            )
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
                "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)",
            )
            db.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, 'placeholder')",
            )
        }

        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
            error("v2 bootstrap only")
        }
    }

    private fun bootstrapVersion1Database(context: Context, dbName: String) {
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(Version1SchemaCallback())
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase
        db.execSQL(
            """
            INSERT INTO journal_entries (
                timestamp, dayBucket, moodTags, microWins, tomorrowTasks,
                reflection, promptShown, aiReflection
            ) VALUES (
                1000, '2026-05-01', 'calm|focused', '', '',
                'reflection text', 'daily prompt', NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO ai_trend_profiles (
                periodStart, periodEnd, generatedAt, entryCount,
                summary, structuredJson, schemaVersion
            ) VALUES (
                1, 2, 3, 1, 'summary', NULL, 1
            )
            """.trimIndent(),
        )
        helper.close()
    }

    /**
     * Схема v1 — из `app/schemas/com.pocketreflect.app.data.local.AppDatabase/1.json`.
     */
    private class Version1SchemaCallback : SupportSQLiteOpenHelper.Callback(1) {
        override fun onCreate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS journal_entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    dayBucket TEXT NOT NULL,
                    moodTags TEXT NOT NULL,
                    microWins TEXT NOT NULL,
                    tomorrowTasks TEXT NOT NULL,
                    reflection TEXT NOT NULL,
                    promptShown TEXT NOT NULL,
                    aiReflection TEXT
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_journal_entries_dayBucket ON journal_entries(dayBucket)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_journal_entries_timestamp ON journal_entries(timestamp)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS ai_trend_profiles (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    periodStart INTEGER NOT NULL,
                    periodEnd INTEGER NOT NULL,
                    generatedAt INTEGER NOT NULL,
                    entryCount INTEGER NOT NULL,
                    summary TEXT NOT NULL,
                    structuredJson TEXT,
                    schemaVersion INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)",
            )
            db.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, '64a867e883e7c8aaea0764b784bb79fc')",
            )
        }

        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
            error("v1 bootstrap only")
        }
    }
}
