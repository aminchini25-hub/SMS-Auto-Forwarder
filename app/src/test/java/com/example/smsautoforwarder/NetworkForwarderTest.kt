package com.example.smsautoforwarder

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkForwarderTest {

    @Test
    fun `webhook accepts only https URLs`() {
        assertTrue(NetworkForwarder.isValidHttpsUrl("https://example.com/hook"))
        assertFalse(NetworkForwarder.isValidHttpsUrl("http://example.com/hook"))
        assertFalse(NetworkForwarder.isValidHttpsUrl("not a URL"))
    }
}