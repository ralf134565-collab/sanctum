// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.local

import android.content.Context
import androidx.room.Room
import com.pocketreflect.app.core.security.DatabaseEncryptionPolicy
import com.pocketreflect.app.core.security.DatabasePassphraseManager
import com.pocketreflect.app.core.security.DatabaseProvider
import com.pocketreflect.app.core.security.SqlCipherLoader
import com.pocketreflect.app.data.local.migration.AppDatabaseMigrations
import com.pocketreflect.app.di.SqlCipherKdfHook
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * Singleton-реализация [DatabaseProvider]: close → rebuild Room при unlock.
 */
@Singleton
class RoomDatabaseProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val passphraseManager: DatabasePassphraseManager,
) : DatabaseProvider {

    private val mutex = Any()

    @Volatile
    private var database: AppDatabase? = null

    @Volatile
    private var locked = true

    private val _revision = MutableStateFlow(0L)
    override val revision: StateFlow<Long> = _revision.asStateFlow()

    private val _isLocked = MutableStateFlow(true)
    override val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    override fun get(): AppDatabase {
        synchronized(mutex) {
            check(!locked) { "Database is locked — call unlock() first" }
            val current = database
            if (current != null && current.isOpen) {
                return current
            }
            return openDatabase().also { database = it }
        }
    }

    override fun lock() {
        synchronized(mutex) {
            database?.let { db ->
                if (db.isOpen) {
                    db.close()
                }
            }
            database = null
            locked = true
            _isLocked.value = true
            passphraseManager.clearPassphraseFromMemory()
        }
    }

    override fun unlock() {
        synchronized(mutex) {
            passphraseManager.getOrCreatePassphrase()
            database?.let { db ->
                if (db.isOpen) {
                    db.close()
                }
            }
            database = openDatabase()
            locked = false
            _isLocked.value = false
            _revision.update { it + 1L }
        }
    }

    private fun openDatabase(): AppDatabase {
        val builder = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME,
        ).addMigrations(
            AppDatabaseMigrations.MIGRATION_1_2,
            AppDatabaseMigrations.MIGRATION_2_3,
            AppDatabaseMigrations.MIGRATION_3_4,
        )

        if (DatabaseEncryptionPolicy.useSqlCipherInThisProcess()) {
            SqlCipherLoader.ensureLoaded()
            val factory = SupportOpenHelperFactory(
                passphraseManager.getOrCreatePassphrase(),
                SqlCipherKdfHook,
                false,
            )
            builder.openHelperFactory(factory)
        }

        return builder.build()
    }
}
