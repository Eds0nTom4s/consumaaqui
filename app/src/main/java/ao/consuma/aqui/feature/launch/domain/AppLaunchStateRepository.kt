package ao.consuma.aqui.feature.launch.domain

import kotlinx.coroutines.flow.StateFlow

/**
 * Abstracção para o estado de lançamento da aplicação.
 * Implementação actual em memória; futuramente substituível por DataStore.
 */
interface AppLaunchStateRepository {
    val locationPreference: StateFlow<LocationPreference>

    fun hasCompletedOnboarding(): Boolean
    fun currentLocation(): MockLocation?
    fun completeOnboarding()
    fun selectLocation(location: MockLocation)
    fun skipLocation()
    fun clearLocation()
}
