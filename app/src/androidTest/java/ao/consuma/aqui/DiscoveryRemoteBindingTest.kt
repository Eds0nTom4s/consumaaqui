package ao.consuma.aqui

import ao.consuma.aqui.feature.discovery.domain.capability.DiscoverySource
import ao.consuma.aqui.feature.discovery.domain.capability.DiscoverySourcePolicy
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DiscoveryRemoteBindingTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)
    @Inject lateinit var sourcePolicy: DiscoverySourcePolicy

    @Before fun inject() { hiltRule.inject() }

    @Test fun debug_binding_requires_explicit_source_selection() {
        assertTrue(sourcePolicy.selectable)
        assertEquals(DiscoverySource.MOCK, sourcePolicy.source.value)
        assertTrue(sourcePolicy.select(DiscoverySource.REMOTE))
        assertEquals(DiscoverySource.REMOTE, sourcePolicy.source.value)
        assertTrue(sourcePolicy.select(DiscoverySource.MOCK))
    }
}
