package ao.consuma.aqui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import ao.consuma.aqui.core.navigation.NavigationTestTags
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class AppNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun splash_initialization_navigates_to_home() {
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodes(hasTestTag(NavigationTestTags.HOME))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.HOME).assertIsDisplayed()
    }

    @Test
    fun bottom_navigation_displays_four_destinations() {
        waitForHome()
        composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION).assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAV_HOME).assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAV_SEARCH).assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAV_ORDERS).assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAV_MORE).assertIsDisplayed()
    }

    @Test
    fun navigate_home_to_search_to_orders_to_more() {
        waitForHome()
        clickBottomNav(NavigationTestTags.BOTTOM_NAV_SEARCH)
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH).assertIsDisplayed()
        clickBottomNav(NavigationTestTags.BOTTOM_NAV_ORDERS)
        composeTestRule.onNodeWithTag(NavigationTestTags.ORDERS).assertIsDisplayed()
        clickBottomNav(NavigationTestTags.BOTTOM_NAV_MORE)
        composeTestRule.onNodeWithTag(NavigationTestTags.MORE).assertIsDisplayed()
    }

    @Test
    fun back_navigation_between_top_level_destinations_is_predictable() {
        waitForHome()
        clickBottomNav(NavigationTestTags.BOTTOM_NAV_SEARCH)
        clickBottomNav(NavigationTestTags.BOTTOM_NAV_ORDERS)
        composeTestRule.onNodeWithTag(NavigationTestTags.ORDERS).assertIsDisplayed()

        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(hasTestTag(NavigationTestTags.SEARCH))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH).assertIsDisplayed()

        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(hasTestTag(NavigationTestTags.HOME))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.HOME).assertIsDisplayed()
    }

    @Test
    fun tapping_same_destination_twice_does_not_duplicate() {
        waitForHome()
        clickBottomNav(NavigationTestTags.BOTTOM_NAV_SEARCH)
        clickBottomNav(NavigationTestTags.BOTTOM_NAV_SEARCH)
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH).assertIsDisplayed()
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.HOME).assertIsDisplayed()
    }

    @Test
    fun rotation_does_not_crash() {
        waitForHome()
        composeTestRule.onNodeWithTag(NavigationTestTags.HOME).assertIsDisplayed()
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.onNodeWithTag(NavigationTestTags.HOME).assertIsDisplayed()
    }

    private fun waitForHome() {
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodes(hasTestTag(NavigationTestTags.HOME))
                .fetchSemanticsNodes().isNotEmpty()
                    && composeTestRule.onAllNodes(hasTestTag(NavigationTestTags.BOTTOM_NAVIGATION))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun clickBottomNav(testTag: String) {
        composeTestRule.onNodeWithTag(testTag).performClick()
    }
}
