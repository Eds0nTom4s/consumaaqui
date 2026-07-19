package ao.consuma.aqui.feature.cart.data.modules

import ao.consuma.aqui.feature.cart.data.InMemoryCartRepository
import ao.consuma.aqui.feature.cart.domain.repository.CartRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CartDataModule {
    @Binds
    @Singleton
    abstract fun bindCartRepository(
        implementation: InMemoryCartRepository
    ): CartRepository
}
