package ao.consuma.aqui.feature.cart.presentation.mapper

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

@Composable
fun CartUiText.resolve(): String = when (this) {
    is CartUiText.Resource -> stringResource(id, *args.toTypedArray())
    is CartUiText.Plural -> pluralStringResource(id, quantity, *args.toTypedArray())
    is CartUiText.Dynamic -> value
}
