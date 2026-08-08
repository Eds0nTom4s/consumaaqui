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

    @Test fun `home consumes canonical response and sends no identity override headers`() = runBlocking {
        server.enqueue(jsonResponse(HOME_JSON))
        val result = repository().home(HomeDiscoveryRequest(null)) as DiscoveryResult.Empty
        assertTrue(result.data!!.recommended.items.isEmpty())
        val request = server.takeRequest()
        assertEquals("/api/v1/discovery/home?page=0&pageSize=20&sort=NAME", request.path)
        assertNull(request.getHeader("Authorization"))
        assertNull(request.getHeader("X-Tenant-Id"))
        assertNull(request.getHeader("X-Tenant-Code"))
        assertNull(request.getHeader("X-Business-Id"))
    }

    @Test fun `home query delegates to canonical search without fake fallback`() = runBlocking {
        server.enqueue(jsonResponse(SEARCH_JSON))
        val result = repository().home(HomeDiscoveryRequest(null, query = "café completo")) as DiscoveryResult.Success
        assertEquals(UUID, result.data.recommended.items.single().id)
        assertEquals("café completo", server.takeRequest().requestUrl!!.queryParameter("query"))
    }

    @Test fun `search sends canonical municipality pagination and NAME sort`() = runBlocking {
        server.enqueue(jsonResponse(SEARCH_JSON))
        val result = repository().search(
            DiscoverySearchRequest(
                query = "café",
                orderBy = DiscoveryOrderBy.NAME,
                page = 2,
                pageSize = 10,
                location = ao.consuma.aqui.feature.discovery.domain.model.DiscoveryLocation("ignored-id", "Luanda", "", "Luanda")
            )
        ) as DiscoveryResult.Success
        val url = server.takeRequest().requestUrl!!
        assertEquals("1", url.queryParameter("page"))
        assertEquals("Luanda", url.queryParameter("municipality"))
        assertNull(url.queryParameter("municipalityId"))
        assertEquals(2, result.data.page)
    }

    @Test fun `merchant uses plural UUID path and canonical detail`() = runBlocking {
        server.enqueue(jsonResponse(MERCHANT_JSON))
        val result = repository().merchant(MerchantRequest(UUID)) as DiscoveryResult.Success
        assertTrue(result.data.catalogAvailable)
        assertEquals(MerchantAvailability.Unknown, result.data.availability)
        assertEquals("/api/v1/discovery/merchants/$UUID", server.takeRequest().path)
    }

    @Test fun `invalid public merchant identity never reaches network`() = runBlocking {
        assertEquals(DiscoveryError.InvalidRequest, (repository().merchant(MerchantRequest("42")) as DiscoveryResult.Error).reason)
        assertEquals(DiscoveryError.InvalidRequest, (repository().merchant(MerchantRequest("merchant-slug")) as DiscoveryResult.Error).reason)
        assertEquals(0, server.requestCount)
    }

    @Test fun `unsupported remote capabilities never fall back or reach network`() = runBlocking {
        assertEquals(DiscoveryError.UnsupportedSort, (repository().search(DiscoverySearchRequest()) as DiscoveryResult.Error).reason)
        assertEquals(DiscoveryError.UnsupportedCapability, (repository().search(DiscoverySearchRequest(orderBy = DiscoveryOrderBy.NAME, categoryId = UUID)) as DiscoveryResult.Error).reason)
        assertEquals(DiscoveryError.UnsupportedCapability, (repository().search(DiscoverySearchRequest(orderBy = DiscoveryOrderBy.NAME, onlyOpen = true)) as DiscoveryResult.Error).reason)
        assertEquals(DiscoveryError.UnsupportedCapability, (repository().search(DiscoverySearchRequest(orderBy = DiscoveryOrderBy.NAME, fulfillmentOptions = setOf(FulfillmentOption.PICKUP))) as DiscoveryResult.Error).reason)
        assertEquals(0, server.requestCount)
    }

    @Test fun `canonical nested error envelope maps by code`() = runBlocking {
        val scenarios = listOf(
            400 to ("INVALID_REQUEST" to DiscoveryError.InvalidRequest),
            400 to ("SORT_NOT_SUPPORTED" to DiscoveryError.UnsupportedSort),
            400 to ("CAPABILITY_NOT_SUPPORTED" to DiscoveryError.UnsupportedCapability),
            404 to ("MERCHANT_NOT_FOUND" to DiscoveryError.NotFound),
            429 to ("RATE_LIMITED" to DiscoveryError.RateLimited),
            503 to ("SERVICE_UNAVAILABLE" to DiscoveryError.ServiceUnavailable),
            500 to ("INTERNAL_ERROR" to DiscoveryError.Server)
        )
        scenarios.forEach { (status, expected) ->
            server.enqueue(errorResponse(status, expected.first))
            val result = repository().merchant(MerchantRequest(UUID)) as DiscoveryResult.Error
            assertEquals(expected.second, result.reason)
        }

        server.enqueue(jsonResponse(
            """{"error":{"code":"INVALID_REQUEST","message":"invalid","retryable":false,"fieldErrors":[{"field":"/onlyOpen","code":"CAPABILITY_NOT_SUPPORTED","message":"unsupported"}],"traceId":"trace-1"}}""",
            400
        ))
        val nestedCapability = repository().merchant(MerchantRequest(UUID)) as DiscoveryResult.Error
        assertEquals(DiscoveryError.UnsupportedCapability, nestedCapability.reason)
    }

    @Test fun `unknown fields are ignored malformed JSON and unbacked 304 are contract errors`() = runBlocking {
        server.enqueue(jsonResponse(SEARCH_JSON.replace("\"hasMore\":false", "\"hasMore\":false,\"future\":{\"x\":1}")))
        assertTrue(repository().search(DiscoverySearchRequest(orderBy = DiscoveryOrderBy.NAME)) is DiscoveryResult.Success)
        server.enqueue(jsonResponse("""{"categories":[],"merchants":"wrong","page":0,"pageSize":20,"totalCount":0,"hasMore":false}"""))
        assertEquals(DiscoveryError.ContractError, (repository().search(DiscoverySearchRequest(orderBy = DiscoveryOrderBy.NAME)) as DiscoveryResult.Error).reason)
        server.enqueue(MockResponse().setResponseCode(304))
        assertEquals(DiscoveryError.ContractError, (repository().merchant(MerchantRequest(UUID)) as DiscoveryResult.Error).reason)
    }

    @Test fun `etag is replayed and backed 304 reuses the canonical in-memory representation`() = runBlocking {
        val repository = repository()
        server.enqueue(jsonResponse(MERCHANT_JSON))
        val first = repository.merchant(MerchantRequest(UUID)) as DiscoveryResult.Success
        assertEquals(UUID, first.data.id)
        assertNull(server.takeRequest().getHeader("If-None-Match"))

        server.enqueue(MockResponse().setResponseCode(304))
        val second = repository.merchant(MerchantRequest(UUID)) as DiscoveryResult.Success
        assertEquals(first.data, second.data)
        assertEquals("\"canonical\"", server.takeRequest().getHeader("If-None-Match"))
    }

    @Test fun `timeout network configuration and cancellation stay distinct`() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val timeoutClient = OkHttpClient.Builder().readTimeout(100, TimeUnit.MILLISECONDS).callTimeout(150, TimeUnit.MILLISECONDS).build()
        assertEquals(DiscoveryError.Timeout, (repository(timeoutClient).merchant(MerchantRequest(UUID)) as DiscoveryResult.Error).reason)
        val network = RemoteDiscoveryRepository(DiscoveryApiProvider { throw IOException("offline") }, DiscoveryDtoMapper(), json)
        assertEquals(DiscoveryError.NetworkUnavailable, (network.merchant(MerchantRequest(UUID)) as DiscoveryResult.Error).reason)
        val configuration = RemoteDiscoveryRepository(DiscoveryApiProvider { throw DiscoveryConfigurationException("missing") }, DiscoveryDtoMapper(), json)
        assertEquals(DiscoveryError.ContractError, (configuration.merchant(MerchantRequest(UUID)) as DiscoveryResult.Error).reason)
        val cancellation = RemoteDiscoveryRepository(DiscoveryApiProvider { throw CancellationException("cancel") }, DiscoveryDtoMapper(), json)
        assertThrows(CancellationException::class.java) { runBlocking { cancellation.merchant(MerchantRequest(UUID)) } }
        Unit
    }

    private fun repository(client: OkHttpClient = DiscoveryNetworkFactory.client()): RemoteDiscoveryRepository {
        val api = Retrofit.Builder().baseUrl(server.url("/")).client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build()
            .create(DiscoveryApi::class.java)
        return RemoteDiscoveryRepository(DiscoveryApiProvider { api }, DiscoveryDtoMapper(), json)
    }

    private fun jsonResponse(body: String, code: Int = 200) = MockResponse().setResponseCode(code)
        .setHeader("Content-Type", "application/json; charset=UTF-8")
        .setHeader("Cache-Control", "public,max-age=60").setHeader("ETag", "\"canonical\"").setBody(body)

    private fun errorResponse(status: Int, code: String) = jsonResponse(
        """{"error":{"code":"$code","message":"public","retryable":false,"fieldErrors":[],"traceId":"trace-1"}}""",
        status
    )

    private companion object {
        const val UUID = "123e4567-e89b-42d3-a456-426614174000"
        const val SUMMARY = """{"merchantId":"$UUID","name":"Café Órbita","availability":"UNKNOWN","fulfillmentOptions":[],"distanceMeters":null,"rating":null,"popularityScore":null,"featured":false,"catalogAvailable":true}"""
        const val HOME_JSON = """{"categories":[],"nearby":{"items":[],"hasMore":false},"recommended":{"items":[],"hasMore":false},"featured":{"items":[],"hasMore":false}}"""
        const val SEARCH_JSON = """{"categories":[],"merchants":[$SUMMARY],"page":1,"pageSize":10,"totalCount":1,"hasMore":false}"""
        const val MERCHANT_JSON = """{"merchantId":"$UUID","name":"Café Órbita","availability":"UNKNOWN","fulfillmentOptions":[],"distanceMeters":null,"rating":null,"popularityScore":null,"featured":false,"catalogAvailable":true,"fullDescription":null,"weeklySchedule":null,"catalogId":null}"""
    }
}
