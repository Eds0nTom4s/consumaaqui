package ao.consuma.aqui.feature.placeholder

import ao.consuma.aqui.core.environment.EnvironmentResolver
import org.junit.Assert.assertEquals
import org.junit.Test

class FoundationReadyViewModelTest {

    @Test
    fun `viewModel exposes environment and version correctly`() {
        val mockResolver = object : EnvironmentResolver() {
            // we override logic by simply instantiating normal object or mocking, but here we can just use the actual class if not mocked, 
            // since we don't have mockito yet, we can test it with the real one.
        }
        val viewModel = FoundationReadyViewModel(mockResolver)
        
        assertEquals(mockResolver.currentEnvironment, viewModel.environment)
        assertEquals(mockResolver.versionName, viewModel.version)
    }
}
