package ao.consuma.aqui.feature.discovery.data.modules

import ao.consuma.aqui.feature.discovery.data.SelectableDiscoveryRepository
import ao.consuma.aqui.feature.discovery.domain.repository.DiscoveryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DiscoveryDataModule {
    @Binds
    @Singleton
    abstract fun bindDiscoveryRepository(implementation: SelectableDiscoveryRepository): DiscoveryRepository
}
