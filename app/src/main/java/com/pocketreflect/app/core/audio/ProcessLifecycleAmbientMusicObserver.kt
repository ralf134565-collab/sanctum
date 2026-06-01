// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.core.audio

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Воспроизводит ambient-музыку, пока процесс приложения на переднем плане.
 */
@Singleton
class ProcessLifecycleAmbientMusicObserver @Inject constructor(
    private val ambientMusicController: AmbientMusicController,
) : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        ambientMusicController.onAppForeground()
    }

    override fun onStop(owner: LifecycleOwner) {
        ambientMusicController.onAppBackground()
    }
}
