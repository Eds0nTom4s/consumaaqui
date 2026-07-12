package ao.consuma.aqui.feature.launch

import ao.consuma.aqui.feature.launch.domain.InMemoryAppLaunchStateRepository
import ao.consuma.aqui.feature.launch.domain.LocationPreference
import ao.consuma.aqui.feature.launch.domain.MockLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryAppLaunchStateRepositoryTest {

    private val repository = InMemoryAppLaunchStateRepository()
    private val testLocation = MockLocation(
        id = "maianga",
        city = "Luanda",
        area = "Maianga",
        displayName = "Luanda — Maianga"
    )

    @Test
    fun `initial state has not completed onboarding and no location`() {
        assertFalse(repository.hasCompletedOnboarding())
        assertNull(repository.currentLocation())
        assertEquals(LocationPreference.NotConfigured, repository.locationPreference.value)
    }

    @Test
    fun `completeOnboarding marks onboarding as completed`() {
        repository.completeOnboarding()
        assertTrue(repository.hasCompletedOnboarding())
    }

    @Test
    fun `selectLocation updates preference and exposes location`() {
        repository.selectLocation(testLocation)

        assertEquals(testLocation, repository.currentLocation())
        assertEquals(LocationPreference.Selected(testLocation), repository.locationPreference.value)
    }

    @Test
    fun `skipLocation marks location as skipped`() {
        repository.skipLocation()

        assertNull(repository.currentLocation())
        assertEquals(LocationPreference.Skipped, repository.locationPreference.value)
    }

    @Test
    fun `clearLocation resets preference to not configured`() {
        repository.selectLocation(testLocation)
        repository.clearLocation()

        assertNull(repository.currentLocation())
        assertEquals(LocationPreference.NotConfigured, repository.locationPreference.value)
    }
}
