package ao.consuma.aqui.feature.checkout.presentation.mapper

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

@Composable
fun CheckoutUiText.resolve(): String = when (this) {
    is CheckoutUiText.Resource -> stringResource(id, *args.toTypedArray())
    is CheckoutUiText.Plural -> pluralStringResource(id, quantity, *args.toTypedArray())
    is CheckoutUiText.Dynamic -> value
}

fun CheckoutUiText.resolve(context: Context): String = when (this) {
    is CheckoutUiText.Resource -> context.getString(id, *args.toTypedArray())
    is CheckoutUiText.Plural -> context.resources.getQuantityString(
        id,
        quantity,
        *args.toTypedArray()
    )
    is CheckoutUiText.Dynamic -> value
}
