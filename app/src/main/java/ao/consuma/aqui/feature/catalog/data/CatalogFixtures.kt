package ao.consuma.aqui.feature.catalog.data

import ao.consuma.aqui.core.domain.model.MoneyAmount
import ao.consuma.aqui.feature.catalog.domain.model.CatalogCategory
import ao.consuma.aqui.feature.catalog.domain.model.CatalogProduct
import ao.consuma.aqui.feature.catalog.domain.model.MerchantCatalog
import ao.consuma.aqui.feature.catalog.domain.model.ProductAvailability
import ao.consuma.aqui.feature.catalog.domain.model.ProductOption
import ao.consuma.aqui.feature.catalog.domain.model.ProductOptionGroup
import ao.consuma.aqui.feature.catalog.domain.model.SelectionRule
import java.time.Instant
import java.time.LocalTime

/** Demonstration data only; it is not a backend payload contract. */
object CatalogFixtures {
    const val CURRENCY = "AOA"

    val knownMerchantIds = setOf(
        "sabor-maianga",
        "doce-embondeiro",
        "cafe-horizonte",
        "mercado-talatona",
        "servicos-viana",
        "cantinho-kilamba",
        "fonte-fresca",
        "paes-mutamba"
    )

    val catalogUnavailableMerchantIds = setOf("servicos-viana")

    val catalogs: List<MerchantCatalog> = listOf(
        restaurantCatalog(),
        bakeryCatalog(),
        cafeCatalog(),
        marketCatalog(),
        simplifiedRestaurantCatalog(),
        emptyDrinksCatalog(),
        simplifiedBreadCatalog()
    )

    fun catalog(merchantId: String): MerchantCatalog? = catalogs.find { it.merchantId == merchantId }

    fun product(merchantId: String, productId: String): CatalogProduct? =
        catalog(merchantId)?.products?.find { it.id == productId }

    private fun restaurantCatalog(): MerchantCatalog {
        val merchant = "sabor-maianga"
        val categories = listOf(
            category(merchant, "pratos", "Pratos", 0),
            category(merchant, "grelhados", "Grelhados", 1),
            category(merchant, "bebidas", "Bebidas", 2),
            category(merchant, "sobremesas", "Sobremesas", 3)
        )
        return catalog(
            merchant,
            "Catálogo Sabor da Maianga",
            "Refeições e sabores demonstrativos preparados no dia.",
            categories,
            listOf(
                product(merchant, "pratos", "muamba-casa", "Muamba da Casa", 450000, 0, featured = true, options = listOf(portionGroup("muamba-casa"), sideGroup("muamba-casa")), tags = setOf("angolano", "frango"), image = true),
                product(merchant, "pratos", "calulu-demonstrativo", "Calulu Demonstrativo", 420000, 1, availability = ProductAvailability.OutOfStock, fullDescription = null, tags = setOf("peixe")),
                product(merchant, "pratos", "funge-com-acompanhamento", "Funge com Acompanhamento", 350000, 2, shortDescription = null, preparationMinutes = null),
                product(merchant, "grelhados", "frango-grelhado", "Frango Grelhado", 390000, 0, compareAt = 450000, featured = true, options = listOf(sauceGroup("frango-grelhado"), extrasGroup("frango-grelhado"))),
                product(merchant, "grelhados", "peixe-grelhado", "Peixe Grelhado", 520000, 1, options = listOf(sideGroup("peixe-grelhado"))),
                product(merchant, "grelhados", "espetada-casa", "Espetada da Casa", 470000, 2, availability = ProductAvailability.TemporarilyUnavailable),
                product(merchant, "bebidas", "sumo-mucua", "Sumo de Múcua", 120000, 0, featured = true, tags = setOf("sumo", "natural")),
                product(merchant, "bebidas", "sumo-gengibre", "Sumo de Gengibre", 110000, 1, preparationMinutes = null),
                product(merchant, "bebidas", "agua-casa", "Água da Casa", 50000, 2, shortDescription = null, fullDescription = null, preparationMinutes = null),
                product(merchant, "sobremesas", "cocada", "Cocada", 90000, 0, tags = setOf("doce")),
                product(merchant, "sobremesas", "bolo-banana", "Bolo de Banana", 100000, 1, compareAt = 120000),
                product(merchant, "sobremesas", "fruta-estacao", "Fruta da Estação", 80000, 2, availability = ProductAvailability.AvailableFrom(LocalTime.of(12, 0)), preparationMinutes = null)
            )
        )
    }

