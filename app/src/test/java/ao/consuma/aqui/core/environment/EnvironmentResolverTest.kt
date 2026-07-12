package ao.consuma.aqui.core.environment

import ao.consuma.aqui.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class EnvironmentResolverTest {

    @Test
    fun `resolver returns concrete values from BuildConfig`() {
        val resolver = EnvironmentResolver()
        assertEquals(BuildConfig.ENVIRONMENT, resolver.currentEnvironment)
        assertEquals(BuildConfig.VERSION_NAME, resolver.versionName)
    }
}
