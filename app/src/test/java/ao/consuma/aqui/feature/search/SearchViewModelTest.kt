package ao.consuma.aqui.feature.search

import androidx.lifecycle.SavedStateHandle
import ao.consuma.aqui.feature.discovery.data.InMemoryDiscoveryRepository
import ao.consuma.aqui.feature.discovery.data.MockDiscoveryScenario
import ao.consuma.aqui.feature.discovery.domain.model.*
import ao.consuma.aqui.feature.discovery.domain.capability.DiscoverySource
import ao.consuma.aqui.feature.discovery.domain.capability.DiscoverySourcePolicy
import ao.consuma.aqui.feature.discovery.domain.repository.DiscoveryRepository
import ao.consuma.aqui.feature.discovery.domain.request.*
import ao.consuma.aqui.feature.discovery.domain.result.DiscoveryResult
import ao.consuma.aqui.feature.discovery.domain.result.DataSource
import ao.consuma.aqui.feature.discovery.data.DiscoveryFixtures
import ao.consuma.aqui.feature.discovery.presentation.mapper.DiscoveryUiMapper
import ao.consuma.aqui.feature.launch.domain.InMemoryAppLaunchStateRepository
import ao.consuma.aqui.feature.launch.domain.MockLocation
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private lateinit var repository: RecordingRepository
    private lateinit var launchRepository: InMemoryAppLaunchStateRepository
    private lateinit var savedState: SavedStateHandle
    private lateinit var viewModel: SearchViewModel

    @Before fun setUp() {
        repository = RecordingRepository(InMemoryDiscoveryRepository())
        launchRepository = InMemoryAppLaunchStateRepository()
        savedState = SavedStateHandle()
        viewModel = SearchViewModel(repository, launchRepository, DiscoveryUiMapper(), savedState, UnconfinedTestDispatcher())
    }

    @Test fun `initial state is loading`() = assertEquals(SearchUiState.Loading, viewModel.uiState.value)
    @Test fun `load returns merchant content`() { viewModel.onEvent(SearchUiEvent.Load); assertTrue(viewModel.uiState.value is SearchUiState.Content) }
    @Test fun `blank query produces exploration mode`() {
        viewModel.onEvent(SearchUiEvent.QueryChanged("   "))
        assertTrue((viewModel.uiState.value as SearchUiState.Content).data.isExplorationMode)
    }
    @Test fun `query filter category fulfillment and order are sent to repository`() {
        viewModel.onEvent(SearchUiEvent.QueryChanged("  café "))
        viewModel.onEvent(SearchUiEvent.OpenNowChanged(true))
        viewModel.onEvent(SearchUiEvent.CategorySelected("drinks"))
        viewModel.onEvent(SearchUiEvent.FulfillmentToggled(FulfillmentOption.DELIVERY))
        viewModel.onEvent(SearchUiEvent.FulfillmentToggled(FulfillmentOption.PICKUP))
        viewModel.onEvent(SearchUiEvent.SortSelected(DiscoveryOrderBy.TOP_RATED))
        assertEquals("  café ", repository.lastSearchRequest.query)
        assertTrue(repository.lastSearchRequest.onlyOpen)
        assertEquals("drinks", repository.lastSearchRequest.categoryId)
        assertEquals(setOf(FulfillmentOption.DELIVERY, FulfillmentOption.PICKUP), repository.lastSearchRequest.fulfillmentOptions)
        assertEquals(DiscoveryOrderBy.TOP_RATED, repository.lastSearchRequest.orderBy)
    }
    @Test fun `criteria are persisted in saved state using centralized keys`() { viewModel.onEvent(SearchUiEvent.QueryChanged("mercado")); assertEquals("mercado", savedState.get<String>(SearchSavedStateKeys.QUERY)) }
    @Test fun `all criteria survive viewmodel recreation`() {
        viewModel.onEvent(SearchUiEvent.QueryChanged("mercado"))
        viewModel.onEvent(SearchUiEvent.CategorySelected("market"))
        viewModel.onEvent(SearchUiEvent.OpenNowChanged(true))
        viewModel.onEvent(SearchUiEvent.FulfillmentToggled(FulfillmentOption.DELIVERY))
        viewModel.onEvent(SearchUiEvent.FulfillmentToggled(FulfillmentOption.PICKUP))
        viewModel.onEvent(SearchUiEvent.SortSelected(DiscoveryOrderBy.NAME))
        val restoredRepository = RecordingRepository(InMemoryDiscoveryRepository())
        val restored = SearchViewModel(restoredRepository, launchRepository, DiscoveryUiMapper(), savedState, UnconfinedTestDispatcher())
        restored.onEvent(SearchUiEvent.Load)
        assertEquals("mercado", restoredRepository.lastSearchRequest.query)
        assertEquals("market", restoredRepository.lastSearchRequest.categoryId)
        assertTrue(restoredRepository.lastSearchRequest.onlyOpen)
        assertEquals(setOf(FulfillmentOption.DELIVERY, FulfillmentOption.PICKUP), restoredRepository.lastSearchRequest.fulfillmentOptions)
        assertEquals(DiscoveryOrderBy.NAME, restoredRepository.lastSearchRequest.orderBy)
    }
    @Test fun `without location defaults to featured and disables nearest`() { viewModel.onEvent(SearchUiEvent.Load); val state = viewModel.uiState.value as SearchUiState.Content; assertEquals(DiscoveryOrderBy.FEATURED, state.data.criteria.orderBy); assertFalse(state.data.criteria.sortOptions.first { it.value == DiscoveryOrderBy.NEAREST }.enabled) }
    @Test fun `remote capabilities expose only name and ignore unsupported filter events`() {
        val remotePolicy = object : DiscoverySourcePolicy {
            override val source = MutableStateFlow(DiscoverySource.REMOTE)
            override val selectable = false
            override fun select(source: DiscoverySource) = false
        }
        val remoteViewModel = SearchViewModel(
            repository,
            launchRepository,
            DiscoveryUiMapper(),
            SavedStateHandle(),
            UnconfinedTestDispatcher(),
            remotePolicy
        )
        remoteViewModel.onEvent(SearchUiEvent.Load)
        remoteViewModel.onEvent(SearchUiEvent.OpenNowChanged(true))
        remoteViewModel.onEvent(SearchUiEvent.FulfillmentToggled(FulfillmentOption.PICKUP))
        val criteria = (remoteViewModel.uiState.value as SearchUiState.Content).data.criteria
        assertEquals(listOf(DiscoveryOrderBy.NAME), criteria.sortOptions.map { it.value })
        assertEquals(DiscoveryOrderBy.NAME, repository.lastSearchRequest.orderBy)
        assertFalse(repository.lastSearchRequest.onlyOpen)
        assertTrue(repository.lastSearchRequest.fulfillmentOptions.isEmpty())
        assertFalse(criteria.supportsOnlyOpen)
        assertFalse(criteria.supportsFulfillmentFilter)
    }
    @Test fun `clear filters preserves query and sort`() { viewModel.onEvent(SearchUiEvent.QueryChanged("café")); viewModel.onEvent(SearchUiEvent.OpenNowChanged(true)); viewModel.onEvent(SearchUiEvent.SortSelected(DiscoveryOrderBy.NAME)); viewModel.onEvent(SearchUiEvent.ClearFilters); assertEquals("café", repository.lastSearchRequest.query); assertEquals(DiscoveryOrderBy.NAME, repository.lastSearchRequest.orderBy); assertFalse(repository.lastSearchRequest.onlyOpen) }
    @Test fun `fulfillment toggle removes an already selected option and keeps other options`() {
        viewModel.onEvent(SearchUiEvent.FulfillmentToggled(FulfillmentOption.DELIVERY))
        viewModel.onEvent(SearchUiEvent.FulfillmentToggled(FulfillmentOption.PICKUP))
        viewModel.onEvent(SearchUiEvent.FulfillmentToggled(FulfillmentOption.DELIVERY))
        assertEquals(setOf(FulfillmentOption.PICKUP), repository.lastSearchRequest.fulfillmentOptions)
    }
    @Test fun `clear query restores exploration without clearing sort`() {
        viewModel.onEvent(SearchUiEvent.QueryChanged("café"))
        viewModel.onEvent(SearchUiEvent.SortSelected(DiscoveryOrderBy.NAME))
        viewModel.onEvent(SearchUiEvent.ClearQuery)
        assertEquals("", repository.lastSearchRequest.query)
        assertEquals(DiscoveryOrderBy.NAME, repository.lastSearchRequest.orderBy)
        assertTrue((viewModel.uiState.value as SearchUiState.Content).data.isExplorationMode)
    }
    @Test fun `empty offline and error map to dedicated states`() {
        repository.delegate.scenario = MockDiscoveryScenario.EMPTY; viewModel.onEvent(SearchUiEvent.Load); assertTrue(viewModel.uiState.value is SearchUiState.Empty)
        repository.delegate.scenario = MockDiscoveryScenario.OFFLINE; viewModel.onEvent(SearchUiEvent.Retry); assertTrue(viewModel.uiState.value is SearchUiState.OfflineContent)
        repository.delegate.scenario = MockDiscoveryScenario.ERROR; viewModel.onEvent(SearchUiEvent.Retry); assertTrue(viewModel.uiState.value is SearchUiState.Error)
    }
    @Test fun `merchant selection passes only id`() { var id: String? = null; viewModel.onEvent(SearchUiEvent.MerchantSelected("sabor-maianga")) { id = it }; assertEquals("sabor-maianga", id) }
    @Test fun `retry and refresh repeat the complete latest request`() {
        viewModel.onEvent(SearchUiEvent.QueryChanged("café"))
        viewModel.onEvent(SearchUiEvent.CategorySelected("drinks"))
        val expected = repository.lastSearchRequest
        val calls = repository.searchRequests.size
        viewModel.onEvent(SearchUiEvent.Retry)
        viewModel.onEvent(SearchUiEvent.Refresh)
        assertEquals(calls + 2, repository.searchRequests.size)
        assertEquals(expected, repository.lastSearchRequest)
        assertFalse((viewModel.uiState.value as SearchUiState.Content).data.isRefreshing)
    }

    @Test fun `next page is requested centrally and empty end preserves current results`() {
        viewModel.onEvent(SearchUiEvent.Load)
        val first = (viewModel.uiState.value as SearchUiState.Content).data.merchants
        viewModel.onEvent(SearchUiEvent.LoadNextPage)
        assertEquals(2, repository.lastSearchRequest.page)
        val ended = (viewModel.uiState.value as SearchUiState.Content).data
        assertEquals(first, ended.merchants)
        assertFalse(ended.hasMore)
        assertFalse(ended.isLoadingMore)
    }

    @Test fun `selected location enables nearest and enters request`() {
        launchRepository.selectLocation(MockLocation("maianga", "Luanda", "Maianga", "Luanda — Maianga"))
        viewModel.onLocationChanged(launchRepository.locationPreference.value)
        viewModel.onEvent(SearchUiEvent.SortSelected(DiscoveryOrderBy.NEAREST))
        assertEquals("maianga", repository.lastSearchRequest.location?.id)
        assertEquals(DiscoveryOrderBy.NEAREST, repository.lastSearchRequest.orderBy)
        val state = viewModel.uiState.value as SearchUiState.Content
        assertTrue(state.data.criteria.sortOptions.first { it.value == DiscoveryOrderBy.NEAREST }.enabled)
    }

    @Test fun `removing location corrects nearest sort and reloads`() {
        launchRepository.selectLocation(MockLocation("maianga", "Luanda", "Maianga", "Luanda — Maianga"))
        viewModel.onLocationChanged(launchRepository.locationPreference.value)
        viewModel.onEvent(SearchUiEvent.SortSelected(DiscoveryOrderBy.NEAREST))
        launchRepository.clearLocation()
        viewModel.onLocationChanged(launchRepository.locationPreference.value)
        assertNull(repository.lastSearchRequest.location)
        assertEquals(DiscoveryOrderBy.FEATURED, repository.lastSearchRequest.orderBy)
    }

    @Test fun `obsolete request cannot replace the newest query state`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val controlled = OutOfOrderRepository()
        val viewModel = SearchViewModel(
            controlled,
            InMemoryAppLaunchStateRepository(),
            DiscoveryUiMapper(),
            SavedStateHandle(),
            dispatcher
        )
        viewModel.onEvent(SearchUiEvent.QueryChanged("old"))
        runCurrent()
        viewModel.onEvent(SearchUiEvent.QueryChanged("new"))
        advanceUntilIdle()
        val state = viewModel.uiState.value as SearchUiState.Content
        assertEquals("new", state.data.criteria.query)
        assertEquals("new", state.data.merchants.single().id)
    }

    private class RecordingRepository(val delegate: InMemoryDiscoveryRepository) : DiscoveryRepository {
        lateinit var lastSearchRequest: DiscoverySearchRequest
        val searchRequests = mutableListOf<DiscoverySearchRequest>()
        override suspend fun home(request: HomeDiscoveryRequest): DiscoveryResult<HomeDiscoveryContent> = delegate.home(request)
        override suspend fun search(request: DiscoverySearchRequest): DiscoveryResult<MerchantSearchContent> {
            lastSearchRequest = request
            searchRequests += request
            return delegate.search(request)
        }
        override suspend fun merchant(request: MerchantRequest): DiscoveryResult<MerchantOverview> = delegate.merchant(request)
    }

    private class OutOfOrderRepository : DiscoveryRepository {
        override suspend fun home(request: HomeDiscoveryRequest): DiscoveryResult<HomeDiscoveryContent> = error("unused")
        override suspend fun merchant(request: MerchantRequest): DiscoveryResult<MerchantOverview> = error("unused")
        override suspend fun search(request: DiscoverySearchRequest): DiscoveryResult<MerchantSearchContent> {
            delay(if (request.query == "old") 1_000 else 1)
            val merchant = DiscoveryFixtures.merchants.first().copy(id = request.query, name = request.query)
            return DiscoveryResult.Success(
                MerchantSearchContent(DiscoveryFixtures.categories, listOf(merchant), 1, 20, 1, false),
                DataSource.MEMORY
            )
        }
    }
}
