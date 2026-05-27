// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.di

import com.pocketreflect.app.data.ai.EngineCoordinator
import com.pocketreflect.app.data.ai.LiteRtGemmaEngine
import com.pocketreflect.app.data.ai.MockGemmaLocalEngine
import com.pocketreflect.app.data.ai.AiEngineStatusProvider
import com.pocketreflect.app.domain.ai.AiEngineStatusSource
import com.pocketreflect.app.domain.ai.GemmaLocalEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

/**
 * DI-модуль для слоя ИИ.
 *
 * После Sub-PR #3b публичный биндинг `GemmaLocalEngine` смотрит на
 * [EngineCoordinator] — все use-site (`@Inject GemmaLocalEngine` в VM и
 * Worker'ах) получают именно его, без знания о том, что внутри живёт
 * real/mock переключатель.
 *
 * Сам coordinator получает два qualifier-биндинга:
 *  - `@Named("real")` → [LiteRtGemmaEngine] (LiteRT-LM 0.11 + Gemma 4).
 *  - `@Named("mock")` → [MockGemmaLocalEngine] (детерминированные ответы
 *    под product-tone; всегда есть как fallback).
 *
 * Этот шаблон — буквальная иллюстрация выгоды интерфейса в Clean
 * Architecture: переключение реального движка стоило ОДНОГО изменения
 * в этом модуле, ни одного use-site трогать не пришлось.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AIModule {

    @Binds
    @Singleton
    abstract fun bindGemmaLocalEngine(impl: EngineCoordinator): GemmaLocalEngine

    @Binds
    @Singleton
    @Named("real")
    abstract fun bindLiteRtEngine(impl: LiteRtGemmaEngine): GemmaLocalEngine

    @Binds
    @Singleton
    @Named("mock")
    abstract fun bindMockEngine(impl: MockGemmaLocalEngine): GemmaLocalEngine

    @Binds
    @Singleton
    abstract fun bindAiEngineStatusSource(impl: AiEngineStatusProvider): AiEngineStatusSource
}
