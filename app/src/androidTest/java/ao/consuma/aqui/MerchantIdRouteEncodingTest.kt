package ao.consuma.aqui

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import ao.consuma.aqui.core.navigation.AppDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MerchantIdRouteEncodingTest {
    @Test fun reserved_merchant_ids_are_encoded_and_recovered_from_overview_route() {
        listOf("merchant/maianga 01", "merchant?ref=abc", "merchant#special").forEach { id ->
            val route = AppDestination.merchantOverview(id)
            val encoded = route.removePrefix("merchant/")
            assertFalse(encoded.contains("/"))
            assertFalse(encoded.contains("?"))
            assertFalse(encoded.contains("#"))
            assertEquals(id, Uri.decode(encoded))
        }
    }

    @Test fun catalog_route_encodes_only_id_and_serializes_no_merchant_object() {
        val id = "merchant/maianga 01"
        val route = AppDestination.catalogPlaceholder(id)
        val encoded = route.removePrefix("merchant/").removeSuffix("/catalog")
        assertEquals(id, Uri.decode(encoded))
        assertTrue(route.endsWith("/catalog"))
        assertFalse(route.contains("MerchantSummary"))
    }
}
