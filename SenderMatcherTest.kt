package com.example.smsautoforwarder

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SenderMatcherTest {

    @Test
    fun localAndIranInternationalNumbersMatch() {
        assertTrue(SenderMatcher.matches("09123456789", "+989123456789"))
    }

    @Test
    fun persianDigitsMatchAsciiDigits() {
        assertTrue(SenderMatcher.matches("۰۹۱۲۳۴۵۶۷۸۹", "09123456789"))
    }

    @Test
    fun arabicDigitsMatchAsciiDigits() {
        assertTrue(SenderMatcher.matches("٠٩١٢٣٤٥٦٧٨٩", "09123456789"))
    }

    @Test
    fun senderIdsMatchCaseInsensitively() {
        assertTrue(SenderMatcher.matches("MyBank", "mybank"))
    }

    @Test
    fun differentTextSenderIdsDoNotMatchBySuffix() {
        assertFalse(SenderMatcher.matches("ABC-MYBANK", "XYZ-MYBANK"))
    }

    @Test
    fun differentPhoneNumbersDoNotMatch() {
        assertFalse(SenderMatcher.matches("09123456789", "09123456780"))
    }
}
