// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.ambient

object AmbientMusicPolicy {

    const val MAX_CUSTOM_TRACKS = 5
    const val MAX_CUSTOM_TRACK_BYTES = 20 * 1024 * 1024L
    const val MAX_DISPLAY_NAME_LENGTH = 80
    const val VOLUME_MIN = 0f
    const val VOLUME_MAX = 1f
    const val DEFAULT_VOLUME = 0.45f

    fun normalizeVolume(raw: Float): Float = raw.coerceIn(VOLUME_MIN, VOLUME_MAX)

    fun volumeFromPercent(percent: Int): Float =
        normalizeVolume(percent / 100f)

    fun volumeToPercent(volume: Float): Int =
        (normalizeVolume(volume) * 100f).toInt().coerceIn(0, 100)

    fun shouldAutoPlay(
        featureEnabled: Boolean,
        pausedByUser: Boolean,
        hasTracks: Boolean,
    ): Boolean = featureEnabled && !pausedByUser && hasTracks
}
