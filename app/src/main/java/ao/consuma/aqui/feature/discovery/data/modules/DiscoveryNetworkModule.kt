package ao.consuma.aqui.feature.discovery.data.modules

import ao.consuma.aqui.feature.discovery.remote.BuildConfigDiscoveryApiProvider
import ao.consuma.aqui.feature.discovery.remote.DiscoveryApiProvider
import ao.consuma.aqui.feature.discovery.remote.DiscoveryNetworkFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
abstract class DiscoveryNetworkModule {
    @Binds abstract fun bindApiProvider(implementation: BuildConfigDiscoveryApiProvider): DiscoveryApiProvider

    companion object {
        @Provides @Singleton fun provideJson(): Json = DiscoveryNetworkFactory.json()
        @Provides @Singleton fun provideHttpClient(): OkHttpClient = DiscoveryNetworkFactory.client()
    }
}
