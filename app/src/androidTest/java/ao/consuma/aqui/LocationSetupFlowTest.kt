package ao.consuma.aqui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.Lifecycle
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
class LocationSetupFlowTest {

    @BindValue
    @JvmField
    var appLaunchStateRepository: AppLaunchStateRepository = InMemoryAppLaunchStateRepository()

    @get:Rule(order = 0)
    val launchStateRule = LaunchStateRule(
        setup = { },
        assign = { appLaunchStateRepository = it }
    )

    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun use_current_location_detects_and_confirms() {
        launchLocationSetup()

        composeTestRule.onNodeWithTag(NavigationTestTags.LOCATION_USE_CURRENT).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.location_found_title))
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(NavigationTestTags.LOCATION_CONFIRM).performClick()

        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodes(hasTestTag(NavigationTestTags.HOME))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.HOME).assertIsDisplayed()
    }

    @Test
    fun choose_manual_location_and_confirm() {
        launchLocationSetup()

        composeTestRule.onNodeWithTag(NavigationTestTags.LOCATION_CHOOSE_MANUALLY).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.LOCATION_LIST).assertIsDisplayed()

        composeTestRule.onAllNodes(hasTestTag(NavigationTestTags.LOCATION_LIST_ITEM))[1]
            .performClick()

        composeTestRule.onNodeWithTag(NavigationTestTags.LOCATION_CONFIRM).performClick()

        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodes(hasTestTag(NavigationTestTags.HOME))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.HOME).assertIsDisplayed()
    }

    @Test
    fun skip_location_enters_home_without_location() {
        launchLocationSetup()

        composeTestRule.onNodeWithTag(NavigationTestTags.LOCATION_SKIP).performClick()

        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodes(hasTestTag(NavigationTestTags.HOME))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.HOME).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.home_no_location_title))
            .assertIsDisplayed()
    }

    @Test
    fun back_from_manual_selection_returns_to_explanation() {
        launchLocationSetup()

        composeTestRule.onNodeWithTag(NavigationTestTags.LOCATION_CHOOSE_MANUALLY).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.LOCATION_LIST).assertIsDisplayed()

        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }

        composeTestRule.onNodeWithTag(NavigationTestTags.LOCATION_USE_CURRENT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.LOCATION_CHOOSE_MANUALLY).assertIsDisplayed()
    }

    @Test
    fun bottom_navigation_not_visible_during_location_setup() {
        launchLocationSetup()
        composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION).assertIsNotDisplayed()
    }

    @Test
    fun initial_setup_replaces_onboarding_stack_and_back_does_not_restore_setup() {
        launchLocationSetup()
        composeTestRule.onNodeWithTag(NavigationTestTags.LOCATION_SKIP).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.HOME).assertIsDisplayed()
        composeTestRule.onAllNodes(hasTestTag(NavigationTestTags.APP_SHELL)).assertCountEquals(1)

        pressBack()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.activityRule.scenario.state == Lifecycle.State.DESTROYED
        }
    }

    private fun launchLocationSetup() {
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodes(hasTestTag(NavigationTestTags.ONBOARDING))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.ONBOARDING_SKIP).performClick()
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodes(hasTestTag(NavigationTestTags.LOCATION_SETUP))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.LOCATION_SETUP).assertIsDisplayed()
    }

    private fun pressBack() {
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()
    }
}