    private fun bakeryCatalog(): MerchantCatalog {
        val merchant = "doce-embondeiro"
        val categories = listOf(
            category(merchant, "paes", "Pães", 0),
            category(merchant, "bolos", "Bolos", 1),
            category(merchant, "salgados", "Salgados", 2),
            category(merchant, "bebidas", "Bebidas", 3)
        )
        return catalog(
            merchant,
            "Selecção Doce Embondeiro",
            "Pastelaria artesanal fictícia para demonstração.",
            categories,
            listOf(
                product(merchant, "paes", "pao-centeio", "Pão de Centeio", 70000, 0, featured = true, image = true, preparationMinutes = null),
                product(merchant, "paes", "pao-sementes", "Pão de Sementes", 85000, 1, tags = setOf("artesanal"), preparationMinutes = null),
                product(merchant, "paes", "pao-doce", "Pão Doce", 60000, 2, availability = ProductAvailability.OutOfStock, preparationMinutes = null),
                product(merchant, "bolos", "bolo-chocolate", "Bolo de Chocolate", 750000, 0, compareAt = 850000, options = listOf(portionGroup("bolo-chocolate"))),
                product(merchant, "bolos", "bolo-coco", "Bolo de Coco", 680000, 1, fullDescription = null),
                product(merchant, "bolos", "fatia-cenoura", "Fatia de Cenoura", 95000, 2, shortDescription = null),
                product(merchant, "salgados", "empada-frango", "Empada de Frango", 85000, 0, featured = true),
                product(merchant, "salgados", "pastel-legumes", "Pastel de Legumes", 75000, 1, tags = setOf("vegetal")),
                product(merchant, "salgados", "croquete-casa", "Croquete da Casa", 80000, 2, availability = ProductAvailability.TemporarilyUnavailable),
                product(merchant, "bebidas", "cafe-curto", "Café Curto", 60000, 0, options = listOf(sizeGroup("cafe-curto")), preparationMinutes = 5),
                product(merchant, "bebidas", "cha-casa", "Chá da Casa", 70000, 1, preparationMinutes = 5),
                product(merchant, "bebidas", "sumo-fruta", "Sumo de Fruta", 100000, 2, preparationMinutes = null)
            )
        )
    }

    private fun cafeCatalog(): MerchantCatalog {
        val merchant = "cafe-horizonte"
        val categories = listOf(
            category(merchant, "cafes", "Cafés", 0),
            category(merchant, "infusoes", "Infusões", 1),
            category(merchant, "sumos", "Sumos", 2),
            category(merchant, "lanches", "Lanches", 3)
        )
        return catalog(
            merchant,
            "Menu Café Horizonte",
            null,
            categories,
            listOf(
                product(merchant, "cafes", "espresso-horizonte", "Espresso Horizonte", 65000, 0, featured = true, options = listOf(sizeGroup("espresso-horizonte"))),
                product(merchant, "cafes", "cafe-leite", "Café com Leite", 90000, 1, options = listOf(sizeGroup("cafe-leite"))),
                product(merchant, "cafes", "cafe-gelado", "Café Gelado", 130000, 2, availability = ProductAvailability.AvailableFrom(LocalTime.of(10, 0))),
                product(merchant, "infusoes", "cha-hibisco", "Infusão de Hibisco", 90000, 0, preparationMinutes = 6),
                product(merchant, "infusoes", "cha-gengibre", "Infusão de Gengibre", 95000, 1, preparationMinutes = 6),
                product(merchant, "infusoes", "cha-folhas", "Infusão de Folhas", 85000, 2, availability = ProductAvailability.Unknown),
                product(merchant, "sumos", "sumo-manga", "Sumo de Manga", 120000, 0, featured = true, image = true),
                product(merchant, "sumos", "sumo-abacaxi", "Sumo de Abacaxi", 120000, 1),
                product(merchant, "sumos", "limonada-casa", "Limonada da Casa", 100000, 2, compareAt = 125000),
                product(merchant, "lanches", "sandes-frango", "Sandes de Frango", 240000, 0, options = listOf(sauceGroup("sandes-frango"), extrasGroup("sandes-frango"))),
                product(merchant, "lanches", "tosta-vegetal", "Tosta Vegetal", 210000, 1, tags = setOf("vegetal")),
                product(merchant, "lanches", "biscoitos-casa", "Biscoitos da Casa", 80000, 2, shortDescription = null, preparationMinutes = null)
            )
        )
    }

