package ao.consuma.aqui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import ao.consuma.aqui.core.navigation.NavigationTestTags
import ao.consuma.aqui.feature.home.data.HomeDiscoveryDataModule
import ao.consuma.aqui.feature.home.data.InMemoryHomeDiscoveryRepository
import ao.consuma.aqui.feature.home.data.MockHomeScenario
import ao.consuma.aqui.feature.home.domain.repository.HomeDiscoveryRepository
import ao.consuma.aqui.feature.launch.di.LaunchStateModule
import ao.consuma.aqui.feature.launch.domain.AppLaunchStateRepository
import ao.consuma.aqui.feature.launch.domain.InMemoryAppLaunchStateRepository
import ao.consuma.aqui.feature.launch.domain.MockLocation
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
@UninstallModules(LaunchStateModule::class, HomeDiscoveryDataModule::class)
class HomeDiscoveryTest {
    private val homeRepository = InMemoryHomeDiscoveryRepository()

    @BindValue @JvmField
    var appLaunchStateRepository: AppLaunchStateRepository = InMemoryAppLaunchStateRepository()

    @BindValue @JvmField
    var homeDiscoveryRepository: HomeDiscoveryRepository = homeRepository

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

    @Test fun home_opens_with_discovery_after_completed_flow() { waitForHome(); composeTestRule.onNodeWithText("Sabor da Maianga").assertIsDisplayed() }
    @Test fun current_location_is_displayed() { waitForHome(); composeTestRule.onNodeWithText("Luanda — Maianga").assertIsDisplayed() }
    @Test fun search_filters_and_clear_restores_merchants() {
        waitForHome()
        composeTestRule.onNodeWithTag(NavigationTestTags.HOME_SEARCH).performTextInput("Horizonte")
        composeTestRule.onNodeWithText("Café Horizonte").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sabor da Maianga").assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.HOME_SEARCH).performTextClearance()
        composeTestRule.onNodeWithText("Sabor da Maianga").assertIsDisplayed()
    }
    @Test fun category_filters_and_all_restores() {
        waitForHome()
        composeTestRule.onNodeWithTag("${NavigationTestTags.HOME_CATEGORY}_bakery").performClick()
        composeTestRule.onNodeWithText("Doce Embondeiro").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sabor da Maianga").assertIsNotDisplayed()
        composeTestRule.onNodeWithTag("${NavigationTestTags.HOME_CATEGORY}_all").performClick()
        composeTestRule.onNodeWithText("Sabor da Maianga").assertIsDisplayed()
    }
    @Test fun merchant_opens_placeholder_and_back_returns_home() {
        waitForHome()
        composeTestRule.onAllNodes(hasTestTag("${NavigationTestTags.HOME_MERCHANT}_sabor-maianga"))[0].performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.MERCHANT_PLACEHOLDER).assertIsDisplayed()
        pressBack()
        composeTestRule.onNodeWithTag(NavigationTestTags.HOME).assertIsDisplayed()
    }
    @Test fun empty_scenario_presents_action() {
        waitForHome(); homeRepository.scenario = MockHomeScenario.EMPTY
        composeTestRule.onNodeWithTag(NavigationTestTags.HOME_REFRESH).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.HOME_EMPTY).assertIsDisplayed()
    }
    @Test fun error_scenario_retries() {
        waitForHome(); homeRepository.scenario = MockHomeScenario.ERROR
        composeTestRule.onNodeWithTag(NavigationTestTags.HOME_REFRESH).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.HOME_ERROR).assertIsDisplayed()
        homeRepository.scenario = MockHomeScenario.CONTENT
        composeTestRule.onNodeWithText("Tentar Novamente").performClick()
        composeTestRule.onNodeWithText("Sabor da Maianga").assertIsDisplayed()
    }
    @Test fun offline_banner_keeps_content_accessible() {
        waitForHome(); homeRepository.scenario = MockHomeScenario.OFFLINE
        composeTestRule.onNodeWithTag(NavigationTestTags.HOME_REFRESH).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.HOME_OFFLINE).assertIsDisplayed()
        composeTestRule.onNodeWithText("Sabor da Maianga").assertIsDisplayed()
    }
    @Test fun refresh_does_not_duplicate_nearby_item() {
        waitForHome(); composeTestRule.onNodeWithTag(NavigationTestTags.HOME_REFRESH).performClick()
        val cards = composeTestRule.onAllNodes(hasTestTag("${NavigationTestTags.HOME_MERCHANT}_sabor-maianga")).fetchSemanticsNodes()
        org.junit.Assert.assertEquals(1, cards.size)
    }
    @Test fun rotation_preserves_query() {
        waitForHome(); composeTestRule.onNodeWithTag(NavigationTestTags.HOME_SEARCH).performTextInput("Horizonte")
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.onNodeWithText("Café Horizonte").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sabor da Maianga").assertIsNotDisplayed()
    }
    @Test fun switching_tab_preserves_query() {
        waitForHome(); composeTestRule.onNodeWithTag(NavigationTestTags.HOME_SEARCH).performTextInput("Horizonte")
        composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAV_MORE).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAV_HOME).performClick()
        composeTestRule.onNodeWithText("Café Horizonte").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sabor da Maianga").assertIsNotDisplayed()
    }

    private fun waitForHome() {
        composeTestRule.waitUntil(10000) { composeTestRule.onAllNodes(hasTestTag(NavigationTestTags.HOME_SEARCH)).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun pressBack() { composeTestRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; composeTestRule.waitForIdle() }
}
