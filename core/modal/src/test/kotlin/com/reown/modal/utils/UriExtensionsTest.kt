package com.reown.modal.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class UriExtensionsTest {
    private val wcUri = "wc:abc@2?relay-protocol=irn&symKey=123"
    private val encoded = "wc%3Aabc%402%3Frelay-protocol%3Dirn%26symKey%3D123"

    @Test
    fun `lowercases scheme and adds slash before wc path`() {
        assertEquals(
            "myapp://app.example.com/wc?uri=$encoded",
            formatNativeDeeplink("myApp://app.example.com", wcUri)
        )
    }

    @Test
    fun `does not add double slash for scheme only link`() {
        assertEquals("metamask://wc?uri=$encoded", formatNativeDeeplink("metamask://", wcUri))
    }

    @Test
    fun `bare scheme without separator gets canonical form`() {
        assertEquals("metamask://wc?uri=$encoded", formatNativeDeeplink("metamask", wcUri))
    }
}
