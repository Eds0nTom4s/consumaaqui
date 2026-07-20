package ao.consuma.aqui.feature.checkout.domain.service

import java.util.UUID
import javax.inject.Inject

interface CheckoutIdGenerator {
    fun createId(): String
}

class UuidCheckoutIdGenerator @Inject constructor() : CheckoutIdGenerator {
    override fun createId(): String = UUID.randomUUID().toString()
}
