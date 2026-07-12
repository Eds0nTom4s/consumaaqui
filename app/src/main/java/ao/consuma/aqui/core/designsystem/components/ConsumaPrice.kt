package ao.consuma.aqui.core.designsystem.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import ao.consuma.aqui.R
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing

@Composable
fun ConsumaPriceText(
    price: String,
    modifier: Modifier = Modifier,
    oldPrice: String? = null,
    unit: String? = null
) {
    val spokenPrice = when {
        oldPrice != null -> stringResource(R.string.discount_price_accessibility, price, oldPrice)
        unit != null -> stringResource(R.string.price_with_unit_accessibility, price, unit)
        else -> stringResource(R.string.price_accessibility, price)
    }
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = spokenPrice },
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = price,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        if (unit != null) {
            Spacer(modifier = Modifier.width(ConsumaSpacing.xxs))
            Text(
                text = "/ $unit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (oldPrice != null) {
            Spacer(modifier = Modifier.width(ConsumaSpacing.sm))
            Text(
                text = oldPrice,
                style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConsumaPricePreview() {
    ConsumaAquiTheme {
        ConsumaPriceText(price = "5.500 Kz", oldPrice = "6.000 Kz")
    }
}
