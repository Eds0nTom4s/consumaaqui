package ao.consuma.aqui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.core.navigation.NavigationTestTags
import ao.consuma.aqui.feature.home.presentation.CategoryUiModel
import ao.consuma.aqui.feature.home.presentation.HomeScreen
import ao.consuma.aqui.feature.home.presentation.HomeUiState
import ao.consuma.aqui.feature.home.presentation.UiText
import org.junit.Rule
import org.junit.Test

class HomeDiscoveryPresentationTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test fun loading_composes_without_crash() {
        composeTestRule.setContent { HomeScreen(HomeUiState.Loading, {}) }
        composeTestRule.onNodeWithTag(NavigationTestTags.HOME_LOADING).assertIsDisplayed()
    }
    @Test fun empty_composes_with_action() {
        composeTestRule.setContent { HomeScreen(HomeUiState.Empty(null, listOf(CategoryUiModel(null, "Todos")), "", null), {}) }
        composeTestRule.onNodeWithTag(NavigationTestTags.HOME_EMPTY).assertIsDisplayed()
    }
    @Test fun error_composes_with_retry() {
        composeTestRule.setContent { HomeScreen(HomeUiState.Error(UiText.Resource(R.string.home_error_generic), true), {}) }
        composeTestRule.onNodeWithTag(NavigationTestTags.HOME_ERROR).assertIsDisplayed()
    }
    @Test fun dark_theme_composes_without_crash() {
        composeTestRule.setContent { ConsumaAquiTheme(darkTheme = true) { HomeScreen(HomeUiState.Loading, {}) } }
        composeTestRule.onNodeWithTag(NavigationTestTags.HOME_LOADING).assertIsDisplayed()
    }
    @Test fun increased_font_scale_composes_without_critical_crash() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                HomeScreen(HomeUiState.Empty(null, listOf(CategoryUiModel(null, "Todos")), "", null), {})
            }
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.HOME_EMPTY).assertIsDisplayed()
    }
}
