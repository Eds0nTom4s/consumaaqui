package ao.consuma.aqui.core.environment

import org.junit.Assert.assertNotNull
import org.junit.Test

class EnvironmentResolverTest {

    @Test
    fun `environment returns non null value`() {
        val resolver = EnvironmentResolver()
        assertNotNull(resolver.currentEnvironment)
        assertNotNull(resolver.versionName)
    }
}
