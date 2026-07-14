package ao.consuma.aqui.feature.discovery.presentation.merchant

import androidx.lifecycle.SavedStateHandle
import ao.consuma.aqui.feature.discovery.data.InMemoryDiscoveryRepository
import ao.consuma.aqui.feature.discovery.data.MockDiscoveryScenario
import ao.consuma.aqui.feature.discovery.presentation.mapper.DiscoveryUiMapper
import ao.consuma.aqui.feature.discovery.domain.model.*
import ao.consuma.aqui.feature.discovery.domain.repository.DiscoveryRepository
import ao.consuma.aqui.feature.discovery.domain.request.*
import ao.consuma.aqui.feature.discovery.domain.result.DiscoveryResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class MerchantOverviewViewModelTest {
    @Test fun `initial state is loading`() {
        assertEquals(MerchantOverviewUiState.Loading, viewModel(InMemoryDiscoveryRepository()).uiState.value)
    }

    @Test fun `merchant id from navigation loads overview`() {
        val viewModel = viewModel(InMemoryDiscoveryRepository())
        viewModel.load()
        val state = viewModel.uiState.value as MerchantOverviewUiState.Content
        assertEquals("sabor-maianga", state.merchant.id)
    }

    @Test fun `offline and error have dedicated states`() {
        val repository = InMemoryDiscoveryRepository().apply { scenario = MockDiscoveryScenario.OFFLINE }
        val viewModel = viewModel(repository)
        viewModel.load()
        assertTrue(viewModel.uiState.value is MerchantOverviewUiState.OfflineContent)
        repository.scenario = MockDiscoveryScenario.ERROR
        viewModel.load()
        assertTrue(viewModel.uiState.value is MerchantOverviewUiState.Error)
    }

    @Test fun `missing navigation id maps to not found without crashing`() {
        val viewModel = MerchantOverviewViewModel(InMemoryDiscoveryRepository(), DiscoveryUiMapper(), SavedStateHandle(), UnconfinedTestDispatcher())
        viewModel.load()
        assertTrue(viewModel.uiState.value is MerchantOverviewUiState.NotFound)
    }

    @Test fun `unknown merchant maps explicit not found`() {
        val viewModel = MerchantOverviewViewModel(
            InMemoryDiscoveryRepository(),
            DiscoveryUiMapper(),
            SavedStateHandle(mapOf("merchantId" to "missing")),
            UnconfinedTestDispatcher()
        )
        viewModel.load()
        assertTrue(viewModel.uiState.value is MerchantOverviewUiState.NotFound)
    }

    @Test fun `retry repeats exactly the navigation merchant id`() {
        val repository = RecordingRepository(InMemoryDiscoveryRepository())
        val viewModel = MerchantOverviewViewModel(
            repository,
            DiscoveryUiMapper(),
            SavedStateHandle(mapOf("merchantId" to "sabor-maianga")),
            UnconfinedTestDispatcher()
        )
        viewModel.load()
        viewModel.onEvent(MerchantOverviewUiEvent.Retry)
        assertEquals(listOf("sabor-maianga", "sabor-maianga"), repository.merchantIds)
    }

    @Test fun `catalog action emits only id when catalog is available`() {
        val viewModel = viewModel(InMemoryDiscoveryRepository())
        viewModel.load()
        var selected: String? = null
        viewModel.onEvent(MerchantOverviewUiEvent.OpenCatalog) { selected = it }
        assertEquals("sabor-maianga", selected)
    }

    @Test fun `catalog action is blocked when catalog is unavailable`() {
        val viewModel = MerchantOverviewViewModel(
            InMemoryDiscoveryRepository(),
            DiscoveryUiMapper(),
            SavedStateHandle(mapOf("merchantId" to "servicos-viana")),
            UnconfinedTestDispatcher()
        )
        viewModel.load()
        val state = viewModel.uiState.value as MerchantOverviewUiState.Content
        assertFalse(state.merchant.catalogAvailable)
        var selected: String? = null
        viewModel.onEvent(MerchantOverviewUiEvent.OpenCatalog) { selected = it }
        assertNull(selected)
    }

    private fun viewModel(repository: InMemoryDiscoveryRepository) = MerchantOverviewViewModel(
        repository,
        DiscoveryUiMapper(),
        SavedStateHandle(mapOf("merchantId" to "sabor-maianga")),
        UnconfinedTestDispatcher()
    )

    private class RecordingRepository(private val delegate: InMemoryDiscoveryRepository) : DiscoveryRepository {
        val merchantIds = mutableListOf<String>()
        override suspend fun home(request: HomeDiscoveryRequest): DiscoveryResult<HomeDiscoveryContent> = delegate.home(request)
        override suspend fun search(request: DiscoverySearchRequest): DiscoveryResult<MerchantSearchContent> = delegate.search(request)
        override suspend fun merchant(request: MerchantRequest): DiscoveryResult<MerchantOverview> {
            merchantIds += request.merchantId
            return delegate.merchant(request)
        }
    }
}
