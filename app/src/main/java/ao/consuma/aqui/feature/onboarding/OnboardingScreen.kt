package ao.consuma.aqui.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ao.consuma.aqui.R
import ao.consuma.aqui.core.designsystem.components.ConsumaPrimaryButton
import ao.consuma.aqui.core.designsystem.components.ConsumaTextButton
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSize
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing
import ao.consuma.aqui.core.navigation.NavigationTestTags

private data class OnboardingPageData(
    @androidx.annotation.StringRes val title: Int,
    @androidx.annotation.StringRes val description: Int,
    val icon: ImageVector
)

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    OnboardingContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onComplete = onOnboardingComplete
    )
}

@Composable
private fun OnboardingContent(
    uiState: OnboardingUiState,
    onEvent: (OnboardingEvent) -> Unit,
    onComplete: () -> Unit
) {
    val pages = listOf(
        OnboardingPageData(
            title = R.string.onboarding_discover_title,
            description = R.string.onboarding_discover_description,
            icon = Icons.Default.Search
        ),
        OnboardingPageData(
            title = R.string.onboarding_choose_title,
            description = R.string.onboarding_choose_description,
            icon = Icons.Default.LocationOn
        ),
        OnboardingPageData(
            title = R.string.onboarding_track_title,
            description = R.string.onboarding_track_description,
            icon = Icons.Default.CheckCircle
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })

    LaunchedEffect(uiState.currentPage) {
        if (pagerState.currentPage != uiState.currentPage) {
            pagerState.animateScrollToPage(uiState.currentPage)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != uiState.currentPage) {
            onEvent(OnboardingEvent.PageChanged(pagerState.currentPage))
        }
    }

    Scaffold(
        modifier = Modifier.testTag(NavigationTestTags.ONBOARDING),
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = ConsumaSpacing.lg, end = ConsumaSpacing.lg),
                contentAlignment = Alignment.TopEnd
            ) {
                ConsumaTextButton(
                    text = stringResource(R.string.onboarding_skip),
                    onClick = {
                        onEvent(OnboardingEvent.Skip)
                        onComplete()
                    },
                    modifier = Modifier.testTag(NavigationTestTags.ONBOARDING_SKIP)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = ConsumaSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag(NavigationTestTags.ONBOARDING_PAGE),
                userScrollEnabled = true
            ) { pageIndex ->
                OnboardingPage(
                    page = pages[pageIndex],
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PageIndicator(
                    pageCount = pages.size,
                    currentPage = uiState.currentPage,
                    modifier = Modifier.testTag(NavigationTestTags.ONBOARDING_INDICATOR)
                )

                Spacer(modifier = Modifier.height(ConsumaSpacing.lg))

                if (uiState.isLastPage) {
                    ConsumaPrimaryButton(
                        text = stringResource(R.string.onboarding_start),
                        onClick = {
                            onEvent(OnboardingEvent.Finish)
                            onComplete()
                        },
                        fullWidth = true,
                        modifier = Modifier.testTag(NavigationTestTags.ONBOARDING_START)
                    )
                } else {
                    ConsumaPrimaryButton(
                        text = stringResource(R.string.onboarding_continue),
                        onClick = { onEvent(OnboardingEvent.Continue) },
                        fullWidth = true,
                        modifier = Modifier.testTag(NavigationTestTags.ONBOARDING_CONTINUE)
                    )
                }

                Spacer(modifier = Modifier.height(ConsumaSpacing.md))
            }
        }
    }
}

@Composable
private fun OnboardingPage(
    page: OnboardingPageData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = page.icon,
            contentDescription = null,
            modifier = Modifier.size(ConsumaSize.iconXLarge),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(ConsumaSpacing.xl))
        Text(
            text = stringResource(page.title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(ConsumaSpacing.md))
        Text(
            text = stringResource(page.description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            Box(
                modifier = Modifier
                    .size(if (isSelected) ConsumaSize.indicatorSelected else ConsumaSize.indicatorDefault)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
            if (index < pageCount - 1) {
                Spacer(modifier = Modifier.width(ConsumaSpacing.sm))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingFirstPagePreview() {
    ConsumaAquiTheme {
        OnboardingContent(
            uiState = OnboardingUiState(currentPage = 0),
            onEvent = {},
            onComplete = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingLastPagePreview() {
    ConsumaAquiTheme {
        OnboardingContent(
            uiState = OnboardingUiState(currentPage = 2),
            onEvent = {},
            onComplete = {}
        )
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OnboardingDarkPreview() {
    ConsumaAquiTheme {
        OnboardingContent(
            uiState = OnboardingUiState(currentPage = 0),
            onEvent = {},
            onComplete = {}
        )
    }
}
