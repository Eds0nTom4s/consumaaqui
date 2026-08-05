package ao.consuma.aqui.feature.discovery.data.modules

import ao.consuma.aqui.feature.discovery.data.BuildConfigDiscoverySourcePolicy
import ao.consuma.aqui.feature.discovery.domain.capability.DiscoverySourcePolicy
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DiscoverySourceModule {
    @Binds
    @Singleton
    abstract fun bindDiscoverySourcePolicy(
        implementation: BuildConfigDiscoverySourcePolicy
    ): DiscoverySourcePolicy
}
