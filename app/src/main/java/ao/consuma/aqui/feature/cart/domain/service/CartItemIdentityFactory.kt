package ao.consuma.aqui.feature.cart.domain.service

import ao.consuma.aqui.feature.cart.domain.model.CartItemSelection
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

class CartItemIdentityFactory @Inject constructor() {
    fun createCartId(): String = UUID.randomUUID().toString()

    fun createItemId(): String = UUID.randomUUID().toString()

    fun configurationFingerprint(
        merchantId: String,
        productId: String,
        selections: List<CartItemSelection>,
        note: String?
    ): String {
        require(merchantId.isNotBlank()) { "Merchant id is required" }
        require(productId.isNotBlank()) { "Product id is required" }
        val canonical = buildString {
            appendSegment(merchantId)
            appendSegment(productId)
            selections.sortedWith(compareBy(CartItemSelection::groupId, CartItemSelection::optionId))
                .forEach {
                    appendSegment(it.groupId)
                    appendSegment(it.optionId)
                }
            appendSegment(note.normalizedNote().orEmpty())
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    private fun StringBuilder.appendSegment(value: String) {
        append(value.length).append(':').append(value).append('|')
    }
}

internal fun String?.normalizedNote(): String? = this?.trim()?.takeIf(String::isNotEmpty)
