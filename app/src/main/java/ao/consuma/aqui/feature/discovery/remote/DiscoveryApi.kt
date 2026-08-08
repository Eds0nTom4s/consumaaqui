package ao.consuma.aqui.feature.discovery.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface DiscoveryApi {
    @GET("api/v1/discovery/home")
    suspend fun home(
        @Query("municipality") municipality: String? = null,
        @Query("categoryId") categoryId: String? = null,
        @Query("page") page: Int = 0,
        @Query("pageSize") pageSize: Int = 20,
        @Query("sort") sort: String = "NAME",
        @Header("If-None-Match") ifNoneMatch: String? = null
    ): Response<DiscoveryHomeDto>

    @GET("api/v1/discovery/search")
    suspend fun search(
        @Query("query") query: String = "",
        @Query("categoryId") categoryId: String? = null,
        @Query("municipality") municipality: String? = null,
        @Query("page") page: Int = 0,
        @Query("pageSize") pageSize: Int = 20,
        @Query("sort") sort: String = "NAME",
        @Header("If-None-Match") ifNoneMatch: String? = null
    ): Response<MerchantSearchDto>

    @GET("api/v1/discovery/merchants/{merchantId}")
    suspend fun merchant(
        @Path("merchantId") merchantId: String,
        @Header("If-None-Match") ifNoneMatch: String? = null
    ): Response<MerchantOverviewDto>
}
