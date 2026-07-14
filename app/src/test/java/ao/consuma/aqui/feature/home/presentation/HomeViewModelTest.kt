package ao.consuma.aqui.feature.home.presentation

import ao.consuma.aqui.feature.discovery.data.InMemoryDiscoveryRepository
import ao.consuma.aqui.feature.discovery.data.MockDiscoveryScenario
import ao.consuma.aqui.feature.discovery.domain.model.*
import ao.consuma.aqui.feature.discovery.domain.repository.DiscoveryRepository
import ao.consuma.aqui.feature.discovery.domain.request.*
import ao.consuma.aqui.feature.discovery.domain.result.DiscoveryResult
import ao.consuma.aqui.feature.launch.domain.InMemoryAppLaunchStateRepository
import ao.consuma.aqui.feature.launch.domain.MockLocation
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private lateinit var repository: RecordingRepository
    private lateinit var launchRepository: InMemoryAppLaunchStateRepository
    private lateinit var viewModel: HomeViewModel

    @Before fun setUp() {
        repository = RecordingRepository(InMemoryDiscoveryRepository())
        launchRepository = InMemoryAppLaunchStateRepository()
        viewModel = HomeViewModel(repository, launchRepository, HomeUiMapper(), UnconfinedTestDispatcher())
    }

    @Test fun `initial state is loading`() = assertEquals(HomeUiState.Loading, viewModel.uiState.value)
    @Test fun `success produces content`() { load(); assertTrue(viewModel.uiState.value is HomeUiState.Content) }
    @Test fun `absence of location uses recommendations instead of proximity`() { load(); val state = viewModel.uiState.value as HomeUiState.Content; assertNull(state.location); assertTrue(state.nearbyMerchants.isEmpty()); assertTrue(state.recommendedMerchants.isNotEmpty()) }
    @Test fun `selected location is sent to repository`() { selectLocation(); load(); assertEquals("maianga", repository.lastHomeRequest.location?.id) }
    @Test fun `category updates repository request`() { load(); viewModel.onEvent(HomeUiEvent.CategorySelected("bakery")); assertEquals("bakery", repository.lastHomeRequest.selectedCategoryId) }
    @Test fun `query updates repository request`() { load(); viewModel.onEvent(HomeUiEvent.SearchChanged("café")); assertEquals("café", repository.lastHomeRequest.query) }
    @Test fun `refresh sends force refresh`() { load(); viewModel.onEvent(HomeUiEvent.Refresh); assertTrue(repository.lastHomeRequest.forceRefresh) }
    @Test fun `refresh preserves home query category and location`() {
        selectLocation(); load()
        viewModel.onEvent(HomeUiEvent.SearchChanged("pão"))
        viewModel.onEvent(HomeUiEvent.CategorySelected("bakery"))
        viewModel.onEvent(HomeUiEvent.Refresh)
        assertEquals("pão", repository.lastHomeRequest.query)
        assertEquals("bakery", repository.lastHomeRequest.selectedCategoryId)
        assertEquals("maianga", repository.lastHomeRequest.location?.id)
        assertTrue(repository.lastHomeRequest.forceRefresh)
    }
    @Test fun `explicit repository scenarios map to presentation states`() {
        repository.delegate.scenario = MockDiscoveryScenario.ERROR; load(); assertTrue(viewModel.uiState.value is HomeUiState.Error)
        repository.delegate.scenario = MockDiscoveryScenario.EMPTY; viewModel.onEvent(HomeUiEvent.Retry); assertTrue(viewModel.uiState.value is HomeUiState.Empty)
        repository.delegate.scenario = MockDiscoveryScenario.OFFLINE; viewModel.onEvent(HomeUiEvent.Retry); assertTrue((viewModel.uiState.value as HomeUiState.Content).isOffline)
    }
    @Test fun `merchant selection passes only id`() { var selected: String? = null; viewModel.onEvent(HomeUiEvent.MerchantSelected("sabor-maianga"), { selected = it }); assertEquals("sabor-maianga", selected) }
    @Test fun `view all emits search callback`() { var called = false; viewModel.onEvent(HomeUiEvent.ViewAll, onViewAll = { called = true }); assertTrue(called) }
    @Test fun `location action emits settings callback`() {
        var called = false
        viewModel.onEvent(HomeUiEvent.LocationSelected, onLocationSelected = { called = true })
        assertTrue(called)
    }
    @Test fun `location change reloads sections and request`() {
        load()
        assertNull(repository.lastHomeRequest.location)
        selectLocation()
        viewModel.onLocationChanged(launchRepository.locationPreference.value)
        assertEquals("maianga", repository.lastHomeRequest.location?.id)
        val state = viewModel.uiState.value as HomeUiState.Content
        assertTrue(state.nearbyMerchants.isNotEmpty())
        assertTrue(state.recommendedMerchants.isEmpty())
    }

    private fun load() = viewModel.onLocationChanged(launchRepository.locationPreference.value)
    private fun selectLocation() = launchRepository.selectLocation(MockLocation("maianga", "Luanda", "Maianga", "Luanda — Maianga"))

    private class RecordingRepository(val delegate: InMemoryDiscoveryRepository) : DiscoveryRepository {
        lateinit var lastHomeRequest: HomeDiscoveryRequest
        override suspend fun home(request: HomeDiscoveryRequest): DiscoveryResult<HomeDiscoveryContent> { lastHomeRequest = request; return delegate.home(request) }
        override suspend fun search(request: DiscoverySearchRequest): DiscoveryResult<MerchantSearchContent> = delegate.search(request)
        override suspend fun merchant(request: MerchantRequest): DiscoveryResult<MerchantOverview> = delegate.merchant(request)
    }
}
