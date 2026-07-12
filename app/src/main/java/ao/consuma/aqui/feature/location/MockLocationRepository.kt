package ao.consuma.aqui.feature.location

import ao.consuma.aqui.feature.launch.domain.MockLocation
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositório de localizações simuladas.
 *
 * Não utiliza GPS, geocoding ou backend. A lista é fixa e limitada,
 * servindo apenas para validar o fluxo de escolha de localização.
 */
@Singleton
class MockLocationRepository @Inject constructor() {

    private val locations: List<MockLocation> = listOf(
        MockLocation(
            id = "maianga",
            city = "Luanda",
            area = "Maianga",
            displayName = "Luanda — Maianga"
        ),
        MockLocation(
            id = "talatona",
            city = "Luanda",
            area = "Talatona",
            displayName = "Luanda — Talatona"
        ),
        MockLocation(
            id = "kilamba",
            city = "Luanda",
            area = "Kilamba",
            displayName = "Luanda — Kilamba"
        ),
        MockLocation(
            id = "viana",
            city = "Luanda",
            area = "Viana",
            displayName = "Luanda — Viana"
        ),
        MockLocation(
            id = "mutamba",
            city = "Luanda",
            area = "Mutamba",
            displayName = "Luanda — Mutamba"
        )
    )

    fun getAll(): List<MockLocation> = locations

    fun getDefault(): MockLocation = locations.first()

    fun findById(id: String): MockLocation? = locations.find { it.id == id }

    fun search(query: String): List<MockLocation> {
        if (query.isBlank()) return locations
        val normalizedQuery = query.trim().lowercase()
        return locations.filter {
            it.city.lowercase().contains(normalizedQuery) ||
                it.area.lowercase().contains(normalizedQuery) ||
                it.displayName.lowercase().contains(normalizedQuery)
        }
    }
}
