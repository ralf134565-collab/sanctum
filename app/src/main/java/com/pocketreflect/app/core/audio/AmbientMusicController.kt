// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.core.audio

import android.content.Context
import androidx.annotation.MainThread
import com.pocketreflect.app.core.locale.AppLanguage
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.pocketreflect.app.core.locale.AppLanguageResolver
import com.pocketreflect.app.data.ambient.AmbientMusicStorage
import com.pocketreflect.app.data.repository.UserPreferencesRepository
import com.pocketreflect.app.domain.ambient.AmbientMusicPolicy
import com.pocketreflect.app.domain.ambient.AmbientTrack
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class AmbientMusicController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val storage: AmbientMusicStorage,
    private val appLanguageResolver: AppLanguageResolver,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        repeatMode = Player.REPEAT_MODE_ALL
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        setAudioAttributes(audioAttributes, true)
        addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    syncPlayingState()
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val trackId = mediaItem?.mediaId ?: return
                    scope.launch {
                        userPreferencesRepository.setAmbientMusicSelectedTrackId(trackId)
                    }
                    updateCurrentTrackTitle(trackId)
                }
            },
        )
    }

    private var appInForeground = false
    private var latestTracks: List<AmbientTrack> = emptyList()
    private var volumePersistJob: Job? = null
    private var playlistSignature: String? = null

    private val _uiState = MutableStateFlow(AmbientPlaybackUiState())
    val uiState: StateFlow<AmbientPlaybackUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            userPreferencesRepository.ambientMusicVolumePercent.collect { percent ->
                val volume = AmbientMusicPolicy.volumeFromPercent(percent)
                exoPlayer.volume = volume
                _uiState.update { it.copy(volume = volume) }
            }
        }
        scope.launch {
            userPreferencesRepository.ambientMusicPausedByUser.collect { paused ->
                _uiState.update { it.copy(pausedByUser = paused) }
                syncPlaybackFromPausePref()
            }
        }
        scope.launch {
            combine(
                userPreferencesRepository.ambientMusicEnabled,
                userPreferencesRepository.ambientMusicSelectedTrackId,
                userPreferencesRepository.ambientMusicCustomTracksJson,
                appLanguageResolver.resolved,
            ) { enabled, selectedId, customJson, language ->
                PlaylistSnapshot(enabled, selectedId, customJson, language)
            }.distinctUntilChanged()
                .collect { snapshot ->
                    applyPlaylist(snapshot)
                }
        }
    }

    @MainThread
    fun onAppForeground() {
        appInForeground = true
        syncPlaybackFromPausePref()
    }

    @MainThread
    fun onAppBackground() {
        appInForeground = false
        exoPlayer.pause()
        syncPlayingState()
    }

    fun togglePlayPause() {
        val state = _uiState.value
        if (!state.featureEnabled || !state.canPlay) return
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            scope.launch { userPreferencesRepository.setAmbientMusicPausedByUser(true) }
        } else {
            scope.launch { userPreferencesRepository.setAmbientMusicPausedByUser(false) }
            exoPlayer.play()
        }
        syncPlayingState()
    }

    fun play() {
        if (!_uiState.value.canPlay) return
        scope.launch { userPreferencesRepository.setAmbientMusicPausedByUser(false) }
        exoPlayer.play()
        syncPlayingState()
    }

    fun pause() {
        exoPlayer.pause()
        syncPlayingState()
    }

    fun skipNext() {
        if (exoPlayer.mediaItemCount == 0) return
        exoPlayer.seekToNextMediaItem()
        exoPlayer.play()
        syncPlayingState()
    }

    fun skipPrevious() {
        if (exoPlayer.mediaItemCount == 0) return
        exoPlayer.seekToPreviousMediaItem()
        exoPlayer.play()
        syncPlayingState()
    }

    fun setVolume(volume: Float) {
        val normalized = AmbientMusicPolicy.normalizeVolume(volume)
        exoPlayer.volume = normalized
        _uiState.update { it.copy(volume = normalized) }
        volumePersistJob?.cancel()
        volumePersistJob = scope.launch {
            delay(VOLUME_PERSIST_DEBOUNCE_MS)
            userPreferencesRepository.setAmbientMusicVolumePercent(
                AmbientMusicPolicy.volumeToPercent(normalized),
            )
        }
    }

    fun selectTrack(trackId: String) {
        val index = latestTracks.indexOfFirst { it.id == trackId }
        if (index < 0) return
        scope.launch { userPreferencesRepository.setAmbientMusicSelectedTrackId(trackId) }
        exoPlayer.seekToDefaultPosition(index)
        if (appInForeground && !_uiState.value.pausedByUser) {
            exoPlayer.play()
        }
        updateCurrentTrackTitle(trackId)
        syncPlayingState()
    }

    private suspend fun applyPlaylist(snapshot: PlaylistSnapshot) {
        val tracks = withContext(Dispatchers.IO) {
            storage.resolvePlaylist(snapshot.customJson, snapshot.language)
        }
        latestTracks = tracks

        val signature = buildPlaylistSignature(snapshot, tracks)
        val playlistUnchanged = signature == playlistSignature && exoPlayer.mediaItemCount > 0

        if (!snapshot.enabled || tracks.isEmpty()) {
            playlistSignature = null
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            _uiState.update {
                it.copy(
                    featureEnabled = snapshot.enabled,
                    tracks = tracks,
                    canPlay = false,
                    isPlaying = false,
                    currentTrackId = null,
                    currentTrackTitle = "",
                )
            }
            return
        }

        val selectedId = tracks.firstOrNull { it.id == snapshot.selectedId }?.id
            ?: tracks.first().id
        val startIndex = tracks.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)

        if (!playlistUnchanged) {
            playlistSignature = signature
            val mediaItems = tracks.map { track ->
                MediaItem.Builder()
                    .setUri(track.uri)
                    .setMediaId(track.id)
                    .build()
            }
            val wasPlaying = exoPlayer.isPlaying
            exoPlayer.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
            exoPlayer.prepare()
            if (wasPlaying && !_uiState.value.pausedByUser) {
                exoPlayer.play()
            }
        }

        _uiState.update {
            it.copy(
                featureEnabled = snapshot.enabled,
                tracks = tracks,
                canPlay = true,
                currentTrackId = selectedId,
                currentTrackTitle = tracks[startIndex].title,
            )
        }

        if (snapshot.selectedId != selectedId) {
            userPreferencesRepository.setAmbientMusicSelectedTrackId(selectedId)
        }

        syncPlaybackFromPausePref()
    }

    private fun buildPlaylistSignature(snapshot: PlaylistSnapshot, tracks: List<AmbientTrack>): String =
        buildString {
            append(snapshot.enabled)
            append('|')
            append(snapshot.selectedId)
            append('|')
            append(snapshot.language.storageKey.orEmpty())
            append('|')
            tracks.joinToString(",") { "${it.id}:${it.uri}" }
        }

    private fun syncPlaybackFromPausePref() {
        val state = _uiState.value
        if (!appInForeground || !state.canPlay) {
            exoPlayer.pause()
            syncPlayingState()
            return
        }
        if (state.pausedByUser) {
            exoPlayer.pause()
        }
        syncPlayingState()
    }

    private fun syncPlayingState() {
        _uiState.update { it.copy(isPlaying = exoPlayer.isPlaying) }
    }

    private fun updateCurrentTrackTitle(trackId: String) {
        val title = latestTracks.firstOrNull { it.id == trackId }?.title.orEmpty()
        _uiState.update { it.copy(currentTrackId = trackId, currentTrackTitle = title) }
    }

    private data class PlaylistSnapshot(
        val enabled: Boolean,
        val selectedId: String,
        val customJson: String,
        val language: AppLanguage,
    )

    private companion object {
        const val VOLUME_PERSIST_DEBOUNCE_MS = 400L
    }
}

data class AmbientPlaybackUiState(
    val featureEnabled: Boolean = true,
    val isPlaying: Boolean = false,
    val pausedByUser: Boolean = true,
    val volume: Float = AmbientMusicPolicy.DEFAULT_VOLUME,
    val currentTrackId: String? = null,
    val currentTrackTitle: String = "",
    val tracks: List<AmbientTrack> = emptyList(),
    val canPlay: Boolean = false,
)
