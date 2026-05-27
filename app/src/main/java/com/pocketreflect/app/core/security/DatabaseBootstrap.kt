// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.core.security

import android.content.Context
import android.net.Uri
import android.util.Log
import com.pocketreflect.app.data.local.AppDatabase
import com.pocketreflect.app.data.repository.DailyPromptsHistoryRepository
import com.pocketreflect.app.data.repository.UserPreferencesRepository
import com.pocketreflect.app.data.transfer.BackupRepository
import com.pocketreflect.app.data.transfer.ImportFileDecoder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Подготовка БД до первого доступа Hilt [AppDatabase]: SQLCipher + plain→encrypted.
 */
@Singleton
class DatabaseBootstrap @Inject constructor(
    @ApplicationContext private val context: Context,
    private val passphraseManager: DatabasePassphraseManager,
    private val plainMigrator: PlainToEncryptedDatabaseMigrator,
    private val accessState: DatabaseAccessState,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val promptsHistory: DailyPromptsHistoryRepository,
    private val backupRepository: BackupRepository,
    private val decoder: ImportFileDecoder,
) {

    fun ensureReadyOnMainThread() {
        if (accessState.isBlocked) return
        if (!DatabaseEncryptionPolicy.useSqlCipherInThisProcess()) {
            accessState.markReady()
            return
        }
        runCatching {
            SqlCipherLoader.ensureLoaded()
            val passphrase = passphraseManager.getOrCreatePassphrase()
            plainMigrator.migrateIfNeeded(passphrase)
            verifyOpens(passphrase)
            accessState.markReady()
        }.onFailure { error ->
            Log.e(TAG, "Database bootstrap failed", error)
            val reason = if (PlainToEncryptedDatabaseMigrator.isPlainSqliteFile(
                    context.getDatabasePath(AppDatabase.DATABASE_NAME),
                )
            ) {
                BlockReason.MIGRATION_FAILED
            } else {
                BlockReason.OPEN_FAILED
            }
            accessState.markBlocked(reason)
        }
    }

    suspend fun startFresh() = withContext(Dispatchers.IO) {
        deleteLocalDatabaseFiles()
        passphraseManager.clearPassphrase()
        passphraseManager.getOrCreatePassphrase()
        userPreferencesRepository.clearChatPreferences()
        promptsHistory.clear()
        accessState.markReady()
    }

    suspend fun importAndUnblock(
        uri: Uri,
        password: CharArray,
        overwrite: Boolean,
    ) = withContext(Dispatchers.IO) {
        val dto = context.contentResolver.openInputStream(uri).use { stream ->
            requireNotNull(stream) { "Cannot open backup URI" }
            decoder.decode(stream, password)
        }
        deleteLocalDatabaseFiles()
        passphraseManager.clearPassphrase()
        passphraseManager.getOrCreatePassphrase()
        accessState.markReady()
        backupRepository.importDto(dto, overwrite)
    }

    private fun verifyOpens(passphrase: ByteArray) {
        val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
        if (!dbFile.exists()) return
        SQLiteDatabase.openDatabase(
            dbFile.absolutePath,
            passphrase,
            null,
            SQLiteDatabase.OPEN_READONLY,
            null,
            com.pocketreflect.app.di.SqlCipherKdfHook,
        ).use { db ->
            db.rawQuery("SELECT 1", null).close()
        }
    }

    private fun deleteLocalDatabaseFiles() {
        val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
        context.deleteDatabase(AppDatabase.DATABASE_NAME)
        PlainToEncryptedDatabaseMigrator.deleteDatabaseFiles(dbFile)
        val backup = File(dbFile.parent, "${dbFile.name}${PlainToEncryptedDatabaseMigrator.BACKUP_SUFFIX}")
        PlainToEncryptedDatabaseMigrator.deleteDatabaseFiles(backup)
    }

    private companion object {
        const val TAG = "DatabaseBootstrap"
    }
}
