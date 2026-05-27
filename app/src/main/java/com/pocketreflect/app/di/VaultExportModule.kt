// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.di

import com.pocketreflect.app.data.export.vault.DefaultVaultExportRepository
import com.pocketreflect.app.data.export.vault.VaultExportRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class VaultExportModule {

    @Binds
    @Singleton
    abstract fun bindVaultExportRepository(impl: DefaultVaultExportRepository): VaultExportRepository
}
