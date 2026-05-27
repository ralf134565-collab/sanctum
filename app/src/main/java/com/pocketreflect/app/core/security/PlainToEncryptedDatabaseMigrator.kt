// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.core.security

import android.content.Context
import android.util.Log
import com.pocketreflect.app.data.local.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Однократная конвертация незашифрованной SQLite-БД (dev-сборки) в SQLCipher.
 * На свежей установке файла нет — миграция пропускается.
 */
@Singleton
class PlainToEncryptedDatabaseMigrator @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun migrateIfNeeded(passphrase: ByteArray) {
        val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
        if (!dbFile.exists()) return
        if (!isPlainSqliteFile(dbFile)) return

        SqlCipherLoader.ensureLoaded()
        checkpointPlainDatabase(dbFile)

        val backupBase = File(dbFile.parent, "${dbFile.name}$BACKUP_SUFFIX")
        copyDatabaseFiles(dbFile, backupBase)

        val tempFile = File(dbFile.parent, "${dbFile.name}$TEMP_SUFFIX")
        if (tempFile.exists()) tempFile.delete()
        deleteSidecarFiles(tempFile)

        try {
            exportPlainToEncrypted(
                plainFile = dbFile,
                encryptedFile = tempFile,
                passphrase = passphrase,
            )
            deleteDatabaseFiles(dbFile)
            check(tempFile.renameTo(dbFile)) {
                "rename encrypted db failed: ${tempFile.absolutePath}"
            }
            deleteSidecarFiles(backupBase)
            Log.i(TAG, "Plain database encrypted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Encryption migration failed, restoring backup", e)
            deleteDatabaseFiles(dbFile)
            restoreDatabaseFiles(backupBase, dbFile)
            deleteSidecarFiles(tempFile)
            throw e
        }
    }

    private fun exportPlainToEncrypted(
        plainFile: File,
        encryptedFile: File,
        passphrase: ByteArray,
    ) {
        val plainPath = plainFile.absolutePath.replace("'", "''")
        val db = SQLiteDatabase.openOrCreateDatabase(
            encryptedFile.absolutePath,
            passphrase,
            null,
            null,
        )
        try {
            db.execSQL("ATTACH DATABASE '$plainPath' AS plain KEY ''")
            db.rawQuery("SELECT sqlcipher_export('main', 'plain')", null).use { cursor ->
                check(cursor.moveToFirst()) { "sqlcipher_export returned no row" }
            }
            db.execSQL("DETACH DATABASE plain")
        } finally {
            db.close()
        }
    }

    private fun checkpointPlainDatabase(dbFile: File) {
        android.database.sqlite.SQLiteDatabase.openDatabase(
            dbFile.absolutePath,
            null,
            android.database.sqlite.SQLiteDatabase.OPEN_READWRITE,
        ).use { db ->
            db.rawQuery("PRAGMA wal_checkpoint(FULL)", null).close()
        }
    }

    companion object {
        const val TAG = "PlainToEncryptedMigrator"
        const val BACKUP_SUFFIX = ".pre_encrypt_backup"
        private const val TEMP_SUFFIX = ".encrypting"
        private val PLAIN_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

        fun isPlainSqliteFile(file: File): Boolean {
            if (!file.exists() || file.length() < PLAIN_HEADER.size) return false
            val header = ByteArray(PLAIN_HEADER.size)
            file.inputStream().use { input ->
                if (input.read(header) < PLAIN_HEADER.size) return false
            }
            return header.contentEquals(PLAIN_HEADER)
        }

        fun deleteDatabaseFiles(mainFile: File) {
            mainFile.delete()
            deleteSidecarFiles(mainFile)
        }

        fun deleteSidecarFiles(mainFile: File) {
            File(mainFile.path + "-wal").delete()
            File(mainFile.path + "-shm").delete()
        }

        fun copyDatabaseFiles(source: File, destBase: File) {
            copyIfExists(source, destBase)
            copyIfExists(File(source.path + "-wal"), File(destBase.path + "-wal"))
            copyIfExists(File(source.path + "-shm"), File(destBase.path + "-shm"))
        }

        fun restoreDatabaseFiles(backupBase: File, target: File) {
            copyIfExists(backupBase, target)
            copyIfExists(File(backupBase.path + "-wal"), File(target.path + "-wal"))
            copyIfExists(File(backupBase.path + "-shm"), File(target.path + "-shm"))
        }

        private fun copyIfExists(source: File, dest: File) {
            if (!source.exists()) return
            source.copyTo(dest, overwrite = true)
        }
    }
}
