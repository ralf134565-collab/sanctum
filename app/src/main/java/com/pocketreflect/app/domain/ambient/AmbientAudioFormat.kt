// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.ambient

object AmbientAudioFormat {

    const val EXT_OGG = "ogg"
    const val EXT_MP3 = "mp3"

    val BUILTIN_EXTENSION = EXT_OGG

    val SUPPORTED_IMPORT_EXTENSIONS: Set<String> = setOf(EXT_OGG, EXT_MP3)

    fun resolveImportExtension(mimeType: String?, lastPathSegment: String?): String? {
        extensionFromMime(mimeType)?.let { return it }
        val segment = lastPathSegment?.substringAfterLast('.', "")?.lowercase()
        return segment?.takeIf { it in SUPPORTED_IMPORT_EXTENSIONS }
    }

    fun extensionFromMime(mime: String?): String? = when {
        mime.isNullOrBlank() -> null
        mime.contains("ogg") -> EXT_OGG
        mime.contains("mpeg") || mime.contains("mp3") -> EXT_MP3
        else -> null
    }
}
