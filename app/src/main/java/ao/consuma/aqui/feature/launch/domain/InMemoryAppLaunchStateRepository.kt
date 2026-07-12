package ao.consuma.aqui.feature.launch.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementação temporária em memória do estado de lançamento.
 *
 * RESTRIÇÃO: o estado sobrevive apenas ao ciclo de vida do processo.
 * Será substituído por persistência definitiva numa fase posterior.
 */
@Singleton
class InMemoryAppLaunchStateRepository @Inject constructor() : AppLaunchStateRepository {

    private var onboardingCompleted: Boolean = false
    private val _locationPreference = MutableStateFlow<LocationPreference>(LocationPreference.NotConfigured)

    override val locationPreference: StateFlow<LocationPreference> = _locationPreference.asStateFlow()

    override fun hasCompletedOnboarding(): Boolean = onboardingCompleted

    override fun currentLocation(): MockLocation? {
        val preference = _locationPreference.value
        return if (preference is LocationPreference.Selected) preference.location else null
    }

    override fun completeOnboarding() {
        onboardingCompleted = true
    }

    override fun selectLocation(location: MockLocation) {
        _locationPreference.value = LocationPreference.Selected(location)
    }

    override fun skipLocation() {
        _locationPreference.value = LocationPreference.Skipped
    }

    override fun clearLocation() {
        _locationPreference.value = LocationPreference.NotConfigured
    }

    /**
     * Reverte o estado para o inicial.
     * Destinado a testes instrumentados que precisam de isolamento.
     */
    fun reset() {
        onboardingCompleted = false
        _locationPreference.value = LocationPreference.NotConfigured
    }
}