    private fun marketCatalog(): MerchantCatalog {
        val merchant = "mercado-talatona"
        val categories = listOf(
            category(merchant, "mercearia", "Mercearia", 0),
            category(merchant, "bebidas", "Bebidas", 1),
            category(merchant, "higiene", "Higiene", 2),
            category(merchant, "frescos", "Frescos", 3, available = false)
        )
        return catalog(
            merchant,
            "Mercado Talatona",
            "Essenciais demonstrativos para casa.",
            categories,
            listOf(
                product(merchant, "mercearia", "arroz-casa", "Arroz da Casa", 180000, 0, featured = true, preparationMinutes = null),
                product(merchant, "mercearia", "farinha-milho", "Farinha de Milho", 90000, 1, preparationMinutes = null),
                product(merchant, "mercearia", "feijao-seleccionado", "Feijão Seleccionado", 150000, 2, preparationMinutes = null),
                product(merchant, "bebidas", "agua-mesa", "Água de Mesa", 50000, 0, preparationMinutes = null),
                product(merchant, "bebidas", "sumo-tropical", "Sumo Tropical", 95000, 1, image = true, preparationMinutes = null),
                product(merchant, "bebidas", "bebida-gengibre", "Bebida de Gengibre", 100000, 2, compareAt = 120000, preparationMinutes = null),
                product(merchant, "higiene", "sabonete-neutro", "Sabonete Neutro", 70000, 0, preparationMinutes = null),
                product(merchant, "higiene", "detergente-casa", "Detergente da Casa", 130000, 1, preparationMinutes = null),
                product(merchant, "higiene", "papel-casa", "Papel para Casa", 160000, 2, availability = ProductAvailability.OutOfStock, preparationMinutes = null),
                product(merchant, "frescos", "tomate-fresco", "Tomate Fresco", 100000, 0, preparationMinutes = null),
                product(merchant, "frescos", "cebola-fresca", "Cebola Fresca", 90000, 1, preparationMinutes = null),
                product(merchant, "frescos", "folhas-estacao", "Folhas da Estação", 80000, 2, availability = ProductAvailability.Unavailable, preparationMinutes = null)
            )
        )
    }

    private fun simplifiedRestaurantCatalog(): MerchantCatalog {
        val merchant = "cantinho-kilamba"
        val categories = listOf(
            category(merchant, "refeicoes", "Refeições", 0),
            category(merchant, "bebidas", "Bebidas", 1)
        )
        return catalog(
            merchant,
            "Cantinho do Kilamba",
            null,
            categories,
            listOf(
                product(merchant, "refeicoes", "grelhado-familiar", "Grelhado Familiar", 650000, 0, options = listOf(sideGroup("grelhado-familiar"))),
                product(merchant, "refeicoes", "prato-dia", "Prato do Dia", 350000, 1, featured = true),
                product(merchant, "bebidas", "sumo-kilamba", "Sumo do Kilamba", 100000, 0),
                product(merchant, "bebidas", "agua-kilamba", "Água", 50000, 1, preparationMinutes = null)
            )
        )
    }

    private fun emptyDrinksCatalog(): MerchantCatalog {
        val merchant = "fonte-fresca"
        return catalog(
            merchant,
            "Fonte Fresca",
            "Catálogo publicado sem produtos neste cenário controlado.",
            listOf(category(merchant, "bebidas", "Bebidas", 0)),
            emptyList()
        )
    }

    private fun simplifiedBreadCatalog(): MerchantCatalog {
        val merchant = "paes-mutamba"
        val categories = listOf(
            category(merchant, "paes", "Pães", 0),
            category(merchant, "pequeno-almoco", "Pequeno-almoço", 1)
        )
        return catalog(
            merchant,
            "Pães da Mutamba",
            null,
            categories,
            listOf(
                product(merchant, "paes", "pao-mutamba", "Pão da Mutamba", 55000, 0, featured = true, preparationMinutes = null),
                product(merchant, "paes", "pao-integral", "Pão Integral", 70000, 1, preparationMinutes = null),
                product(merchant, "pequeno-almoco", "cafe-pao", "Café com Pão", 130000, 0, options = listOf(sizeGroup("cafe-pao"))),
                product(merchant, "pequeno-almoco", "cha-torrada", "Chá com Torrada", 140000, 1)
            )
        )
    }

    private fun catalog(
        merchantId: String,
        name: String,
        description: String?,
        categories: List<CatalogCategory>,
        products: List<CatalogProduct>
    ) = MerchantCatalog(
        merchantId = merchantId,
        catalogId = "catalog-$merchantId",
        name = name,
        description = description,
        categories = categories,
        products = products,
        currencyCode = CURRENCY,
        updatedAt = Instant.parse("2026-07-01T08:00:00Z"),
        version = "mock-1"
    )

