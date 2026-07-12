package ao.consuma.aqui.feature.onboarding

import ao.consuma.aqui.feature.launch.domain.InMemoryAppLaunchStateRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingViewModelTest {

    private val repository = InMemoryAppLaunchStateRepository()
    private val viewModel = OnboardingViewModel(repository)

    @Test
    fun `initial state is first page of three`() {
        val initial = viewModel.uiState.value

        assertEquals(0, initial.currentPage)
        assertEquals(3, initial.totalPages)
        assertFalse(initial.isLastPage)
    }

    @Test
    fun `Continue advances to next page`() {
        viewModel.onEvent(OnboardingEvent.Continue)

        assertEquals(1, viewModel.uiState.value.currentPage)
    }

    @Test
    fun `Continue does not exceed last page`() {
        repeat(5) { viewModel.onEvent(OnboardingEvent.Continue) }

        assertEquals(2, viewModel.uiState.value.currentPage)
        assertTrue(viewModel.uiState.value.isLastPage)
    }

    @Test
    fun `Skip marks onboarding as completed`() {
        viewModel.onEvent(OnboardingEvent.Skip)

        assertTrue(repository.hasCompletedOnboarding())
    }

    @Test
    fun `Finish marks onboarding as completed`() {
        viewModel.onEvent(OnboardingEvent.Finish)

        assertTrue(repository.hasCompletedOnboarding())
    }

    @Test
    fun `PageChanged updates current page`() {
        viewModel.onEvent(OnboardingEvent.PageChanged(2))

        assertEquals(2, viewModel.uiState.value.currentPage)
        assertTrue(viewModel.uiState.value.isLastPage)
    }
}
