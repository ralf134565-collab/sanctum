// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.di

import com.pocketreflect.app.core.security.DatabaseProvider
import com.pocketreflect.app.data.local.RoomDatabaseProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.zetetic.database.sqlcipher.SQLiteConnection
import net.zetetic.database.sqlcipher.SQLiteDatabaseHook

val SqlCipherKdfHook = object : SQLiteDatabaseHook {
    override fun preKey(connection: SQLiteConnection?) {}
    override fun postKey(connection: SQLiteConnection?) {
        connection?.executeRaw("PRAGMA cipher_kdf_iter = 200000;", null, null)
    }
}

/**
 * DI-модуль для Room-стека (SQLCipher через [SupportOpenHelperFactory]).
 *
 * [com.pocketreflect.app.core.security.DatabaseBootstrap.ensureReadyOnMainThread]
 * должен выполниться в [com.pocketreflect.app.PocketReflectApp.onCreate]
 * до первого [DatabaseProvider.unlock].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {

    @Binds
    @Singleton
    abstract fun bindDatabaseProvider(impl: RoomDatabaseProvider): DatabaseProvider
}
