package ao.consuma.aqui.core.environment

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class EnvironmentModule {
    @Binds
    abstract fun bindAppEnvironmentProvider(
        environmentResolver: EnvironmentResolver
    ): AppEnvironmentProvider
}
