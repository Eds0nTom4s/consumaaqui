package ao.consuma.aqui.core.designsystem.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing

enum class ConsumaStatusSemantic {
    NEUTRAL, SUCCESS, WARNING, ERROR, INFO
}

@Composable
fun ConsumaFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.small
    )
}

@Composable
fun ConsumaStatusChip(
    text: String,
    semantic: ConsumaStatusSemantic,
    modifier: Modifier = Modifier
) {
    val (containerColor, contentColor) = when (semantic) {
        ConsumaStatusSemantic.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        ConsumaStatusSemantic.SUCCESS -> ConsumaAquiTheme.semanticColors.success to ConsumaAquiTheme.semanticColors.onSuccess
        ConsumaStatusSemantic.WARNING -> ConsumaAquiTheme.semanticColors.warning to ConsumaAquiTheme.semanticColors.onWarning
        ConsumaStatusSemantic.ERROR -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
        ConsumaStatusSemantic.INFO -> ConsumaAquiTheme.semanticColors.info to ConsumaAquiTheme.semanticColors.onInfo
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = ConsumaSpacing.sm, vertical = ConsumaSpacing.xs)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConsumaChipPreview() {
    ConsumaAquiTheme {
        ConsumaStatusChip(text = "ABERTO", semantic = ConsumaStatusSemantic.SUCCESS)
    }
}
