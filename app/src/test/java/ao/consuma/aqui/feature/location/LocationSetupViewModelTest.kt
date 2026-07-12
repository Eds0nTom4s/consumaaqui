package ao.consuma.aqui.feature.location

import ao.consuma.aqui.feature.launch.domain.InMemoryAppLaunchStateRepository
import ao.consuma.aqui.feature.launch.domain.LocationPreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocationSetupViewModelTest {

    private lateinit var repository: InMemoryAppLaunchStateRepository
    private lateinit var mockLocationRepository: MockLocationRepository
    private lateinit var viewModel: LocationSetupViewModel

    @Before
    fun setup() {
        repository = InMemoryAppLaunchStateRepository()
        mockLocationRepository = MockLocationRepository()
        viewModel = LocationSetupViewModel(repository, mockLocationRepository)
    }

    @Test
    fun `initial state is Explanation`() {
        assertTrue(viewModel.uiState.value is LocationSetupUiState.Explanation)
    }

    @Test
    fun `UseCurrentLocation transitions to Detected`() {
        viewModel.onEvent(LocationSetupEvent.UseCurrentLocation) {}

        val state = viewModel.uiState.value as LocationSetupUiState.Detected
        assertEquals(mockLocationRepository.getDefault(), state.location)
    }

    @Test
    fun `ChooseManually shows all locations`() {
        viewModel.onEvent(LocationSetupEvent.ChooseManually) {}

        val state = viewModel.uiState.value as LocationSetupUiState.ManualSelection
        assertEquals(mockLocationRepository.getAll(), state.allLocations)
        assertEquals(mockLocationRepository.getAll(), state.filteredLocations)
        assertNull(state.selectedLocation)
    }

    @Test
    fun `SearchChanged filters by city`() {
        viewModel.onEvent(LocationSetupEvent.ChooseManually) {}
        viewModel.onEvent(LocationSetupEvent.SearchChanged("Luanda")) {}

        val state = viewModel.uiState.value as LocationSetupUiState.ManualSelection
        assertEquals(mockLocationRepository.getAll(), state.filteredLocations)
    }

    @Test
    fun `SearchChanged filters by area`() {
        viewModel.onEvent(LocationSetupEvent.ChooseManually) {}
        viewModel.onEvent(LocationSetupEvent.SearchChanged("Maianga")) {}

        val state = viewModel.uiState.value as LocationSetupUiState.ManualSelection
        assertEquals(1, state.filteredLocations.size)
        assertEquals("maianga", state.filteredLocations.first().id)
    }

    @Test
    fun `SearchChanged with no match returns empty list`() {
        viewModel.onEvent(LocationSetupEvent.ChooseManually) {}
        viewModel.onEvent(LocationSetupEvent.SearchChanged("xyz")) {}

        val state = viewModel.uiState.value as LocationSetupUiState.ManualSelection
        assertTrue(state.filteredLocations.isEmpty())
    }

    @Test
    fun `LocationSelected updates selected location`() {
        viewModel.onEvent(LocationSetupEvent.ChooseManually) {}
        viewModel.onEvent(LocationSetupEvent.LocationSelected("talatona")) {}

        val state = viewModel.uiState.value as LocationSetupUiState.ManualSelection
        assertEquals("talatona", state.selectedLocation?.id)
    }

    @Test
    fun `Confirm persists selected location and triggers completion`() {
        var completed = false
        viewModel.onEvent(LocationSetupEvent.ChooseManually) {}
        viewModel.onEvent(LocationSetupEvent.LocationSelected("kilamba")) {}
        viewModel.onEvent(LocationSetupEvent.Confirm) { completed = true }

        val location = repository.currentLocation()
        assertEquals("kilamba", location?.id)
        assertEquals(
            LocationPreference.Selected(mockLocationRepository.findById("kilamba")!!),
            repository.locationPreference.value
        )
        assertTrue(completed)
    }

    @Test
    fun `Skip persists skipped state and triggers completion`() {
        var completed = false
        viewModel.onEvent(LocationSetupEvent.Skip) { completed = true }

        assertNull(repository.currentLocation())
        assertEquals(LocationPreference.Skipped, repository.locationPreference.value)
        assertTrue(completed)
    }

    @Test
    fun `ChooseAnother returns to manual selection`() {
        viewModel.onEvent(LocationSetupEvent.UseCurrentLocation) {}
        viewModel.onEvent(LocationSetupEvent.ChooseAnother) {}

        assertTrue(viewModel.uiState.value is LocationSetupUiState.ManualSelection)
    }

    @Test
    fun `BackToExplanation returns to explanation state`() {
        viewModel.onEvent(LocationSetupEvent.ChooseManually) {}
        viewModel.onEvent(LocationSetupEvent.BackToExplanation) {}

        assertTrue(viewModel.uiState.value is LocationSetupUiState.Explanation)
    }
}
