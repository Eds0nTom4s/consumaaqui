package ao.consuma.aqui.feature.discovery.remote

import ao.consuma.aqui.feature.discovery.domain.model.HomeDiscoveryContent
import ao.consuma.aqui.feature.discovery.domain.model.MerchantSection
import ao.consuma.aqui.feature.discovery.domain.model.MerchantOverview
import ao.consuma.aqui.feature.discovery.domain.model.MerchantSearchContent
import ao.consuma.aqui.feature.discovery.domain.model.DiscoveryOrderBy
import ao.consuma.aqui.feature.discovery.domain.repository.DiscoveryRepository
import ao.consuma.aqui.feature.discovery.domain.request.DiscoveryPaging
import ao.consuma.aqui.feature.discovery.domain.request.DiscoverySearchRequest
import ao.consuma.aqui.feature.discovery.domain.request.HomeDiscoveryRequest
import ao.consuma.aqui.feature.discovery.domain.request.MerchantRequest
import ao.consuma.aqui.feature.discovery.domain.result.DataSource
import ao.consuma.aqui.feature.discovery.domain.result.DiscoveryError
import ao.consuma.aqui.feature.discovery.domain.result.DiscoveryResult
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.DateTimeException
import java.util.UUID
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.Response

@Singleton
class RemoteDiscoveryRepository @Inject constructor(
    private val apiProvider: DiscoveryApiProvider,
    private val mapper: DiscoveryDtoMapper,
    private val json: Json
) : DiscoveryRepository {
    override suspend fun home(request: HomeDiscoveryRequest): DiscoveryResult<HomeDiscoveryContent> {
        if (request.selectedCategoryId != null) {
            return DiscoveryResult.Error(DiscoveryError.UnsupportedCapability)
        }
        val municipality = request.location?.city?.takeIf(String::isNotBlank)
        return if (!request.query.isNullOrBlank()) {
            execute(
                call = { apiProvider.get().search(request.query, request.selectedCategoryId, municipality, 0, DiscoveryPaging.DEFAULT_PAGE_SIZE, "NAME") },
                map = { dto ->
                    val search = mapper.search(dto)
                    HomeDiscoveryContent(
                        categories = search.categories,
                        nearby = MerchantSection(emptyList(), false),
                        recommended = MerchantSection(search.merchants, search.hasMore),
                        featured = MerchantSection(emptyList(), false)
                    )
                },
                empty = { it.nearby.items.isEmpty() && it.recommended.items.isEmpty() && it.featured.items.isEmpty() }
            )
        } else {
            execute(
                call = { apiProvider.get().home(municipality, request.selectedCategoryId, 0, DiscoveryPaging.DEFAULT_PAGE_SIZE, "NAME") },
                map = mapper::home,
                empty = { it.nearby.items.isEmpty() && it.recommended.items.isEmpty() && it.featured.items.isEmpty() }
            )
        }
    }

    override suspend fun search(request: DiscoverySearchRequest): DiscoveryResult<MerchantSearchContent> {
        if (request.orderBy != DiscoveryOrderBy.NAME) return DiscoveryResult.Error(DiscoveryError.UnsupportedSort)
        if (request.categoryId != null || request.onlyOpen || request.fulfillmentOptions.isNotEmpty()) {
            return DiscoveryResult.Error(DiscoveryError.UnsupportedCapability)
        }
        if (request.page < 1 || request.pageSize !in 1..DiscoveryPaging.MAX_PAGE_SIZE) {
            return DiscoveryResult.Error(DiscoveryError.InvalidRequest)
        }
        return execute(
            call = {
                apiProvider.get().search(
                    query = request.query,
                    categoryId = request.categoryId,
                    municipality = request.location?.city?.takeIf(String::isNotBlank),
                    page = DiscoveryPagingMapper.toBackend(request.page),
                    pageSize = request.pageSize,
                    sort = "NAME"
                )
            },
            map = mapper::search,
            empty = { it.merchants.isEmpty() }
        )
    }

    override suspend fun merchant(request: MerchantRequest): DiscoveryResult<MerchantOverview> {
        if (!request.merchantId.isCanonicalUuid()) return DiscoveryResult.Error(DiscoveryError.InvalidRequest)
        return execute(
            call = { apiProvider.get().merchant(request.merchantId) },
            map = mapper::overview,
            empty = { false }
        )
    }

    private suspend fun <Dto, Domain> execute(
        call: suspend () -> Response<Dto>,
        map: (Dto) -> Domain,
        empty: (Domain) -> Boolean
    ): DiscoveryResult<Domain> {
        return try {
            val response = call()
            if (response.code() == 304) return DiscoveryResult.Error(DiscoveryError.ContractError)
            if (!response.isSuccessful) return DiscoveryResult.Error(mapHttpError(response))
            val body = response.body() ?: return DiscoveryResult.Error(DiscoveryError.ContractError)
            val domain = map(body)
            if (empty(domain)) DiscoveryResult.Empty(domain)
            else DiscoveryResult.Success(domain, DataSource.REMOTE)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (timeout: SocketTimeoutException) {
            DiscoveryResult.Error(DiscoveryError.Timeout)
        } catch (serialization: SerializationException) {
            DiscoveryResult.Error(DiscoveryError.ContractError)
        } catch (configuration: DiscoveryConfigurationException) {
            DiscoveryResult.Error(DiscoveryError.ContractError)
        } catch (io: IOException) {
            DiscoveryResult.Error(DiscoveryError.NetworkUnavailable)
        } catch (dateTime: DateTimeException) {
            DiscoveryResult.Error(DiscoveryError.ContractError)
        } catch (contract: IllegalArgumentException) {
            DiscoveryResult.Error(DiscoveryError.ContractError)
        }
    }

    private fun mapHttpError(response: Response<*>): DiscoveryError {
        val code = response.errorBody()?.string()?.let { body ->
            runCatching { json.decodeFromString<AndroidPublicErrorEnvelopeDto>(body).error.code }.getOrNull()
        }
        return when (response.code()) {
            400 -> when (code) {
                "SORT_NOT_SUPPORTED" -> DiscoveryError.UnsupportedSort
                "CAPABILITY_NOT_SUPPORTED" -> DiscoveryError.UnsupportedCapability
                else -> DiscoveryError.InvalidRequest
            }
            401 -> DiscoveryError.Unauthorized
            403 -> DiscoveryError.Forbidden
            404 -> DiscoveryError.NotFound
            429 -> DiscoveryError.RateLimited
            503 -> DiscoveryError.ServiceUnavailable
            in 500..599 -> DiscoveryError.Server
            else -> DiscoveryError.Unknown
        }
    }

    private fun String.isCanonicalUuid(): Boolean = runCatching {
        UUID.fromString(this).toString() == lowercase()
    }.getOrDefault(false)
}
