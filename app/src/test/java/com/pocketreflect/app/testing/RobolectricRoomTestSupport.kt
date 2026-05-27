// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.testing

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.pocketreflect.app.data.local.AppDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher

/**
 * Вспомогательные методы для Robolectric + Room.
 *
 * Room-тесты используют `@Config(application = Application::class)` и **не**
 * вызывают [prepareWorkManager] — иначе Robolectric CloseGuard ругается на
 * незакрытую служебную БД WorkManager.
 *
 * [prepareWorkManager] / [shutdownWorkManager] — только для тестов, где явно
 * нужен [androidx.work.WorkManager.getInstance].
 *
 * In-memory Room: [inMemoryDatabase] привязывает coroutine-контекст запросов к
 * [StandardTestDispatcher], чтобы Invalidation Tracker не уезжал на фоновый
 * пул WorkManager после [InMemoryAppDatabase.close] (гонка с Robolectric SQLite).
 */
@OptIn(ExperimentalCoroutinesApi::class)
object RobolectricRoomTestSupport {

    class InMemoryAppDatabase(
        val database: AppDatabase,
        private val queryDispatcher: StandardTestDispatcher,
    ) {
        fun close() {
            runBlocking {
                queryDispatcher.scheduler.advanceUntilIdle()
            }
            shutdownWorkManager()
            database.close()
        }
    }

    fun inMemoryDatabase(context: Context): InMemoryAppDatabase {
        shutdownWorkManager()
        val queryDispatcher = StandardTestDispatcher()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryCoroutineContext(queryDispatcher)
            .build()
        return InMemoryAppDatabase(database, queryDispatcher)
    }

    fun prepareWorkManager(context: Context) {
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.ERROR)
            .setExecutor(SynchronousExecutor())
            .setTaskExecutor(SynchronousExecutor())
            .build()
        try {
            WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        } catch (_: IllegalStateException) {
            WorkManagerTestInitHelper.closeWorkDatabase()
            WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        }
    }

    fun shutdownWorkManager() {
        runCatching { WorkManagerTestInitHelper.closeWorkDatabase() }
    }

    /** @see [InMemoryAppDatabase.close] */
    fun closeDatabase(database: AppDatabase) {
        shutdownWorkManager()
        database.close()
    }
}
