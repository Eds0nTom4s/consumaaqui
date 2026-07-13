package ao.consuma.aqui.feature.home.data

import ao.consuma.aqui.feature.home.domain.model.FulfillmentOption
import ao.consuma.aqui.feature.home.domain.model.HomeDiscoveryContent
import ao.consuma.aqui.feature.home.domain.model.MerchantAvailability
import ao.consuma.aqui.feature.home.domain.model.MerchantCategory
import ao.consuma.aqui.feature.home.domain.model.MerchantSection
import ao.consuma.aqui.feature.home.domain.model.MerchantSummary
import ao.consuma.aqui.feature.home.domain.model.MoneyAmount
import ao.consuma.aqui.feature.home.domain.model.PromotionSummary

object HomeDiscoveryFixtures {
    val categories = listOf(
        MerchantCategory("restaurant", "Restaurantes"),
        MerchantCategory("bakery", "Pastelaria"),
        MerchantCategory("drinks", "Bebidas"),
        MerchantCategory("market", "Mercado"),
        MerchantCategory("services", "Serviços")
    )

    val merchants = listOf(
        MerchantSummary("sabor-maianga", "Sabor da Maianga", "restaurant", "Sabores angolanos preparados no dia", null, MerchantAvailability.Open, setOf(FulfillmentOption.DELIVERY, FulfillmentOption.PICKUP, FulfillmentOption.DINE_IN), 450, 25, 4.7, 125, MoneyAmount(550000, "AOA"), PromotionSummary("almoco-local", "Menu do dia", "Selecção especial ao almoço", "DESTAQUE"), true),
        MerchantSummary("doce-embondeiro", "Doce Embondeiro", "bakery", "Bolos, pão e pastelaria artesanal", "https://images.invalid/doce-embondeiro.jpg", MerchantAvailability.ClosingSoon(35), setOf(FulfillmentOption.PICKUP, FulfillmentOption.DELIVERY), 900, 20, 4.5, 82, MoneyAmount(250000, "AOA"), null, true),
        MerchantSummary("cafe-horizonte", "Café Horizonte", "drinks", "Café, sumos naturais e encontros tranquilos", null, MerchantAvailability.OpensAt("07:30"), setOf(FulfillmentOption.DINE_IN, FulfillmentOption.PICKUP), 1250, 15, 4.3, 44, null, null, false),
        MerchantSummary("mercado-talatona", "Mercado Talatona", "market", "Mercearia e essenciais para casa", "https://images.invalid/mercado-talatona.jpg", MerchantAvailability.Open, setOf(FulfillmentOption.DELIVERY, FulfillmentOption.PICKUP), 2400, 35, 4.6, 210, MoneyAmount(1000000, "AOA"), PromotionSummary("cabaz-semana", "Cabaz da semana", null, "OPORTUNIDADE"), true),
        MerchantSummary("servicos-viana", "Serviços Viana", "services", "Reparações e assistência ao domicílio", null, MerchantAvailability.Unknown, setOf(FulfillmentOption.SERVICE), 5100, null, null, null, null, null, false),
        MerchantSummary("cantinho-kilamba", "Cantinho do Kilamba", "restaurant", "Refeições familiares e grelhados", null, MerchantAvailability.Closed, setOf(FulfillmentOption.DINE_IN, FulfillmentOption.PICKUP), 7300, 40, 4.2, 61, MoneyAmount(400000, "AOA"), null, false),
        MerchantSummary("fonte-fresca", "Fonte Fresca", "drinks", "Água, sumos e bebidas para entrega", null, MerchantAvailability.Open, setOf(FulfillmentOption.DELIVERY), 1800, 18, null, null, null, PromotionSummary("entrega-bairro", "Entrega no bairro", "Condições apresentadas pelo comerciante", null), false),
        MerchantSummary("paes-mutamba", "Pães da Mutamba", "bakery", "Pão fresco e pequenos-almoços", null, MerchantAvailability.ClosingSoon(20), setOf(FulfillmentOption.PICKUP), 3200, 12, 4.8, 39, null, null, false)
    )

    fun content(items: List<MerchantSummary> = merchants): HomeDiscoveryContent = HomeDiscoveryContent(
        categories = categories,
        nearby = MerchantSection(items = items, hasMore = false),
        featured = MerchantSection(items = items.filter { it.isFeatured }, hasMore = false)
    )
}
