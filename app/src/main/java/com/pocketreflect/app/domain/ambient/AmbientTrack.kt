// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.ambient

import android.net.Uri

/**
 * Локальный трек для фонового плеера (встроенный asset или импорт пользователя).
 */
data class AmbientTrack(
    val id: String,
    val title: String,
    val uri: Uri,
    val source: AmbientTrackSource,
)

enum class AmbientTrackSource {
    BUILTIN_ASSET,
    USER_FILE,
}
