package ao.consuma.aqui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import ao.consuma.aqui.core.navigation.NavigationTestTags
import org.junit.Rule
import org.junit.Test

class SearchStateRestorationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun search_query_is_restored_after_switching_tabs() {
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(hasTestTag(NavigationTestTags.HOME))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH).assertIsDisplayed()

        composeTestRule.onNodeWithTag("search_field").performTextInput("café")

        composeTestRule.onNodeWithTag(NavigationTestTags.HOME).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.HOME).assertIsDisplayed()

        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH).assertIsDisplayed()
    }
}
