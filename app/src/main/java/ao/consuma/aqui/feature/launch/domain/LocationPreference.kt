package ao.consuma.aqui.feature.launch.domain

/**
 * Estado da preferência de localização do utilizador.
 * Evita confundir "nunca configurado", "recusado" e "seleccionado".
 */
sealed interface LocationPreference {
    data object NotConfigured : LocationPreference
    data object Skipped : LocationPreference
    data class Selected(val location: MockLocation) : LocationPreference
}
