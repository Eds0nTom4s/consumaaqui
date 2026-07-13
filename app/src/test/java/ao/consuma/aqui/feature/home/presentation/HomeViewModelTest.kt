package ao.consuma.aqui.feature.home.presentation

import ao.consuma.aqui.feature.home.data.HomeDiscoveryFixtures
import ao.consuma.aqui.feature.home.data.InMemoryHomeDiscoveryRepository
import ao.consuma.aqui.feature.home.data.MockHomeScenario
import ao.consuma.aqui.feature.home.domain.repository.HomeDiscoveryRepository
import ao.consuma.aqui.feature.home.domain.repository.HomeDiscoveryRequest
import ao.consuma.aqui.feature.home.domain.repository.HomeDiscoveryResult
import ao.consuma.aqui.feature.launch.domain.InMemoryAppLaunchStateRepository
import ao.consuma.aqui.feature.launch.domain.MockLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HomeViewModelTest {
    private lateinit var repository: RecordingRepository
    private lateinit var launchRepository: InMemoryAppLaunchStateRepository
    private lateinit var viewModel: HomeViewModel

    @Before fun setUp() {
        repository = RecordingRepository(InMemoryHomeDiscoveryRepository())
        launchRepository = InMemoryAppLaunchStateRepository()
        viewModel = HomeViewModel(repository, launchRepository, HomeUiMapper())
    }

    @Test fun `initial state is loading`() = assertEquals(HomeUiState.Loading, viewModel.uiState.value)
    @Test fun `success produces content`() { load(); assertTrue(viewModel.uiState.value is HomeUiState.Content) }
    @Test fun `absence of location never invents proximity`() { load(); val state = viewModel.uiState.value as HomeUiState.Content; assertNull(state.location); assertTrue(state.nearbyMerchants.all { it.distanceText == null }) }
    @Test fun `selected location is sent to repository`() { selectLocation(); load(); assertEquals("maianga", repository.lastRequest.location?.id) }
    @Test fun `category updates request`() { load(); viewModel.onEvent(HomeUiEvent.CategorySelected("bakery")); assertEquals("bakery", repository.lastRequest.selectedCategoryId) }
    @Test fun `query updates request`() { load(); viewModel.onEvent(HomeUiEvent.SearchChanged("café")); assertEquals("café", repository.lastRequest.query) }
    @Test fun `refresh sends force refresh and clears refreshing`() { load(); viewModel.onEvent(HomeUiEvent.Refresh); assertTrue(repository.lastRequest.forceRefresh); assertFalse((viewModel.uiState.value as HomeUiState.Content).isRefreshing) }
    @Test fun `error produces error state`() { repository.delegate.scenario = MockHomeScenario.ERROR; load(); assertTrue(viewModel.uiState.value is HomeUiState.Error) }
    @Test fun `empty produces empty state`() { repository.delegate.scenario = MockHomeScenario.EMPTY; load(); assertTrue(viewModel.uiState.value is HomeUiState.Empty) }
    @Test fun `offline content is marked offline`() { repository.delegate.scenario = MockHomeScenario.OFFLINE; load(); assertTrue((viewModel.uiState.value as HomeUiState.Content).isOffline) }
    @Test fun `retry reloads content`() { repository.delegate.scenario = MockHomeScenario.ERROR; load(); repository.delegate.scenario = MockHomeScenario.CONTENT; viewModel.onEvent(HomeUiEvent.Retry); assertTrue(viewModel.uiState.value is HomeUiState.Content) }
    @Test fun `merchant selection emits callback`() { var selected: String? = null; viewModel.onEvent(HomeUiEvent.MerchantSelected("sabor-maianga"), { selected = it }); assertEquals("sabor-maianga", selected) }
    @Test fun `refresh preserves query and category`() { load(); viewModel.onEvent(HomeUiEvent.SearchChanged("pão")); viewModel.onEvent(HomeUiEvent.CategorySelected("bakery")); viewModel.onEvent(HomeUiEvent.Refresh); assertEquals("pão", repository.lastRequest.query); assertEquals("bakery", repository.lastRequest.selectedCategoryId) }

    private fun load() = viewModel.onLocationChanged(launchRepository.locationPreference.value)
    private fun selectLocation() = launchRepository.selectLocation(MockLocation("maianga", "Luanda", "Maianga", "Luanda — Maianga"))

    private class RecordingRepository(val delegate: InMemoryHomeDiscoveryRepository) : HomeDiscoveryRepository {
        lateinit var lastRequest: HomeDiscoveryRequest
        override suspend fun getHomeContent(request: HomeDiscoveryRequest): HomeDiscoveryResult { lastRequest = request; return delegate.getHomeContent(request) }
    }
}
