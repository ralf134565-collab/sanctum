// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.export.vault

import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal data class VaultArchiveFile(
    val path: String,
    val content: ByteArray,
)

internal object VaultZipWriter {

    fun write(files: List<VaultArchiveFile>, out: OutputStream) {
        ZipOutputStream(out).use { zip ->
            files.forEach { file ->
                val entry = ZipEntry(normalizePath(file.path))
                zip.putNextEntry(entry)
                zip.write(file.content)
                zip.closeEntry()
            }
        }
    }

    private fun normalizePath(path: String): String =
        path.replace('\\', '/').trimStart('/')
}
