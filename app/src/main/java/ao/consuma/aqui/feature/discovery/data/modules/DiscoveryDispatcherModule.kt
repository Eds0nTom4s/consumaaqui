package ao.consuma.aqui.feature.discovery.data.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DiscoveryDispatcher

@Module
@InstallIn(SingletonComponent::class)
object DiscoveryDispatcherModule {
    @Provides
    @DiscoveryDispatcher
    fun provideDiscoveryDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
