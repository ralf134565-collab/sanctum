// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.ambient

import android.content.Context
import android.net.Uri
import com.pocketreflect.app.R
import com.pocketreflect.app.core.locale.AppLanguage
import com.pocketreflect.app.domain.ambient.AmbientAudioFormat
import com.pocketreflect.app.domain.ambient.AmbientMusicPolicy
import com.pocketreflect.app.domain.ambient.AmbientTrack
import com.pocketreflect.app.domain.ambient.AmbientTrackSource
import com.pocketreflect.app.domain.ambient.StoredCustomAmbientTrack
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Singleton
class AmbientMusicStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val json = Json { ignoreUnknownKeys = true }

    private val musicDir: File
        get() = File(context.filesDir, MUSIC_DIR).apply { mkdirs() }

    private val cacheDir: File
        get() = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }

    fun parseCustomTracks(rawJson: String): List<StoredCustomAmbientTrack> {
        if (rawJson.isBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(StoredCustomAmbientTrack.serializer()), rawJson)
        }.getOrElse { emptyList() }
    }

    fun encodeCustomTracks(tracks: List<StoredCustomAmbientTrack>): String =
        json.encodeToString(ListSerializer(StoredCustomAmbientTrack.serializer()), tracks)

    fun resolvePlaylist(
        customTracksJson: String,
        language: AppLanguage,
    ): List<AmbientTrack> {
        val builtin = resolveBuiltinTracks(language)
        val custom = parseCustomTracks(customTracksJson).mapNotNull { stored ->
            val file = File(musicDir, stored.fileName)
            if (!file.exists() || file.length() == 0L) return@mapNotNull null
            AmbientTrack(
                id = stored.id,
                title = stored.displayName,
                uri = Uri.fromFile(file),
                source = AmbientTrackSource.USER_FILE,
            )
        }
        return builtin + custom
    }

    fun resolveBuiltinTracks(language: AppLanguage): List<AmbientTrack> =
        BUILTIN_ASSET_FILES.mapNotNull { spec -> resolveBuiltinTrack(spec, language) }

    private fun resolveBuiltinTrack(spec: BuiltinAssetSpec, language: AppLanguage): AmbientTrack? {
        val cached = cachedBuiltinFile(spec.cacheFileName)
        if (!cached.exists() || cached.length() == 0L) {
            val copied = copyBuiltinFromAssets(spec.assetFileName, cached) ||
                spec.legacyAssetFileName?.let { copyBuiltinFromAssets(it, cached) } == true
            if (!copied) return null
        }
        return AmbientTrack(
            id = spec.id,
            title = context.getString(
                if (language.isEnglish) spec.titleEnRes else spec.titleRuRes,
            ),
            uri = Uri.fromFile(cached),
            source = AmbientTrackSource.BUILTIN_ASSET,
        )
    }

    suspend fun importCustomTrack(
        sourceUri: Uri,
        displayName: String,
        existing: List<StoredCustomAmbientTrack>,
    ): Result<StoredCustomAmbientTrack> = runCatching {
        require(existing.size < AmbientMusicPolicy.MAX_CUSTOM_TRACKS) {
            "max_custom_tracks"
        }
        context.contentResolver.openFileDescriptor(sourceUri, "r")?.use { pfd ->
            val size = pfd.statSize
            if (size > AmbientMusicPolicy.MAX_CUSTOM_TRACK_BYTES) {
                error("file_too_large")
            }
        } ?: error("cannot_open")

        val extension = AmbientAudioFormat.resolveImportExtension(
            mimeType = context.contentResolver.getType(sourceUri),
            lastPathSegment = sourceUri.lastPathSegment,
        ) ?: error("unsupported_format")
        val id = UUID.randomUUID().toString()
        val fileName = "$id.$extension"
        val dest = File(musicDir, fileName)
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        } ?: error("cannot_copy")

        val name = displayName.trim().take(AmbientMusicPolicy.MAX_DISPLAY_NAME_LENGTH)
            .ifBlank { dest.nameWithoutExtension }
        StoredCustomAmbientTrack(
            id = id,
            displayName = name,
            fileName = fileName,
        )
    }

    fun deleteCustomTrack(
        track: StoredCustomAmbientTrack,
        existing: List<StoredCustomAmbientTrack>,
    ): List<StoredCustomAmbientTrack> {
        File(musicDir, track.fileName).delete()
        return existing.filter { it.id != track.id }
    }

    private fun cachedBuiltinFile(fileName: String): File = File(cacheDir, fileName)

    private fun copyBuiltinFromAssets(fileName: String, dest: File): Boolean =
        runCatching {
            context.assets.open("$ASSETS_PREFIX/$fileName").use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            true
        }.getOrDefault(false)

    private data class BuiltinAssetSpec(
        val id: String,
        val assetFileName: String,
        val cacheFileName: String,
        val titleRuRes: Int,
        val titleEnRes: Int,
        val legacyAssetFileName: String? = null,
    )

    private companion object {
        const val MUSIC_DIR = "ambient_music"
        const val CACHE_DIR = "ambient_builtin_cache"
        const val ASSETS_PREFIX = "ambient"

        val BUILTIN_ASSET_FILES = listOf(
            BuiltinAssetSpec(
                id = "builtin_dark_refuge",
                assetFileName = "Dark Refuge.ogg",
                cacheFileName = "dark_refuge.ogg",
                titleRuRes = R.string.ambient_track_1_title_ru,
                titleEnRes = R.string.ambient_track_1_title_en,
                legacyAssetFileName = "track_1.ogg",
            ),
            BuiltinAssetSpec(
                id = "builtin_ethereal_clarity",
                assetFileName = "Ethereal Clarity.ogg",
                cacheFileName = "ethereal_clarity.ogg",
                titleRuRes = R.string.ambient_track_2_title_ru,
                titleEnRes = R.string.ambient_track_2_title_en,
                legacyAssetFileName = "track_2.ogg",
            ),
            BuiltinAssetSpec(
                id = "builtin_warm_cocoon",
                assetFileName = "Warm Cocoon.ogg",
                cacheFileName = "warm_cocoon.ogg",
                titleRuRes = R.string.ambient_track_3_title_ru,
                titleEnRes = R.string.ambient_track_3_title_en,
                legacyAssetFileName = "track_3.ogg",
            ),
            BuiltinAssetSpec(
                id = "builtin_hypnotic_stream",
                assetFileName = "Hypnotic Stream.ogg",
                cacheFileName = "hypnotic_stream.ogg",
                titleRuRes = R.string.ambient_track_4_title_ru,
                titleEnRes = R.string.ambient_track_4_title_en,
            ),
            BuiltinAssetSpec(
                id = "builtin_ray_of_hope",
                assetFileName = "Ray of Hope.ogg",
                cacheFileName = "ray_of_hope.ogg",
                titleRuRes = R.string.ambient_track_5_title_ru,
                titleEnRes = R.string.ambient_track_5_title_en,
            ),
        )
    }
}
