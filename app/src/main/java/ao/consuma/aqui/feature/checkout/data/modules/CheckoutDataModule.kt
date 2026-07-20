package ao.consuma.aqui.feature.checkout.data.modules

import ao.consuma.aqui.feature.checkout.data.InMemoryCheckoutRepository
import ao.consuma.aqui.feature.checkout.domain.repository.CheckoutRepository
import ao.consuma.aqui.feature.checkout.domain.service.CheckoutIdGenerator
import ao.consuma.aqui.feature.checkout.domain.service.UuidCheckoutIdGenerator
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CheckoutDataModule {
    @Binds
    @Singleton
    abstract fun bindCheckoutRepository(implementation: InMemoryCheckoutRepository): CheckoutRepository

    @Binds
    @Singleton
    abstract fun bindCheckoutIdGenerator(implementation: UuidCheckoutIdGenerator): CheckoutIdGenerator

    companion object {
        @Provides
        @Singleton
        fun provideCheckoutClock(): Clock = Clock.systemUTC()
    }
}
