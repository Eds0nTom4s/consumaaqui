package ao.consuma.aqui.feature.catalog.data.modules

import ao.consuma.aqui.feature.catalog.data.InMemoryCatalogRepository
import ao.consuma.aqui.feature.catalog.domain.repository.CatalogRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import javax.inject.Qualifier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CatalogDispatcher

@Module
@InstallIn(SingletonComponent::class)
abstract class CatalogDataModule {
    @Binds
    @Singleton
    abstract fun bindCatalogRepository(
        implementation: InMemoryCatalogRepository
    ): CatalogRepository

    companion object {
        @Provides
        @CatalogDispatcher
        fun provideCatalogDispatcher(): CoroutineDispatcher = Dispatchers.IO
    }
}
