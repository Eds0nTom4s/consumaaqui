package ao.consuma.aqui

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class NavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun app_navigates_from_splash_to_design_system_catalog() {
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(hasTestTag("design_system_catalog"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNode(hasTestTag("design_system_catalog")).assertExists()
    }
}
