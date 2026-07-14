package ao.consuma.aqui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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
class SearchStateRestorationTest {

    @BindValue
    @JvmField
    var appLaunchStateRepository: AppLaunchStateRepository = InMemoryAppLaunchStateRepository()

    @get:Rule(order = 0)
    val launchStateRule = LaunchStateRule(
        setup = { completeOnboarding() },
        assign = { appLaunchStateRepository = it }
    )

    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun search_query_is_restored_after_switching_tabs() {
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodes(hasTestTag(NavigationTestTags.HOME))
                .fetchSemanticsNodes().isNotEmpty()
                    && composeTestRule.onAllNodes(hasTestTag(NavigationTestTags.BOTTOM_NAVIGATION))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAV_SEARCH).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH).assertIsDisplayed()

        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_FIELD).performTextInput("café")

        composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAV_HOME).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.HOME).assertIsDisplayed()

        composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAV_SEARCH).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH).assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_FIELD).assertTextContains("café")
    }
}
