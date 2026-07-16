package ao.consuma.aqui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import ao.consuma.aqui.core.designsystem.components.ConsumaStatusSemantic
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.core.navigation.NavigationTestTags
import ao.consuma.aqui.feature.catalog.presentation.mapper.CatalogUiText
import ao.consuma.aqui.feature.catalog.presentation.mapper.ProductConfigurationErrorUiModel
import ao.consuma.aqui.feature.catalog.presentation.mapper.ProductDetailUiModel
import ao.consuma.aqui.feature.catalog.presentation.mapper.ProductOptionGroupUiModel
import ao.consuma.aqui.feature.catalog.presentation.mapper.ProductOptionUiModel
import ao.consuma.aqui.feature.catalog.presentation.mapper.ProductPriceSummaryUiModel
import ao.consuma.aqui.feature.catalog.presentation.product.ProductDetailContentUiModel
import ao.consuma.aqui.feature.catalog.presentation.product.ProductDetailScreen
import ao.consuma.aqui.feature.catalog.presentation.product.ProductDetailUiEvent
import ao.consuma.aqui.feature.catalog.presentation.product.ProductDetailUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProductDetailPresentationTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test fun product_loading_not_found_error_and_offline_are_identifiable() {
        var state by mutableStateOf<ProductDetailUiState>(ProductDetailUiState.Loading)
        composeTestRule.setContent { ConsumaAquiTheme { ProductDetailScreen(state, {}, {}) } }
        composeTestRule.onNodeWithTag(NavigationTestTags.PRODUCT_LOADING).assertIsDisplayed()
        composeTestRule.runOnIdle { state = ProductDetailUiState.NotFound }
        composeTestRule.onNodeWithText("Produto não encontrado").assertIsDisplayed()
        composeTestRule.runOnIdle {
            state = ProductDetailUiState.Error(CatalogUiText.Resource(R.string.product_error_generic), true)
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.PRODUCT_ERROR).assertIsDisplayed()
        composeTestRule.runOnIdle { state = ProductDetailUiState.Content(content(offline = true)) }
        composeTestRule.onNodeWithTag(NavigationTestTags.PRODUCT_OFFLINE).assertIsDisplayed()
    }

    @Test fun required_single_optional_multiple_and_unavailable_options_expose_semantics() {
        setProduct(ProductDetailUiState.Content(content()))
        composeTestRule.onNodeWithTag("${NavigationTestTags.PRODUCT_OPTION_GROUP}_size")
            .assertContentDescriptionContains("Grupo obrigatório", substring = true)
        composeTestRule.onNodeWithTag("${NavigationTestTags.PRODUCT_OPTION}_small").assertIsDisplayed()
        composeTestRule.onNodeWithTag("${NavigationTestTags.PRODUCT_OPTION}_blocked").assertIsDisplayed()
        composeTestRule.onNodeWithText("Opção indisponível").assertIsDisplayed()
        composeTestRule.onNodeWithText("Escolha 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Escolha até 2").assertIsDisplayed()
    }

    @Test fun quantity_total_and_add_have_accessible_actions_and_emit_events() {
        val events = mutableListOf<ProductDetailUiEvent>()
        setProduct(ProductDetailUiState.Content(content()), events::add)
        composeTestRule.onNodeWithTag(NavigationTestTags.PRODUCT_LIST).performScrollToNode(
            hasTestTag(NavigationTestTags.PRODUCT_QUANTITY)
        )
        composeTestRule.onNodeWithTag(NavigationTestTags.PRODUCT_QUANTITY)
            .assertContentDescriptionContains("Quantidade seleccionada: 1")
        composeTestRule.onNodeWithContentDescription("Diminuir quantidade").assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription("Aumentar quantidade").assertIsEnabled().performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.PRODUCT_ADD).performClick()
        assertEquals(listOf(ProductDetailUiEvent.QuantityIncreased, ProductDetailUiEvent.Add), events)
    }

    @Test fun validation_error_is_associated_with_first_group_semantics() {
        val invalid = content().copy(
            selections = emptyMap(),
            validationErrors = listOf(
                ProductConfigurationErrorUiModel("size", CatalogUiText.Dynamic("Seleccione pelo menos 1 opção."))
            )
        )
        setProduct(ProductDetailUiState.Content(invalid))
        composeTestRule.onNodeWithTag("${NavigationTestTags.PRODUCT_OPTION_GROUP}_size").assertIsDisplayed()
        composeTestRule.onNodeWithText("Seleccione pelo menos 1 opção.").assertIsDisplayed()
    }

    @Test fun unavailable_product_stays_visible_and_add_is_disabled_in_dark_theme() {
        setProduct(ProductDetailUiState.Unavailable(detail().copy(canConfigure = false), false), dark = true)
        composeTestRule.onNodeWithText("Muamba da Casa").assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.PRODUCT_ADD).assertIsNotEnabled()
    }

    @Test fun product_at_one_point_three_and_one_point_five_font_scale_remains_scrollable() {
        var fontScale by mutableStateOf(1.3f)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
                ConsumaAquiTheme { ProductDetailScreen(ProductDetailUiState.Content(content()), {}, {}) }
            }
        }
        listOf(1.3f, 1.5f).forEach { scale ->
            composeTestRule.runOnIdle { fontScale = scale }
            composeTestRule.onNodeWithTag(NavigationTestTags.PRODUCT_LIST).performScrollToNode(
                hasTestTag(NavigationTestTags.PRODUCT_TOTAL)
            )
            composeTestRule.onNodeWithTag(NavigationTestTags.PRODUCT_TOTAL)
                .assertContentDescriptionContains("Total estimado", substring = true)
            composeTestRule.onNodeWithTag(NavigationTestTags.PRODUCT_ADD).assertIsDisplayed()
        }
    }

    private fun setProduct(
        state: ProductDetailUiState,
        onEvent: (ProductDetailUiEvent) -> Unit = {},
        dark: Boolean = false
    ) = composeTestRule.setContent {
        ConsumaAquiTheme(darkTheme = dark) { ProductDetailScreen(state, {}, onEvent) }
    }

    private fun content(offline: Boolean = false) = ProductDetailContentUiModel(
        product = detail(),
        selections = mapOf("size" to setOf("small")),
        quantity = 1,
        note = "",
        validationErrors = emptyList(),
        priceSummary = ProductPriceSummaryUiModel("4.500 Kz", "0 Kz", "4.500 Kz"),
        canAdd = true,
        isOffline = offline
    )

    private fun detail() = ProductDetailUiModel(
        id = "product",
        merchantId = "merchant",
        name = "Muamba da Casa",
        fullDescription = "Prato angolano",
        hasImage = false,
        priceText = "4.500 Kz",
        compareAtPriceText = "5.000 Kz",
        availabilityLabel = CatalogUiText.Dynamic("Disponível"),
        availabilitySemantic = ConsumaStatusSemantic.SUCCESS,
        preparationText = CatalogUiText.Dynamic("Preparação: 20 min"),
        optionGroups = listOf(
            ProductOptionGroupUiModel(
                "size", "Tamanho", null, true, 1, 1, true,
                CatalogUiText.Dynamic("Escolha 1"),
                listOf(
                    ProductOptionUiModel("small", "Pequeno", null, null, true, true),
                    ProductOptionUiModel("large", "Grande", null, "+500 Kz", true, false)
                )
            ),
            ProductOptionGroupUiModel(
                "extras", "Adicionais", null, false, 0, 2, false,
                CatalogUiText.Dynamic("Escolha até 2"),
                listOf(ProductOptionUiModel("blocked", "Bacon", null, "+300 Kz", false, false))
            )
        ),
        canConfigure = true
    )
}
