package ao.consuma.aqui.feature.placeholder

import ao.consuma.aqui.core.environment.AppEnvironmentProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class FoundationReadyViewModelTest {

    private class FakeEnvironmentProvider : AppEnvironmentProvider {
        override val currentEnvironment = "TEST_ENV"
        override val versionName = "1.0-test"
    }

    @Test
    fun `viewModel exposes environment and version correctly from provider`() {
        val fakeProvider = FakeEnvironmentProvider()
        val viewModel = FoundationReadyViewModel(fakeProvider)
        
        assertEquals("TEST_ENV", viewModel.environment)
        assertEquals("1.0-test", viewModel.version)
    }
}
