package ao.consuma.aqui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import ao.consuma.aqui.core.navigation.NavigationTestTags
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class MoreDestinationsTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun more_to_settings_back_to_more() {
        navigateToMore()
        composeTestRule.onNodeWithText(context.getString(R.string.more_settings)).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.SETTINGS).assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION).assertIsNotDisplayed()

        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.MORE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION).assertIsDisplayed()
    }

    @Test
    fun more_to_help_back() {
        navigateToMore()
        composeTestRule.onNodeWithText(context.getString(R.string.more_help)).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.HELP).assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION).assertIsNotDisplayed()

        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.MORE).assertIsDisplayed()
    }

    @Test
    fun more_to_about_back() {
        navigateToMore()
        composeTestRule.onNodeWithText(context.getString(R.string.more_about)).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.ABOUT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION).assertIsNotDisplayed()

        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.MORE).assertIsDisplayed()
    }

    private fun navigateToMore() {
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodes(hasTestTag(NavigationTestTags.HOME))
                .fetchSemanticsNodes().isNotEmpty()
                    && composeTestRule.onAllNodes(hasTestTag(NavigationTestTags.BOTTOM_NAVIGATION))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAV_MORE).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.MORE).assertIsDisplayed()
    }
}
