package ao.consuma.aqui.feature.launch.domain

/**
 * Localização simulada utilizada durante a fase de mock.
 * Não representa coordenadas GPS reais.
 */
data class MockLocation(
    val id: String,
    val city: String,
    val area: String,
    val displayName: String
)
