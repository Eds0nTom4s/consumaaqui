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
import ao.consuma.aqui.feature.launch.di.LaunchStateModule
import ao.consuma.aqui.feature.launch.domain.AppLaunchStateRepository
import ao.consuma.aqui.feature.launch.domain.InMemoryAppLaunchStateRepository
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
@UninstallModules(LaunchStateModule::class)
class OnboardingFlowTest {

    @BindValue
    @JvmField
    var appLaunchStateRepository: AppLaunchStateRepository = InMemoryAppLaunchStateRepository()

    @get:Rule(order = 0)
    val launchStateRule = LaunchStateRule(
        setup = { /* onboarding incompleto por defeito */ },
        assign = { appLaunchStateRepository = it }
    )

    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun first_opening_shows_onboarding() {
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodes(hasTestTag(NavigationTestTags.ONBOARDING))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.ONBOARDING).assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION).assertIsNotDisplayed()
    }

    @Test
    fun onboarding_advances_through_three_pages() {
        waitForOnboarding()

        composeTestRule.onNodeWithText(context.getString(R.string.onboarding_discover_title))
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(NavigationTestTags.ONBOARDING_CONTINUE).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.onboarding_choose_title))
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(NavigationTestTags.ONBOARDING_CONTINUE).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.onboarding_track_title))
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(NavigationTestTags.ONBOARDING_START).assertIsDisplayed()
    }

    @Test
    fun skip_onboarding_navigates_to_location_setup() {
        waitForOnboarding()

        composeTestRule.onNodeWithTag(NavigationTestTags.ONBOARDING_SKIP).performClick()

        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodes(hasTestTag(NavigationTestTags.LOCATION_SETUP))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.LOCATION_SETUP).assertIsDisplayed()
    }

    private fun waitForOnboarding() {
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodes(hasTestTag(NavigationTestTags.ONBOARDING))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
