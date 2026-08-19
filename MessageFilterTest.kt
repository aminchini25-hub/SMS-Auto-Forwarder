package com.example.smsautoforwarder

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageFilterTest {

    @Test
    fun `empty keywords matches everything`() {
        assertTrue(MessageFilter.matches("any body", "", excludeMode = false))
        assertTrue(MessageFilter.matches("any body", "", excludeMode = true))
    }

    @Test
    fun `include mode requires a keyword match`() {
        assertTrue(MessageFilter.matches("Your OTP code is 1234", "otp, code", excludeMode = false))
        assertFalse(MessageFilter.matches("Hello there", "otp, code", excludeMode = false))
    }

    @Test
    fun `include mode is case insensitive`() {
        assertTrue(MessageFilter.matches("YOUR CODE IS READY", "code", excludeMode = false))
    }

    @Test
    fun `exclude mode blocks matching keywords`() {
        assertFalse(MessageFilter.matches("This is an ad, buy now", "ad, promo", excludeMode = true))
        assertTrue(MessageFilter.matches("Your delivery has arrived", "ad, promo", excludeMode = true))
    }
}
