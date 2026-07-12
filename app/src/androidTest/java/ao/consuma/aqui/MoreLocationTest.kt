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
import ao.consuma.aqui.feature.launch.domain.MockLocation
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
@UninstallModules(LaunchStateModule::class)
class MoreLocationTest {

    @BindValue
    @JvmField
    var appLaunchStateRepository: AppLaunchStateRepository = InMemoryAppLaunchStateRepository()

    @get:Rule(order = 0)
    val launchStateRule = LaunchStateRule(
        setup = {
            completeOnboarding()
            selectLocation(
                MockLocation(
                    id = "maianga",
                    city = "Luanda",
                    area = "Maianga",
                    displayName = "Luanda — Maianga"
                )
            )
        },
        assign = { appLaunchStateRepository = it }
    )

    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun more_to_location_settings_change_location() {
        navigateToMore()
        composeTestRule.onNodeWithTag(NavigationTestTags.MORE_LOCATION).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.LOCATION_SETTINGS).assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION).assertIsNotDisplayed()

        composeTestRule.onNodeWithTag(NavigationTestTags.LOCATION_SETTINGS_CHANGE).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.LOCATION_SETUP).assertIsDisplayed()

        composeTestRule.onNodeWithTag(NavigationTestTags.LOCATION_CHOOSE_MANUALLY).performClick()
        composeTestRule.onAllNodes(hasTestTag(NavigationTestTags.LOCATION_LIST_ITEM))[2]
            .performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.LOCATION_CONFIRM).performClick()

        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodes(hasTestTag(NavigationTestTags.HOME))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.HOME).assertIsDisplayed()
    }

    @Test
    fun more_to_location_settings_remove_location() {
        navigateToMore()
        composeTestRule.onNodeWithTag(NavigationTestTags.MORE_LOCATION).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.LOCATION_SETTINGS).assertIsDisplayed()

        composeTestRule.onNodeWithTag(NavigationTestTags.LOCATION_SETTINGS_REMOVE).performClick()

        composeTestRule.onNodeWithText(context.getString(R.string.location_not_configured_title))
            .assertIsDisplayed()
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
