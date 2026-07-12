package ao.consuma.aqui.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing

@Composable
fun ConsumaLoadingIndicator(modifier: Modifier = Modifier) {
    CircularProgressIndicator(modifier = modifier, color = MaterialTheme.colorScheme.primary)
}

@Composable
fun ConsumaLoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ConsumaLoadingIndicator()
    }
}

@Composable
fun ConsumaEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxSize().padding(ConsumaSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        if (description != null) {
            Spacer(modifier = Modifier.height(ConsumaSpacing.sm))
            Text(text = description, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(ConsumaSpacing.lg))
            ConsumaPrimaryButton(text = actionText, onClick = onActionClick)
        }
    }
}

@Composable
fun ConsumaErrorState(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(ConsumaSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(ConsumaSpacing.lg))
            ConsumaPrimaryButton(
                text = stringResource(id = ao.consuma.aqui.R.string.try_again), 
                onClick = onRetry
            )
        }
    }
}

@Composable
fun ConsumaOfflineBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(ConsumaSpacing.sm),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(id = ao.consuma.aqui.R.string.offline_banner_text),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun ConsumaInlineMessage(
    message: String,
    semantic: ConsumaStatusSemantic,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (semantic) {
        ConsumaStatusSemantic.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        ConsumaStatusSemantic.SUCCESS -> ConsumaAquiTheme.semanticColors.success to ConsumaAquiTheme.semanticColors.onSuccess
        ConsumaStatusSemantic.WARNING -> ConsumaAquiTheme.semanticColors.warning to ConsumaAquiTheme.semanticColors.onWarning
        ConsumaStatusSemantic.ERROR -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        ConsumaStatusSemantic.INFO -> ConsumaAquiTheme.semanticColors.info to ConsumaAquiTheme.semanticColors.onInfo
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor, MaterialTheme.shapes.small)
            .padding(ConsumaSpacing.md)
    ) {
        Text(text = message, color = textColor, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(showBackground = true)
@Composable
private fun ConsumaFeedbackPreview() {
    ConsumaAquiTheme {
        ConsumaErrorState(message = "Something went wrong", onRetry = {})
    }
}
