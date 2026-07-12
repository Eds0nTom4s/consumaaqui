package ao.consuma.aqui.core.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.core.designsystem.tokens.ConsumaElevation
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing

@Composable
fun ConsumaCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = ConsumaElevation.low),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        content = content
    )
}

@Composable
fun ConsumaClickableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = ConsumaElevation.low),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        content = content
    )
}

@Composable
fun ConsumaMerchantCardPlaceholder(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    ConsumaCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(ConsumaSpacing.md)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(ConsumaSpacing.xs))
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (actionText != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(ConsumaSpacing.sm))
                ConsumaTextButton(text = actionText, onClick = onActionClick)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConsumaCardPreview() {
    ConsumaAquiTheme {
        ConsumaMerchantCardPlaceholder(title = "Restaurante", subtitle = "Angola")
    }
}
