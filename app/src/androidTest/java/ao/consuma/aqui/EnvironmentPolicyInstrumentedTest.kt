package ao.consuma.aqui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import ao.consuma.aqui.BuildConfig
import ao.consuma.aqui.core.navigation.NavigationTestTags
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class EnvironmentPolicyInstrumentedTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun debug_and_staging_show_design_system_entry() {
        assumeTrue(BuildConfig.ENVIRONMENT != "RELEASE")
        navigateToMore()
        composeTestRule.onNodeWithText(context.getString(R.string.more_design_system)).assertIsDisplayed()
    }

    @Test
    fun debug_and_staging_can_navigate_to_design_system() {
        assumeTrue(BuildConfig.ENVIRONMENT != "RELEASE")
        navigateToMore()
        composeTestRule.onNodeWithText(context.getString(R.string.more_design_system)).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.DESIGN_SYSTEM_CATALOG).assertIsDisplayed()
    }

    @Test
    fun release_does_not_show_design_system_entry() {
        assumeTrue(BuildConfig.ENVIRONMENT == "RELEASE")
        navigateToMore()
        composeTestRule.onNodeWithText(context.getString(R.string.more_developer_tools_title)).assertDoesNotExist()
        composeTestRule.onNodeWithText(context.getString(R.string.more_design_system)).assertDoesNotExist()
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
