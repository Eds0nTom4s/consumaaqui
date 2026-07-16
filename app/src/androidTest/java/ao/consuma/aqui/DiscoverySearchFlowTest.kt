package ao.consuma.aqui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import ao.consuma.aqui.core.navigation.NavigationTestTags
import ao.consuma.aqui.feature.discovery.data.InMemoryDiscoveryRepository
import ao.consuma.aqui.feature.discovery.data.MockDiscoveryScenario
import ao.consuma.aqui.feature.discovery.data.modules.DiscoveryDataModule
import ao.consuma.aqui.feature.discovery.domain.repository.DiscoveryRepository
import ao.consuma.aqui.feature.launch.di.LaunchStateModule
import ao.consuma.aqui.feature.launch.domain.*
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
@UninstallModules(LaunchStateModule::class, DiscoveryDataModule::class)
class DiscoverySearchFlowTest {
    private val repository = InMemoryDiscoveryRepository()

    @BindValue @JvmField
    var appLaunchStateRepository: AppLaunchStateRepository = InMemoryAppLaunchStateRepository()

    @BindValue @JvmField
    var discoveryRepository: DiscoveryRepository = repository

    @get:Rule(order = 0)
    val launchStateRule = LaunchStateRule(
        setup = {
            completeOnboarding()
            selectLocation(MockLocation("maianga", "Luanda", "Maianga", "Luanda — Maianga"))
        },
        assign = { appLaunchStateRepository = it }
    )
    @get:Rule(order = 1) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 2) val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test fun search_opens_in_exploration_and_filters_by_name_description_and_category() {
        openSearch()
        composeTestRule.onNodeWithText("Explore comerciantes").assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_FIELD).performTextInput("Horizonte")
        composeTestRule.onNodeWithText("Café Horizonte").assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_FIELD).performTextClearance()
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_FIELD).performTextInput("essenciais")
        composeTestRule.onNodeWithText("Mercado Talatona").assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_FIELD).performTextClearance()
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_FILTER_ACTION).performClick()
        composeTestRule.onNodeWithTag("${NavigationTestTags.SEARCH_CATEGORY}_bakery").performClick()
        composeTestRule.onNodeWithText("Doce Embondeiro").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pães da Mutamba").assertIsDisplayed()
    }

    @Test fun multiple_fulfillment_filters_use_or_and_clear_preserves_query() {
        openSearch()
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_FIELD).performTextInput("a")
        composeTestRule.onNodeWithTag("${NavigationTestTags.SEARCH_FULFILLMENT}_DELIVERY").performClick()
        composeTestRule.onNodeWithTag("${NavigationTestTags.SEARCH_FULFILLMENT}_SERVICE").performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_ACTIVE_FILTERS).assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_LIST).performScrollToNode(
            hasTestTag("${NavigationTestTags.SEARCH_MERCHANT}_servicos-viana")
        )
        composeTestRule.onNodeWithTag("${NavigationTestTags.SEARCH_MERCHANT}_servicos-viana").assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_LIST).performScrollToIndex(3)
        composeTestRule.onNodeWithText("Limpar filtros").performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_FIELD).assertTextContains("a")
    }

    @Test fun query_filters_and_sort_survive_rotation_and_tab_switch() {
        openSearch()
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_FIELD).performTextInput("café")
        composeTestRule.onNodeWithTag("${NavigationTestTags.SEARCH_FULFILLMENT}_DELIVERY").performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_SORT_ACTION).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_LIST).performScrollToIndex(4)
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_SORT_LIST).performScrollToIndex(4)
        composeTestRule.onNodeWithTag("${NavigationTestTags.SEARCH_SORT_OPTION}_NAME").performClick()
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_FIELD).assertTextContains("café")
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_ACTIVE_FILTERS).assertIsDisplayed()
        composeTestRule.onNodeWithText("Ordenar: Nome").assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAV_HOME).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAV_SEARCH).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_FIELD).assertTextContains("café")
    }

    @Test fun search_error_retries_and_offline_keeps_results() {
        openSearch()
        repository.scenario = MockDiscoveryScenario.ERROR
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_FIELD).performTextInput("café")
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_ERROR).assertIsDisplayed()
        repository.scenario = MockDiscoveryScenario.CONTENT
        composeTestRule.onNodeWithText("Tentar Novamente").performClick()
        composeTestRule.onNodeWithText("Café Horizonte").assertIsDisplayed()
        repository.scenario = MockDiscoveryScenario.OFFLINE
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_FIELD).performTextInput(" ")
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_OFFLINE).assertExists()
        composeTestRule.onNodeWithText("Os dados apresentados podem estar desactualizados.").assertExists()
        composeTestRule.onNodeWithText("Café Horizonte").assertExists()
    }

    @Test fun merchant_overview_and_catalog_hide_bottom_navigation_and_return_to_search_state() {
        openSearch()
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_FIELD).performTextInput("Sabor")
        composeTestRule.onNodeWithTag("${NavigationTestTags.SEARCH_MERCHANT}_sabor-maianga").performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.MERCHANT_OVERVIEW).assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION).assertDoesNotExist()
        composeTestRule.onNodeWithTag(NavigationTestTags.MERCHANT_VIEW_CATALOG).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.CATALOG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION).assertDoesNotExist()
        composeTestRule.onNodeWithTag(NavigationTestTags.CATALOG_SEARCH).performTextInput("muamba")
        composeTestRule.onNodeWithTag(NavigationTestTags.CATALOG_LIST).performScrollToNode(
            hasTestTag("${NavigationTestTags.CATALOG_PRODUCT}_sabor-maianga-product-muamba-casa")
        )
        composeTestRule.onNodeWithTag(
            "${NavigationTestTags.CATALOG_PRODUCT}_sabor-maianga-product-muamba-casa"
        ).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.PRODUCT_DETAIL).assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION).assertDoesNotExist()
        composeTestRule.onNodeWithTag(NavigationTestTags.PRODUCT_LIST).performScrollToNode(
            hasTestTag(NavigationTestTags.PRODUCT_QUANTITY)
        )
        composeTestRule.onNodeWithContentDescription("Aumentar quantidade").performClick()
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.onNodeWithTag(NavigationTestTags.PRODUCT_QUANTITY)
            .assertContentDescriptionContains("Quantidade seleccionada: 2")
        composeTestRule.onNodeWithTag(NavigationTestTags.PRODUCT_ADD).performClick()
        composeTestRule.onNodeWithText("Produto configurado. O carrinho será ligado na próxima fase.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.CATALOG_SEARCH).assertTextContains("muamba")
        pressBack()
        composeTestRule.onNodeWithTag(NavigationTestTags.MERCHANT_OVERVIEW).assertIsDisplayed()
        pressBack()
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_FIELD).assertTextContains("Sabor")
    }

    private fun openSearch() {
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasTestTag(NavigationTestTags.BOTTOM_NAV_SEARCH)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAV_SEARCH).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH).assertIsDisplayed()
    }

    private fun pressBack() {
        composeTestRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeTestRule.waitForIdle()
    }
}
