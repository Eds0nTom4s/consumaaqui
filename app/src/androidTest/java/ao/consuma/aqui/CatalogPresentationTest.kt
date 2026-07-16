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
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import ao.consuma.aqui.core.designsystem.components.ConsumaStatusSemantic
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.core.navigation.NavigationTestTags
import ao.consuma.aqui.feature.catalog.presentation.catalog.CatalogEmptyReason
import ao.consuma.aqui.feature.catalog.presentation.catalog.CatalogScreen
import ao.consuma.aqui.feature.catalog.presentation.catalog.CatalogUiEvent
import ao.consuma.aqui.feature.catalog.presentation.catalog.CatalogUiState
import ao.consuma.aqui.feature.catalog.presentation.mapper.CatalogCategoryUiModel
import ao.consuma.aqui.feature.catalog.presentation.mapper.CatalogContentUiModel
import ao.consuma.aqui.feature.catalog.presentation.mapper.CatalogProductCardUiModel
import ao.consuma.aqui.feature.catalog.presentation.mapper.CatalogUiText
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CatalogPresentationTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test fun catalog_loading_empty_error_and_not_found_states_are_identifiable() {
        var state by mutableStateOf<CatalogUiState>(CatalogUiState.Loading)
        composeTestRule.setContent { ConsumaAquiTheme { CatalogScreen(state, {}, {}) } }
        composeTestRule.onNodeWithTag(NavigationTestTags.CATALOG_LOADING).assertIsDisplayed()
        composeTestRule.runOnIdle {
            state = CatalogUiState.Empty(content(products = emptyList()), CatalogEmptyReason.SEARCH_EMPTY)
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.CATALOG_EMPTY).assertIsDisplayed()
        composeTestRule.runOnIdle {
            state = CatalogUiState.Error(CatalogUiText.Resource(R.string.catalog_error_generic), true)
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.CATALOG_ERROR).assertIsDisplayed()
        composeTestRule.runOnIdle { state = CatalogUiState.CatalogNotFound }
        composeTestRule.onNodeWithText("Catálogo não encontrado").assertIsDisplayed()
    }

    @Test fun catalog_content_exposes_search_categories_product_semantics_and_callbacks() {
        val events = mutableListOf<CatalogUiEvent>()
        setCatalog(CatalogUiState.Content(content()), onEvent = events::add)
        composeTestRule.onNodeWithTag(NavigationTestTags.CATALOG_SEARCH).performTextInput("muamba")
        composeTestRule.onNodeWithTag("${NavigationTestTags.CATALOG_CATEGORY}_pratos").performClick()
        composeTestRule.onNodeWithTag("${NavigationTestTags.CATALOG_PRODUCT}_product")
            .assertContentDescriptionContains("Muamba da Casa", substring = true)
            .performClick()
        assertTrue(events.contains(CatalogUiEvent.QueryChanged("muamba")))
        assertTrue(events.contains(CatalogUiEvent.CategorySelected("pratos")))
        assertTrue(events.contains(CatalogUiEvent.ProductSelected("product")))
    }

    @Test fun selected_and_unavailable_categories_have_accessible_state() {
        setCatalog(CatalogUiState.Content(content(selectedCategoryId = "pratos")))
        composeTestRule.onNodeWithTag("${NavigationTestTags.CATALOG_CATEGORY}_pratos")
            .assertContentDescriptionContains("Pratos")
            .assertIsEnabled()
        composeTestRule.onNodeWithTag("${NavigationTestTags.CATALOG_CATEGORY}_blocked")
            .assertIsNotEnabled()
    }

    @Test fun offline_discount_missing_image_and_options_compose_in_dark_theme() {
        setCatalog(CatalogUiState.Content(content(offline = true)), dark = true)
        composeTestRule.onNodeWithTag(NavigationTestTags.CATALOG_OFFLINE).assertIsDisplayed()
        composeTestRule.onNodeWithTag("${NavigationTestTags.CATALOG_PRODUCT}_product")
            .assertContentDescriptionContains("preço anterior 5.000 Kz", substring = true)
            .assertContentDescriptionContains("possui opções", substring = true)
    }

    @Test fun catalog_at_one_point_three_and_one_point_five_font_scale_remains_scrollable() {
        var fontScale by mutableStateOf(1.3f)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
                ConsumaAquiTheme { CatalogScreen(CatalogUiState.Content(content()), {}, {}) }
            }
        }
        listOf(1.3f, 1.5f).forEach { scale ->
            composeTestRule.runOnIdle { fontScale = scale }
            composeTestRule.onNodeWithTag(NavigationTestTags.CATALOG_LIST).performScrollToNode(
                hasTestTag("${NavigationTestTags.CATALOG_PRODUCT}_product")
            )
            composeTestRule.onNodeWithTag("${NavigationTestTags.CATALOG_PRODUCT}_product").assertIsDisplayed()
        }
    }

    private fun setCatalog(
        state: CatalogUiState,
        dark: Boolean = false,
        onEvent: (CatalogUiEvent) -> Unit = {}
    ) = composeTestRule.setContent {
        ConsumaAquiTheme(darkTheme = dark) { CatalogScreen(state, {}, onEvent) }
    }

    private fun content(
        products: List<CatalogProductCardUiModel> = listOf(product()),
        selectedCategoryId: String? = null,
        offline: Boolean = false
    ) = CatalogContentUiModel(
        merchantId = "merchant",
        title = "Sabor da Maianga",
        description = "Refeições do dia",
        query = "",
        categories = listOf(
            CatalogCategoryUiModel(null, CatalogUiText.Dynamic("Todas"), true),
            CatalogCategoryUiModel("pratos", CatalogUiText.Dynamic("Pratos"), true),
            CatalogCategoryUiModel("blocked", CatalogUiText.Dynamic("Indisponível"), false)
        ),
        selectedCategoryId = selectedCategoryId,
        products = products,
        isSearchMode = false,
        isRefreshing = false,
        isOffline = offline,
        resultContext = CatalogUiText.Dynamic("1 produto")
    )

    private fun product() = CatalogProductCardUiModel(
        id = "product",
        merchantId = "merchant",
        name = "Muamba da Casa",
        description = "Prato angolano",
        hasImage = false,
        priceText = "4.500 Kz",
        compareAtPriceText = "5.000 Kz",
        availabilityLabel = CatalogUiText.Dynamic("Disponível"),
        availabilitySemantic = ConsumaStatusSemantic.SUCCESS,
        preparationText = CatalogUiText.Dynamic("Preparação: 20 min"),
        hasOptions = true,
        featured = true,
        categoryName = "Pratos",
        accessibilityDescription = CatalogUiText.Dynamic(
            "Muamba da Casa, preço 4.500 Kz, preço anterior 5.000 Kz, disponível, destaque, possui opções"
        )
    )
}