    private fun category(
        merchantId: String,
        slug: String,
        name: String,
        sortOrder: Int,
        available: Boolean = true
    ) = CatalogCategory(
        id = categoryId(merchantId, slug),
        merchantId = merchantId,
        name = name,
        description = null,
        imageUrl = null,
        sortOrder = sortOrder,
        available = available
    )

    private fun product(
        merchantId: String,
        categorySlug: String,
        slug: String,
        name: String,
        price: Long,
        sortOrder: Int,
        compareAt: Long? = null,
        availability: ProductAvailability = ProductAvailability.Available,
        shortDescription: String? = "Produto fictício preparado para demonstração.",
        fullDescription: String? = "Descrição demonstrativa sem compromisso comercial.",
        preparationMinutes: Int? = 20,
        tags: Set<String> = emptySet(),
        options: List<ProductOptionGroup> = emptyList(),
        featured: Boolean = false,
        image: Boolean = false
    ) = CatalogProduct(
        id = "$merchantId-product-$slug",
        merchantId = merchantId,
        categoryId = categoryId(merchantId, categorySlug),
        name = name,
        shortDescription = shortDescription,
        fullDescription = fullDescription,
        imageUrl = if (image) "https://images.invalid/catalog/$merchantId-$slug.jpg" else null,
        basePrice = money(price),
        compareAtPrice = compareAt?.let(::money),
        availability = availability,
        preparationMinutes = preparationMinutes,
        tags = tags,
        optionGroups = options,
        featured = featured,
        sortOrder = sortOrder
    )

    private fun categoryId(merchantId: String, slug: String) = "$merchantId-category-$slug"
    private fun money(amountMinor: Long) = MoneyAmount(amountMinor, CURRENCY)

    private fun sizeGroup(prefix: String) = ProductOptionGroup(
        id = "$prefix-size",
        name = "Tamanho",
        description = "Seleccione um tamanho.",
        selectionRule = SelectionRule(1, 1),
        options = listOf(
            option("$prefix-size-small", "Pequeno", 0, 0, default = true),
            option("$prefix-size-medium", "Médio", 30000, 1),
            option("$prefix-size-large", "Grande", 60000, 2)
        ),
        sortOrder = 0
    )

    private fun portionGroup(prefix: String) = ProductOptionGroup(
        id = "$prefix-portion",
        name = "Porção",
        description = null,
        selectionRule = SelectionRule(1, 1),
        options = listOf(
            option("$prefix-portion-individual", "Individual", 0, 0, default = true),
            option("$prefix-portion-share", "Para partilhar", 180000, 1)
        ),
        sortOrder = 0
    )

    private fun sauceGroup(prefix: String) = ProductOptionGroup(
        id = "$prefix-sauce",
        name = "Molho",
        description = "Escolha até um molho.",
        selectionRule = SelectionRule(0, 1),
        options = listOf(
            option("$prefix-sauce-none", "Sem molho", 0, 0, default = true),
            option("$prefix-sauce-spicy", "Picante", 0, 1),
            option("$prefix-sauce-garlic", "Alho", 20000, 2)
        ),
        sortOrder = 1
    )

    private fun extrasGroup(prefix: String) = ProductOptionGroup(
        id = "$prefix-extras",
        name = "Adicionais",
        description = "Escolha até dois adicionais.",
        selectionRule = SelectionRule(0, 2),
        options = listOf(
            option("$prefix-extras-cheese", "Queijo extra", 50000, 0),
            option("$prefix-extras-egg", "Ovo", 40000, 1),
            option("$prefix-extras-bacon", "Bacon", 70000, 2, available = false)
        ),
        sortOrder = 2
    )

    private fun sideGroup(prefix: String) = ProductOptionGroup(
        id = "$prefix-side",
        name = "Acompanhamento",
        description = null,
        selectionRule = SelectionRule(1, 1),
        options = listOf(
            option("$prefix-side-rice", "Arroz", 0, 0, default = true),
            option("$prefix-side-funge", "Funge", 20000, 1),
            option("$prefix-side-salad", "Salada", 30000, 2)
        ),
        sortOrder = 1
    )

    private fun option(
        id: String,
        name: String,
        price: Long,
        sortOrder: Int,
        default: Boolean = false,
        available: Boolean = true
    ) = ProductOption(
        id = id,
        name = name,
        description = null,
        additionalPrice = money(price),
        available = available,
        sortOrder = sortOrder,
        defaultSelected = default
    )
}
