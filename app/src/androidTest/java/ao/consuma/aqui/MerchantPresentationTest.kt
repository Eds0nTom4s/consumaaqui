package ao.consuma.aqui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import ao.consuma.aqui.core.designsystem.components.ConsumaStatusSemantic
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.core.navigation.NavigationTestTags
import ao.consuma.aqui.feature.discovery.presentation.mapper.MerchantOverviewUiModel
import ao.consuma.aqui.feature.discovery.presentation.mapper.UiText
import ao.consuma.aqui.feature.discovery.presentation.merchant.*
import org.junit.Rule
import org.junit.Test

class MerchantPresentationTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test fun overview_loading_state_composes() {
        composeTestRule.setContent {
            ConsumaAquiTheme {
                MerchantOverviewScreen(MerchantOverviewUiState.Loading, {}, {}, {})
            }
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.MERCHANT_LOADING).assertIsDisplayed()
    }

    @Test fun overview_not_found_state_composes() {
        composeTestRule.setContent {
            ConsumaAquiTheme {
                MerchantOverviewScreen(MerchantOverviewUiState.NotFound, {}, {}, {})
            }
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.MERCHANT_NOT_FOUND).assertIsDisplayed()
    }

    @Test fun overview_error_state_composes() {
        composeTestRule.setContent {
            ConsumaAquiTheme {
                MerchantOverviewScreen(
                    MerchantOverviewUiState.Error(UiText.Resource(R.string.merchant_error_generic), true),
                    {}, {}, {}
                )
            }
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.MERCHANT_ERROR).assertIsDisplayed()
    }

    @Test fun complete_overview_exposes_semantic_content_and_catalog_action() {
        var opened: String? = null
        composeTestRule.setContent {
            ConsumaAquiTheme {
                MerchantOverviewScreen(
                    MerchantOverviewUiState.Content(overview()),
                    {}, {}, { opened = it }
                )
            }
        }
        composeTestRule.onNodeWithText("Sabor da Maianga").assertIsDisplayed()
        composeTestRule.onNodeWithText("Restaurantes").assertIsDisplayed()
        composeTestRule.onNodeWithText("Avaliação").assertIsDisplayed()
        composeTestRule.onNodeWithText("Contacto").assertIsDisplayed()
        composeTestRule.onNodeWithText("Horário").assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.MERCHANT_VIEW_CATALOG).performClick()
        org.junit.Assert.assertEquals("sabor-maianga", opened)
    }

    @Test fun partial_overview_disables_catalog_and_dark_theme_composes() {
        composeTestRule.setContent {
            ConsumaAquiTheme(darkTheme = true) {
                MerchantOverviewScreen(
                    MerchantOverviewUiState.Content(
                        overview().copy(
                            contactText = null,
                            openingHoursText = null,
                            promotionText = null,
                            catalogAvailable = false
                        )
                    ),
                    {}, {}, {}
                )
            }
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.MERCHANT_CATALOG_UNAVAILABLE).assertIsDisplayed().assertIsNotEnabled()
    }

    @Test fun overview_at_one_point_five_font_scale_keeps_catalog_accessible() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                ConsumaAquiTheme {
                    MerchantOverviewScreen(MerchantOverviewUiState.Content(overview()), {}, {}, {})
                }
            }
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.MERCHANT_LIST).performScrollToIndex(9)
        composeTestRule.onNodeWithTag(NavigationTestTags.MERCHANT_VIEW_CATALOG).assertIsDisplayed()
    }

    private fun overview() = MerchantOverviewUiModel(
        id = "sabor-maianga",
        name = "Sabor da Maianga",
        shortDescription = "Sabores angolanos",
        fullDescription = "Sabores angolanos preparados no dia.",
        category = "Restaurantes",
        hasBanner = false,
        hasLogo = false,
        availabilityLabel = UiText.Resource(R.string.home_availability_open),
        availabilitySemantic = ConsumaStatusSemantic.SUCCESS,
        fulfillmentText = UiText.Resource(R.string.home_fulfillment_delivery),
        contactText = "+244 900 000 000",
        openingHoursText = UiText.Resource(R.string.merchant_schedule_monday_saturday, listOf("08:00", "20:00")),
        addressText = "Maianga, Luanda",
        ratingText = UiText.Dynamic("4,7 (125 avaliações)"),
        promotionText = "MENU DO DIA",
        catalogAvailable = true
    )

}
