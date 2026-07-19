package ao.consuma.aqui.feature.cart.presentation.mapper

import ao.consuma.aqui.R
import ao.consuma.aqui.core.domain.model.MoneyAmount
import ao.consuma.aqui.feature.cart.domain.model.Cart
import ao.consuma.aqui.feature.cart.domain.model.CartItem
import javax.inject.Inject

class CartUiMapper @Inject constructor() {
    fun merchant(cart: Cart): CartMerchantUiModel? = cart.merchant?.let {
        CartMerchantUiModel(it.id, it.name)
    }

    fun items(cart: Cart): List<CartItemUiModel> = cart.items.map(::item)

    fun item(value: CartItem) = CartItemUiModel(
        id = value.id,
        merchantId = value.merchantId,
        productId = value.productId,
        productName = value.productName,
        hasImage = value.productImageUrl != null,
        imageUrl = value.productImageUrl,
        configurationText = value.selections.groupBy { it.groupName }.map { (group, options) ->
            "$group: ${options.joinToString { it.optionName }}"
        },
        noteText = value.note,
        quantity = value.quantity,
        unitPriceText = money(value.totalUnitPrice),
        totalPriceText = money(value.totalPrice),
        canDecrease = value.quantity > CartItem.MIN_QUANTITY,
        canIncrease = value.quantity < CartItem.MAX_QUANTITY,
        accessibilityDescription = CartUiText.Resource(
            R.string.cart_item_accessibility,
            listOf(value.productName, value.quantity, money(value.totalUnitPrice), money(value.totalPrice))
        )
    )

    fun subtotal(cart: Cart): String = money(cart.totals.subtotal)

    fun badge(itemCount: Int) = CartBadgeUiState(
        itemCount = itemCount,
        displayText = when {
            itemCount == 0 -> null
            itemCount > CartItem.MAX_QUANTITY -> "${CartItem.MAX_QUANTITY}+"
            else -> itemCount.toString()
        },
        visible = itemCount > 0,
        accessibilityDescription = when {
            itemCount == 0 -> CartUiText.Resource(R.string.cart_badge_empty_accessibility)
            itemCount > CartItem.MAX_QUANTITY ->
                CartUiText.Resource(R.string.cart_badge_overflow_accessibility)
            else -> CartUiText.Plural(R.plurals.cart_badge_accessibility, itemCount)
        }
    )

    fun money(value: MoneyAmount): String {
        val whole = value.amountMinor / 100
        val fraction = value.amountMinor % 100
        val grouped = whole.toString().reversed().chunked(3).joinToString(".").reversed()
        val amount = if (fraction == 0L) grouped else "$grouped,${fraction.toString().padStart(2, '0')}"
        return "$amount ${if (value.currencyCode == "AOA") "Kz" else value.currencyCode}"
    }
}
