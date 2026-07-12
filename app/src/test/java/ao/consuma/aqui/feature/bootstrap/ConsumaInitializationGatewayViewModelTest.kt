package ao.consuma.aqui.feature.bootstrap

import ao.consuma.aqui.feature.launch.domain.InMemoryAppLaunchStateRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class ConsumaInitializationGatewayViewModelTest {

    @Test
    fun `when onboarding is not completed result is Onboarding`() {
        val repository = InMemoryAppLaunchStateRepository()
        val viewModel = ConsumaInitializationGatewayViewModel(repository)

        assertEquals(InitializationResult.Onboarding, viewModel.result.value)
    }

    @Test
    fun `when onboarding is completed result is AppShell`() {
        val repository = InMemoryAppLaunchStateRepository()
        repository.completeOnboarding()
        val viewModel = ConsumaInitializationGatewayViewModel(repository)

        assertEquals(InitializationResult.AppShell, viewModel.result.value)
    }
}
