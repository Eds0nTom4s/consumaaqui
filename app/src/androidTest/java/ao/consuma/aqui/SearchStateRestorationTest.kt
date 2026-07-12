package ao.consuma.aqui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import ao.consuma.aqui.core.navigation.NavigationTestTags
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class SearchStateRestorationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
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

        composeTestRule.onNodeWithTag("search_field").performTextInput("café")

        composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAV_HOME).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.HOME).assertIsDisplayed()

        composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAV_SEARCH).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH).assertIsDisplayed()
    }
}
