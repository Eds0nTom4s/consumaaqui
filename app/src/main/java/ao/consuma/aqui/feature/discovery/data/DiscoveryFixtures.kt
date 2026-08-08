package ao.consuma.aqui.feature.discovery.data

import ao.consuma.aqui.core.domain.model.MoneyAmount
import ao.consuma.aqui.feature.discovery.domain.model.*
import java.time.DayOfWeek
import java.time.LocalTime

object DiscoveryFixtures {
    private const val HOME_SECTION_SIZE = 4

    val categories = listOf(
        MerchantCategory("restaurant", "Restaurantes"),
        MerchantCategory("bakery", "Pastelaria"),
        MerchantCategory("drinks", "Bebidas"),
        MerchantCategory("market", "Mercado"),
        MerchantCategory("services", "Serviços")
    )

    val merchants = listOf(
        MerchantSummary("sabor-maianga", "Sabor da Maianga", "restaurant", "Sabores angolanos preparados no dia", null, MerchantAvailability.Open, setOf(FulfillmentOption.DELIVERY, FulfillmentOption.PICKUP, FulfillmentOption.DINE_IN), 450, 25, 4.7, 125, 940.0, MoneyAmount(550000, "AOA"), PromotionSummary("almoco-local", "Menu do dia", "Selecção especial ao almoço", "DESTAQUE"), true, true),
        MerchantSummary("doce-embondeiro", "Doce Embondeiro", "bakery", "Bolos, pão e pastelaria artesanal", "https://images.invalid/doce-embondeiro.jpg", MerchantAvailability.ClosingSoon(35), setOf(FulfillmentOption.PICKUP, FulfillmentOption.DELIVERY), 900, 20, 4.5, 82, 760.0, MoneyAmount(250000, "AOA"), null, true, true),
        MerchantSummary("cafe-horizonte", "Café Horizonte", "drinks", "Café, sumos naturais e encontros tranquilos", null, MerchantAvailability.OpensAt(LocalTime.of(7, 30)), setOf(FulfillmentOption.DINE_IN, FulfillmentOption.PICKUP), 1250, 15, 4.3, 44, 530.0, null, null, false, true),
        MerchantSummary("mercado-talatona", "Mercado Talatona", "market", "Mercearia e essenciais para casa", "https://images.invalid/mercado-talatona.jpg", MerchantAvailability.Open, setOf(FulfillmentOption.DELIVERY, FulfillmentOption.PICKUP), 2400, 35, 4.6, 210, 1000.0, MoneyAmount(1000000, "AOA"), PromotionSummary("cabaz-semana", "Cabaz da semana", null, "OPORTUNIDADE"), true, true),
        MerchantSummary("servicos-viana", "Serviços Viana", "services", "Reparações e assistência ao domicílio", null, MerchantAvailability.Unknown, setOf(FulfillmentOption.SERVICE), 5100, null, null, null, 310.0, null, null, false, false),
        MerchantSummary("cantinho-kilamba", "Cantinho do Kilamba", "restaurant", "Refeições familiares e grelhados", null, MerchantAvailability.Closed, setOf(FulfillmentOption.DINE_IN, FulfillmentOption.PICKUP), 7300, 40, 4.2, 61, 620.0, MoneyAmount(400000, "AOA"), null, false, true),
        MerchantSummary("fonte-fresca", "Fonte Fresca", "drinks", "Água, sumos e bebidas para entrega", null, MerchantAvailability.Open, setOf(FulfillmentOption.DELIVERY), 1800, 18, null, null, 470.0, null, PromotionSummary("entrega-bairro", "Entrega no bairro", "Condições apresentadas pelo comerciante", null), false, true),
        MerchantSummary("paes-mutamba", "Pães da Mutamba", "bakery", "Pão fresco e pequenos-almoços", null, MerchantAvailability.ClosingSoon(20), setOf(FulfillmentOption.PICKUP), 3200, 12, 4.8, 39, 680.0, null, null, false, true)
    )

    fun home(items: List<MerchantSummary> = merchants, hasLocation: Boolean = true): HomeDiscoveryContent {
        val featuredItems = items.filter { it.isFeatured }
        val discoveryCandidates = items.filterNot { it.isFeatured }
        val nearbyItems = if (hasLocation) discoveryCandidates.sortedBy { it.distanceMeters ?: Int.MAX_VALUE }.take(HOME_SECTION_SIZE) else emptyList()
        val recommendedItems = if (hasLocation) emptyList() else discoveryCandidates
            .sortedByDescending { it.popularity ?: Double.NEGATIVE_INFINITY }
            .take(HOME_SECTION_SIZE)
        return HomeDiscoveryContent(
            categories = categories,
            nearby = MerchantSection(nearbyItems, hasMore = hasLocation && discoveryCandidates.size > nearbyItems.size),
            recommended = MerchantSection(recommendedItems, hasMore = !hasLocation && discoveryCandidates.size > recommendedItems.size),
            featured = MerchantSection(featuredItems, hasMore = false)
        )
    }

    /** Overview is derived from the summary fixture so shared facts cannot diverge. */
    fun overview(summary: MerchantSummary): MerchantOverview = MerchantOverview(
        id = summary.id,
        name = summary.name,
        shortDescription = summary.shortDescription,
        fullDescription = summary.shortDescription?.let { "$it. Informação demonstrativa para apoiar a escolha do comerciante." },
        category = categories.first { it.id == summary.categoryId },
        bannerUrl = null,
        logoUrl = summary.imageUrl,
        availability = summary.availability,
        fulfillmentOptions = summary.fulfillmentOptions,
        rating = summary.rating,
        ratingCount = summary.ratingCount,
        address = MerchantAddress(
            when (summary.id) {
                "sabor-maianga" -> "Rua demonstrativa — Maianga, Luanda"
                "mercado-talatona" -> "Talatona, Luanda"
                else -> "${categories.first { it.id == summary.categoryId }.name} — Luanda"
            }
        ),
        contact = if (summary.id == "servicos-viana") null else MerchantContact(
            phone = "+244 900 000 000",
            email = "contacto@example.invalid"
        ),
        schedule = if (summary.id == "servicos-viana") null else WeeklySchedule(
            openDays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY),
            opensAt = LocalTime.of(8, 0),
            closesAt = LocalTime.of(20, 0)
        ),
        promotion = summary.promotion,
        catalogAvailable = summary.id != "servicos-viana"
    )
}
