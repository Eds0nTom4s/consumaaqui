package ao.consuma.aqui.feature.cart

import ao.consuma.aqui.core.domain.model.MoneyAmount
import ao.consuma.aqui.feature.cart.domain.command.AddCartItemCommand
import ao.consuma.aqui.feature.cart.domain.command.CartOptionSnapshot
import ao.consuma.aqui.feature.cart.domain.command.ProductCartSnapshot
import ao.consuma.aqui.feature.cart.domain.model.CartItem
import ao.consuma.aqui.feature.cart.domain.model.CartItemSelection
import ao.consuma.aqui.feature.cart.domain.model.CartMerchant
import ao.consuma.aqui.feature.catalog.domain.model.ConfiguredProduct

internal fun merchant(id: String = "merchant-one", name: String = "Merchant One") =
    CartMerchant(id, name)

internal fun addCommand(
    merchantId: String = "merchant-one",
    merchantName: String = "Merchant One",
    productId: String = "product-one",
    productName: String = "Product One",
    optionId: String = "large",
    optionName: String = "Large",
    note: String? = null,
    quantity: Int = 1,
    currency: String = "AOA",
    unitMinor: Long = 1_000,
    optionMinor: Long = 200
): AddCartItemCommand {
    val unit = MoneyAmount(unitMinor, currency)
    val option = MoneyAmount(optionMinor, currency)
    val totalUnit = unit.add(option)
    return AddCartItemCommand(
        merchant = CartMerchant(merchantId, merchantName),
        configuredProduct = ConfiguredProduct(
            merchantId = merchantId,
            productId = productId,
            quantity = quantity,
            selectedOptionIds = mapOf("size" to setOf(optionId)),
            note = note,
            unitPrice = unit,
            optionsPrice = option,
            totalUnitPrice = totalUnit,
            totalPrice = totalUnit.multiply(quantity)
        ),
        snapshot = ProductCartSnapshot(
            productName = productName,
            imageUrl = "https://images.invalid/$productId.jpg",
            selectedOptions = listOf(
                CartOptionSnapshot("size", "Size", optionId, optionName, option)
            )
        )
    )
}

internal fun cartItem(
    id: String = "item-one",
    merchantId: String = "merchant-one",
    productId: String = "product-one",
    quantity: Int = 1,
    note: String? = null,
    currency: String = "AOA",
    fingerprint: String = "$merchantId-$productId-${note.orEmpty()}"
): CartItem {
    val unit = MoneyAmount(1_000, currency)
    val option = MoneyAmount(200, currency)
    val totalUnit = unit.add(option)
    return CartItem(
        id = id,
        merchantId = merchantId,
        productId = productId,
        productName = "Product One",
        productImageUrl = null,
        quantity = quantity,
        note = note,
        selections = listOf(
            CartItemSelection("size", "Size", "large", "Large", option)
        ),
        unitPrice = unit,
        optionsPrice = option,
        totalUnitPrice = totalUnit,
        totalPrice = totalUnit.multiply(quantity),
        configurationFingerprint = fingerprint
    )
}
