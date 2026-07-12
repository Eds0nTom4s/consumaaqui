package ao.consuma.aqui.feature.launch.di

import ao.consuma.aqui.feature.launch.domain.AppLaunchStateRepository
import ao.consuma.aqui.feature.launch.domain.InMemoryAppLaunchStateRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class LaunchStateModule {

    @Binds
    abstract fun bindAppLaunchStateRepository(
        repository: InMemoryAppLaunchStateRepository
    ): AppLaunchStateRepository
}
