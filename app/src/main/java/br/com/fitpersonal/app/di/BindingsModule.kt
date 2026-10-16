package br.com.fitpersonal.app.di

import br.com.fitpersonal.app.data.repo.PerfilFonte
import br.com.fitpersonal.app.data.repo.PerfilRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds
    abstract fun perfilFonte(impl: PerfilRepository): PerfilFonte
}
