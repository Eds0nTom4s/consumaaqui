package ao.consuma.aqui.core.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyAmountTest {
    @Test fun `zero and positive minor amounts are valid`() {
        MoneyAmount(0, "AOA")
        MoneyAmount(100, "USD")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative money is rejected`() {
        MoneyAmount(-1, "AOA")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `currency must be an uppercase three-letter code`() {
        MoneyAmount(10, "aoa")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `empty currency is rejected`() {
        MoneyAmount(10, "")
    }

    @Test fun `currency comparison is explicit`() {
        val kwanza = MoneyAmount(100, "AOA")
        assertTrue(kwanza.hasSameCurrency(MoneyAmount(200, "AOA")))
        assertFalse(kwanza.hasSameCurrency(MoneyAmount(100, "USD")))
    }

    @Test fun `same currency amounts add without mutating operands`() {
        val first = MoneyAmount(100, "AOA")
        val second = MoneyAmount(50, "AOA")
        val result = first.add(second)
        assertEquals(MoneyAmount(150, "AOA"), result)
        assertEquals(100, first.amountMinor)
        assertNotSame(first, result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `addition rejects different currencies`() {
        MoneyAmount(100, "AOA").add(MoneyAmount(50, "USD"))
    }

    @Test fun `multiplication uses a positive quantity`() {
        assertEquals(MoneyAmount(300, "AOA"), MoneyAmount(100, "AOA").multiply(3))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `multiplication rejects zero quantity`() {
        MoneyAmount(100, "AOA").multiply(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `multiplication rejects negative quantity`() {
        MoneyAmount(100, "AOA").multiply(-1)
    }

    @Test(expected = ArithmeticException::class)
    fun `addition overflow is not silently wrapped`() {
        MoneyAmount(Long.MAX_VALUE, "AOA").add(MoneyAmount(1, "AOA"))
    }

    @Test(expected = ArithmeticException::class)
    fun `multiplication overflow is not silently wrapped`() {
        MoneyAmount(Long.MAX_VALUE, "AOA").multiply(2)
    }

    @Test fun `data equality includes amount and strict currency contract`() {
        assertEquals(MoneyAmount(100, "AOA"), MoneyAmount(100, "AOA"))
    }
}
