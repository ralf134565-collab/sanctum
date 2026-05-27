// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.di

import com.pocketreflect.app.core.time.Clock
import com.pocketreflect.app.core.time.SystemClock
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DI-модуль для кросс-слойных утилит (часы, в перспективе — логгер, ID-генератор).
 *
 * Биндинг абстракций сюда, а не в [DatabaseModule]/[AIModule], нужен для того,
 * чтобы зависимости были организованы по их «концептуальной роли»,
 * а не по тому, кто первый их попросил.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CoreModule {

    @Binds
    @Singleton
    abstract fun bindClock(impl: SystemClock): Clock
}
