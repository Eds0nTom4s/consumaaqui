package ao.consuma.aqui.feature.discovery.remote

import ao.consuma.aqui.BuildConfig
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class DiscoveryConfigurationException(message: String) : IllegalStateException(message)

fun interface DiscoveryApiProvider {
    fun get(): DiscoveryApi
}

@Singleton
class BuildConfigDiscoveryApiProvider @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json
) : DiscoveryApiProvider {
    private val api: DiscoveryApi by lazy {
        val configured = BuildConfig.DISCOVERY_BASE_URL.trim()
        val url = configured.toHttpUrlOrNull()
            ?: throw DiscoveryConfigurationException("Discovery base URL is not configured.")
        if (BuildConfig.ENVIRONMENT == "RELEASE" && !url.isHttps) {
            throw DiscoveryConfigurationException("Release Discovery requires HTTPS.")
        }
        Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(DiscoveryApi::class.java)
    }

    override fun get(): DiscoveryApi = api
}

object DiscoveryNetworkFactory {
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        isLenient = false
    }

    fun client(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()
}
