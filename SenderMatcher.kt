package com.example.smsautoforwarder

/**
 * Pure Kotlin sender matching logic.
 *
 * - Phone numbers: converts Persian/Arabic digits, strips formatting,
 *   supports local/international forms such as 0912... and +98912...
 *   by comparing the last 10 digits when both values look like phone numbers.
 * - Alphanumeric sender IDs: normalized and matched as text, never by "last 10".
 */
object SenderMatcher {

    fun matches(incoming: String?, configured: String?): Boolean {
        if (incoming.isNullOrBlank() || configured.isNullOrBlank()) return false

        val a = normalizeForMatching(incoming)
        val b = normalizeForMatching(configured)

        if (a.equals(b, ignoreCase = true)) return true

        val aPhone = phoneDigitsOrNull(a)
        val bPhone = phoneDigitsOrNull(b)

        if (aPhone != null && bPhone != null) {
            if (aPhone == bPhone) return true

            // Handles common local/international formatting differences.
            // Example: 09123456789 vs +989123456789.
            if (aPhone.length >= 10 && bPhone.length >= 10) {
                return aPhone.takeLast(10) == bPhone.takeLast(10)
            }
        }

        return false
    }

    fun normalizeRecipient(value: String): String {
        val converted = convertLocalizedDigits(value.trim())
        val out = StringBuilder()

        converted.forEachIndexed { index, ch ->
            when {
                ch.isDigit() -> out.append(ch)
                ch == '+' && out.isEmpty() -> out.append(ch)
            }
        }

        return out.toString()
    }

    fun isPlausibleRecipient(value: String): Boolean {
        val normalized = normalizeRecipient(value)
        val digits = normalized.count { it.isDigit() }
        return digits in 3..20
    }

    private fun normalizeForMatching(value: String): String {
        val converted = convertLocalizedDigits(value.trim())
        return buildString {
            for (ch in converted) {
                if (ch.isLetterOrDigit() || (ch == '+' && isEmpty())) {
                    append(ch.lowercaseChar())
                }
            }
        }
    }

    private fun phoneDigitsOrNull(value: String): String? {
        val noPlus = value.removePrefix("+")
        if (noPlus.isEmpty()) return null
        if (!noPlus.all { it.isDigit() }) return null
        return noPlus
    }

    private fun convertLocalizedDigits(value: String): String {
        return buildString(value.length) {
            for (ch in value) {
                append(
                    when (ch) {
                        '۰', '٠' -> '0'
                        '۱', '١' -> '1'
                        '۲', '٢' -> '2'
                        '۳', '٣' -> '3'
                        '۴', '٤' -> '4'
                        '۵', '٥' -> '5'
                        '۶', '٦' -> '6'
                        '۷', '٧' -> '7'
                        '۸', '٨' -> '8'
                        '۹', '٩' -> '9'
                        else -> ch
                    }
                )
            }
        }
    }
}
