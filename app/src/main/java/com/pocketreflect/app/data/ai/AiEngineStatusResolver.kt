// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.ai

import com.pocketreflect.app.domain.ai.AiEngineStatus

internal fun resolveAiEngineStatus(
    hasAttachedModel: Boolean,
    warmupState: WarmupState,
    realEngineReady: Boolean,
): AiEngineStatus {
    if (!hasAttachedModel) return AiEngineStatus.MODEL_OFFLINE
    if (realEngineReady) return AiEngineStatus.REAL_READY
    return when (warmupState) {
        WarmupState.Warming,
        WarmupState.Unknown,
        -> AiEngineStatus.WARMING
        WarmupState.Ready -> AiEngineStatus.REAL_READY
        WarmupState.Failed,
        WarmupState.NoModel,
        -> AiEngineStatus.FALLBACK
    }
}
