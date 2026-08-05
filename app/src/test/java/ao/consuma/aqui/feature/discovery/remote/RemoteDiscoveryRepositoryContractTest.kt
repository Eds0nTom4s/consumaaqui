package ao.consuma.aqui.feature.discovery.remote

import ao.consuma.aqui.feature.discovery.domain.model.DiscoveryOrderBy
import ao.consuma.aqui.feature.discovery.domain.model.FulfillmentOption
import ao.consuma.aqui.feature.discovery.domain.model.MerchantAvailability
import ao.consuma.aqui.feature.discovery.domain.request.*
import ao.consuma.aqui.feature.discovery.domain.result.*
import java.io.IOException
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class RemoteDiscoveryRepositoryContractTest {
    private lateinit var server: MockWebServer
    private val json = DiscoveryNetworkFactory.json()

    @Before fun setUp() { server = MockWebServer().also { it.start() } }
    @After fun tearDown() { runCatching { server.shutdown() } }

    @Test fun `home success maps sections optional fields unknown values and catalog availability`() = runBlocking {
        server.enqueue(jsonResponse(HOME_JSON))
        val result = repository().home(HomeDiscoveryRequest(null)) as DiscoveryResult.Success
        val merchant = result.data.recommended.items.single()
        assertEquals(DataSource.REMOTE, result.source)
        assertEquals(MerchantAvailability.Unknown, merchant.availability)
        assertEquals(setOf(FulfillmentOption.PICKUP), merchant.fulfillmentOptions)
        assertTrue(merchant.catalogAvailable)
        assertEquals("/api/v1/discovery/home?&page=0&pageSize=20&sort=NAME".replace("?&", "?"), server.takeRequest().path)
    }

    @Test fun `home query uses search endpoint rather than unsupported home parameter`() = runBlocking {
        server.enqueue(jsonResponse(SEARCH_JSON))
        val result = repository().home(HomeDiscoveryRequest(null, query = "café completo")) as DiscoveryResult.Success
        assertEquals("cafe-orbita", result.data.recommended.items.single().id)
        val request = server.takeRequest()
        assertTrue(request.path!!.startsWith("/api/v1/discovery/search?"))
        assertEquals("café completo", request.requestUrl!!.queryParameter("query"))
    }

    @Test fun `search sends exact supported query pagination and name sort`() = runBlocking {
        server.enqueue(jsonResponse(SEARCH_JSON))
        val result = repository().search(DiscoverySearchRequest(
            query = "café",
            categoryId = "cafe",
            orderBy = DiscoveryOrderBy.NAME,
            page = 2,
            pageSize = 10
        )) as DiscoveryResult.Success
        assertEquals(1, server.takeRequest().requestUrl!!.queryParameter("page")!!.toInt())
        assertEquals(2, result.data.page)
        assertTrue(result.data.hasMore)
    }

    @Test fun `merchant success preserves catalogAvailable and optional omissions`() = runBlocking {
        server.enqueue(jsonResponse(MERCHANT_JSON))
        val result = repository().merchant(MerchantRequest("cafe-orbita")) as DiscoveryResult.Success
        assertTrue(result.data.catalogAvailable)
        assertNull(result.data.contact)
        assertEquals("/api/v1/discovery/merchant/cafe-orbita", server.takeRequest().path)
    }

    @Test fun `empty search is explicit empty with arrays`() = runBlocking {
        server.enqueue(jsonResponse(EMPTY_SEARCH_JSON))
        val result = repository().search(DiscoverySearchRequest(orderBy = DiscoveryOrderBy.NAME))
        assertTrue(result is DiscoveryResult.Empty)
        assertTrue((result as DiscoveryResult.Empty).data!!.merchants.isEmpty())
    }

    @Test fun `unsupported sorts and filters never reach the network`() = runBlocking {
        assertEquals(DiscoveryError.UnsupportedSort, (repository().search(DiscoverySearchRequest()) as DiscoveryResult.Error).reason)
        assertEquals(DiscoveryError.InvalidRequest, (repository().search(DiscoverySearchRequest(orderBy = DiscoveryOrderBy.NAME, onlyOpen = true)) as DiscoveryResult.Error).reason)
        assertEquals(DiscoveryError.InvalidRequest, (repository().search(DiscoverySearchRequest(orderBy = DiscoveryOrderBy.NAME, fulfillmentOptions = setOf(FulfillmentOption.PICKUP))) as DiscoveryResult.Error).reason)
        assertEquals(0, server.requestCount)
    }

    @Test fun `backend error contract maps all required statuses`() = runBlocking {
        val scenarios = listOf(
            400 to ("INVALID_REQUEST" to DiscoveryError.InvalidRequest),
            400 to ("SORT_NOT_SUPPORTED" to DiscoveryError.UnsupportedSort),
            401 to ("UNKNOWN" to DiscoveryError.Unauthorized),
            403 to ("UNKNOWN" to DiscoveryError.Forbidden),
            404 to ("NOT_FOUND" to DiscoveryError.NotFound),
            429 to ("UNKNOWN" to DiscoveryError.RateLimited),
            503 to ("SERVICE_UNAVAILABLE" to DiscoveryError.ServiceUnavailable),
            500 to ("UNKNOWN" to DiscoveryError.Server)
        )
        scenarios.forEach { (status, expected) ->
            server.enqueue(jsonResponse("""{"code":"${expected.first}"}""", status))
            val result = repository().merchant(MerchantRequest("missing")) as DiscoveryResult.Error
            assertEquals(expected.second, result.reason)
        }
    }

    @Test fun `unknown JSON fields are ignored but incompatible JSON is contract error`() = runBlocking {
        server.enqueue(jsonResponse(SEARCH_JSON.replace("\"hasMore\":true", "\"hasMore\":true,\"future\":{\"x\":1}")))
        assertTrue(repository().search(DiscoverySearchRequest(orderBy = DiscoveryOrderBy.NAME)) is DiscoveryResult.Success)
        server.enqueue(jsonResponse("""{"categories":[],"merchants":"wrong","page":0,"pageSize":20,"totalCount":0,"hasMore":false}"""))
        assertEquals(DiscoveryError.ContractError, (repository().search(DiscoverySearchRequest(orderBy = DiscoveryOrderBy.NAME)) as DiscoveryResult.Error).reason)
        server.enqueue(jsonResponse(MERCHANT_JSON.replace(
            "\"catalogAvailable\":true",
            "\"schedule\":{\"openDays\":[\"MONDAY\"],\"opensAt\":\"invalid\",\"closesAt\":\"18:00:00\"},\"catalogAvailable\":true"
        )))
        assertEquals(DiscoveryError.ContractError, (repository().merchant(MerchantRequest("cafe-orbita")) as DiscoveryResult.Error).reason)
    }

    @Test fun `timeout connectivity configuration and cancellation stay distinct`() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val timeoutClient = OkHttpClient.Builder().readTimeout(100, TimeUnit.MILLISECONDS).callTimeout(150, TimeUnit.MILLISECONDS).build()
        assertEquals(DiscoveryError.Timeout, (repository(timeoutClient).merchant(MerchantRequest("slow")) as DiscoveryResult.Error).reason)

        val networkRepository = RemoteDiscoveryRepository(DiscoveryApiProvider { throw IOException("offline") }, DiscoveryDtoMapper(), json)
        assertEquals(DiscoveryError.NetworkUnavailable, (networkRepository.merchant(MerchantRequest("offline")) as DiscoveryResult.Error).reason)

        val configurationRepository = RemoteDiscoveryRepository(DiscoveryApiProvider { throw DiscoveryConfigurationException("missing") }, DiscoveryDtoMapper(), json)
        assertEquals(DiscoveryError.ContractError, (configurationRepository.merchant(MerchantRequest("config")) as DiscoveryResult.Error).reason)

        val cancellationRepository = RemoteDiscoveryRepository(DiscoveryApiProvider { throw CancellationException("cancel") }, DiscoveryDtoMapper(), json)
        assertThrows(CancellationException::class.java) { runBlocking { cancellationRepository.merchant(MerchantRequest("cancel")) } }
        Unit
    }

    private fun repository(client: OkHttpClient = DiscoveryNetworkFactory.client()): RemoteDiscoveryRepository {
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(DiscoveryApi::class.java)
        return RemoteDiscoveryRepository(DiscoveryApiProvider { api }, DiscoveryDtoMapper(), json)
    }

    private fun jsonResponse(body: String, code: Int = 200) = MockResponse()
        .setResponseCode(code)
        .setHeader("Content-Type", "application/json; charset=UTF-8")
        .setHeader("Cache-Control", "no-store")
        .setBody(body)

    companion object {
        private const val SUMMARY = """{"id":"cafe-orbita","name":"Café Órbita","category":{"id":"cafe","name":"Cafés"},"availability":{"status":"FUTURE_STATUS"},"fulfillmentOptions":["PICKUP","FUTURE_MODE"],"catalogAvailable":true}"""
        private const val HOME_JSON = """{"categories":[{"id":"cafe","name":"Cafés"}],"nearby":{"items":[],"hasMore":false},"recommended":{"items":[$SUMMARY],"hasMore":false},"featured":{"items":[],"hasMore":false}}"""
        private const val SEARCH_JSON = """{"categories":[{"id":"cafe","name":"Cafés"}],"merchants":[$SUMMARY],"page":1,"pageSize":10,"totalCount":11,"hasMore":true}"""
        private const val EMPTY_SEARCH_JSON = """{"categories":[],"merchants":[],"page":0,"pageSize":20,"totalCount":0,"hasMore":false}"""
        private const val MERCHANT_JSON = """{"id":"cafe-orbita","name":"Café Órbita","category":{"id":"cafe","name":"Cafés"},"availability":{"status":"UNKNOWN"},"fulfillmentOptions":[],"catalogAvailable":true}"""
    }
}
