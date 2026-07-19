package ao.consuma.aqui.feature.cart.presentation.mapper

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

@Immutable
sealed interface CartUiText {
    data class Resource(@StringRes val id: Int, val args: List<Any> = emptyList()) : CartUiText
    data class Plural(
        @PluralsRes val id: Int,
        val quantity: Int,
        val args: List<Any> = listOf(quantity)
    ) : CartUiText
    data class Dynamic(val value: String) : CartUiText
}

@Immutable
data class CartMerchantUiModel(val id: String, val name: String)

@Immutable
data class CartItemUiModel(
    val id: String,
    val merchantId: String,
    val productId: String,
    val productName: String,
    val hasImage: Boolean,
    val imageUrl: String?,
    val configurationText: List<String>,
    val noteText: String?,
    val quantity: Int,
    val unitPriceText: String,
    val totalPriceText: String,
    val canDecrease: Boolean,
    val canIncrease: Boolean,
    val accessibilityDescription: CartUiText
)

@Immutable
data class CartBadgeUiState(
    val itemCount: Int = 0,
    val displayText: String? = null,
    val visible: Boolean = false,
    val accessibilityDescription: CartUiText
)
