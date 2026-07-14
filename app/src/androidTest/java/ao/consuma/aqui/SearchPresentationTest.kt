package ao.consuma.aqui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.core.navigation.NavigationTestTags
import ao.consuma.aqui.feature.discovery.domain.model.DiscoveryOrderBy
import ao.consuma.aqui.feature.discovery.presentation.mapper.*
import ao.consuma.aqui.feature.search.*
import org.junit.Rule
import org.junit.Test

class SearchPresentationTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test fun search_empty_at_one_point_three_font_scale_is_scrollable_and_accessible_in_dark_theme() {
        val criteria = SearchCriteriaUiState(
            query = "inexistente",
            categories = listOf(CategoryUiModel(null, UiText.Resource(R.string.search_filter_all))),
            sortOptions = DiscoveryOrderBy.entries.map {
                SearchSortOptionUiModel(it, UiText.Dynamic(it.name), it != DiscoveryOrderBy.NEAREST)
            },
            hasLocation = false
        )
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1.3f)) {
                ConsumaAquiTheme(darkTheme = true) {
                    SearchScreen(
                        SearchUiState.Empty(criteria, UiText.Resource(R.string.search_empty_query_description)),
                        {}
                    )
                }
            }
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_LIST).performScrollToIndex(3)
        composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_EMPTY).assertIsDisplayed()
    }
}
