package ao.consuma.aqui.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class UiMessageTest {
    @Test
    fun `UiMessage Resource holds correct id`() {
        val msg = UiMessage.Resource(123)
        assertEquals(123, msg.resId)
    }

    @Test
    fun `UiMessage Dynamic holds correct string`() {
        val msg = UiMessage.Dynamic("Error")
        assertEquals("Error", msg.value)
    }
}
