package ao.consuma.aqui.feature.home.data

import ao.consuma.aqui.feature.home.domain.repository.HomeDiscoveryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HomeDiscoveryDataModule {
    @Binds
    @Singleton
    abstract fun bindHomeDiscoveryRepository(
        implementation: InMemoryHomeDiscoveryRepository
    ): HomeDiscoveryRepository
}
