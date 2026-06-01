// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.ambient

import kotlinx.serialization.Serializable

@Serializable
data class StoredCustomAmbientTrack(
    val id: String,
    val displayName: String,
    val fileName: String,
)
