package com.example.smsautoforwarder

object MessageFilter {

    fun matches(body: String, keywordsRaw: String, excludeMode: Boolean): Boolean {
        val keywords = parseKeywords(keywordsRaw)
        if (keywords.isEmpty()) return true

        val bodyLower = body.lowercase()
        val anyMatch = keywords.any { bodyLower.contains(it) }

        return if (excludeMode) !anyMatch else anyMatch
    }

    fun parseKeywords(keywordsRaw: String): List<String> {
        return keywordsRaw
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
    }
}
