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
        val route = AppDestination.catalog(id)
        val encoded = route.removePrefix("merchant/").removeSuffix("/catalog")
        assertEquals(id, Uri.decode(encoded))
        assertTrue(route.endsWith("/catalog"))
        assertFalse(route.contains("MerchantSummary"))
    }

    @Test fun product_route_encodes_merchant_and_product_ids_independently() {
        val merchantId = "merchant/maianga 01"
        val productId = "produto?especial#1/ação"
        val route = AppDestination.productDetail(merchantId, productId)
        val encodedMerchant = route.removePrefix("merchant/").substringBefore("/catalog/product/")
        val encodedProduct = route.substringAfter("/catalog/product/")
        assertEquals(merchantId, Uri.decode(encodedMerchant))
        assertEquals(productId, Uri.decode(encodedProduct))
        assertFalse(encodedMerchant.contains("/"))
        assertFalse(encodedProduct.contains("?"))
        assertFalse(encodedProduct.contains("#"))
    }

    @Test fun edit_product_route_encodes_optional_cart_item_id_independently() {
        val itemId = "item/with?reserved#chars"
        val route = AppDestination.productDetail("merchant", "product", itemId)
        val encodedItem = route.substringAfter("?cartItemId=")
        assertEquals(itemId, Uri.decode(encodedItem))
        assertFalse(encodedItem.contains("/"))
        assertFalse(encodedItem.contains("?"))
        assertFalse(encodedItem.contains("#"))
        assertFalse(AppDestination.productDetail("merchant", "product").contains("cartItemId"))
    }
}
