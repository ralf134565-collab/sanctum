// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.export.vault

import java.io.InputStream
import java.util.zip.ZipInputStream

internal object VaultZipReader {

    data class ZipEntryData(
        val path: String,
        val content: ByteArray,
    )

    fun readAll(input: InputStream): List<ZipEntryData> {
        val entries = mutableListOf<ZipEntryData>()
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory) {
                    val bytes = zip.readBytes()
                    entries += ZipEntryData(
                        path = entry.name.replace('\\', '/'),
                        content = bytes,
                    )
                }
                zip.closeEntry()
            }
        }
        return entries
    }
}
