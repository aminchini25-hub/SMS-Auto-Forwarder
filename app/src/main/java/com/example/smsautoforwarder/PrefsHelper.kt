package com.example.smsautoforwarder

import android.content.Context
import android.content.SharedPreferences

class PrefsHelper(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("sms_forwarder_prefs", Context.MODE_PRIVATE)

    var sender: String
        get() = prefs.getString(KEY_SENDER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SENDER, value).apply()

    var recipient: String
        get() = prefs.getString(KEY_RECIPIENT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_RECIPIENT, value).apply()

    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var lastEvent: String
        get() = prefs.getString(KEY_LAST_EVENT, EVENT_NONE) ?: EVENT_NONE
        set(value) = prefs.edit().putString(KEY_LAST_EVENT, value).apply()

    var lastEventAt: Long
        get() = prefs.getLong(KEY_LAST_EVENT_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_EVENT_AT, value).apply()

    var lastMessageId: String
        get() = prefs.getString(KEY_LAST_MESSAGE_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_MESSAGE_ID, value).apply()

    var lastTotalParts: Int
        get() = prefs.getInt(KEY_LAST_TOTAL_PARTS, 0)
        set(value) = prefs.edit().putInt(KEY_LAST_TOTAL_PARTS, value).apply()

    var lastSentParts: Int
        get() = prefs.getInt(KEY_LAST_SENT_PARTS, 0)
        set(value) = prefs.edit().putInt(KEY_LAST_SENT_PARTS, value).apply()

    var lastDeliveredParts: Int
        get() = prefs.getInt(KEY_LAST_DELIVERED_PARTS, 0)
        set(value) = prefs.edit().putInt(KEY_LAST_DELIVERED_PARTS, value).apply()

    var lastErrorCode: Int
        get() = prefs.getInt(KEY_LAST_ERROR_CODE, 0)
        set(value) = prefs.edit().putInt(KEY_LAST_ERROR_CODE, value).apply()

    var keywordFilter: String
        get() = prefs.getString(KEY_KEYWORD_FILTER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_KEYWORD_FILTER, value).apply()

    var keywordExcludeMode: Boolean
        get() = prefs.getBoolean(KEY_KEYWORD_EXCLUDE_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_KEYWORD_EXCLUDE_MODE, value).apply()

    var channelSmsEnabled: Boolean
        get() = prefs.getBoolean(KEY_CHANNEL_SMS, true)
        set(value) = prefs.edit().putBoolean(KEY_CHANNEL_SMS, value).apply()

    var channelTelegramEnabled: Boolean
        get() = prefs.getBoolean(KEY_CHANNEL_TELEGRAM, false)
        set(value) = prefs.edit().putBoolean(KEY_CHANNEL_TELEGRAM, value).apply()

    var channelWebhookEnabled: Boolean
        get() = prefs.getBoolean(KEY_CHANNEL_WEBHOOK, false)
        set(value) = prefs.edit().putBoolean(KEY_CHANNEL_WEBHOOK, value).apply()

    var telegramBotToken: String
        get() = prefs.getString(KEY_TELEGRAM_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TELEGRAM_TOKEN, value).apply()

    var telegramChatId: String
        get() = prefs.getString(KEY_TELEGRAM_CHAT_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TELEGRAM_CHAT_ID, value).apply()

    var webhookUrl: String
        get() = prefs.getString(KEY_WEBHOOK_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_WEBHOOK_URL, value).apply()

    /**
     * Each network channel (Telegram / Webhook) keeps its own last-result
     * record so that one channel's status can never overwrite another's.
     */
    data class ChannelStatus(
        val status: ForwardStatus?,
        val httpCode: Int,
        val at: Long
    )

    fun setChannelResult(channel: String, result: ForwardResult) {
        prefs.edit()
            .putString(channelStatusKey(channel), result.status.name)
            .putInt(channelHttpCodeKey(channel), result.httpCode ?: 0)
            .putLong(channelAtKey(channel), System.currentTimeMillis())
            .apply()
    }

    fun getChannelResult(channel: String): ChannelStatus {
        val statusName = prefs.getString(channelStatusKey(channel), null)
        val status = statusName?.let { name ->
            runCatching { ForwardStatus.valueOf(name) }.getOrNull()
        }
        return ChannelStatus(
            status = status,
            httpCode = prefs.getInt(channelHttpCodeKey(channel), 0),
            at = prefs.getLong(channelAtKey(channel), 0L)
        )
    }

    private fun channelStatusKey(channel: String) = "channel_${channel}_status"
    private fun channelHttpCodeKey(channel: String) = "channel_${channel}_http_code"
    private fun channelAtKey(channel: String) = "channel_${channel}_at"

    fun beginForward(messageId: String, totalParts: Int) {
        prefs.edit()
            .putString(KEY_LAST_MESSAGE_ID, messageId)
            .putInt(KEY_LAST_TOTAL_PARTS, totalParts)
            .putInt(KEY_LAST_SENT_PARTS, 0)
            .putInt(KEY_LAST_DELIVERED_PARTS, 0)
            .putInt(KEY_LAST_ERROR_CODE, 0)
            .putString(KEY_LAST_EVENT, EVENT_QUEUED)
            .putLong(KEY_LAST_EVENT_AT, System.currentTimeMillis())
            .apply()
    }

    companion object {
        private const val KEY_SENDER = "sender"
        private const val KEY_RECIPIENT = "recipient"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_LAST_EVENT = "last_event"
        private const val KEY_LAST_EVENT_AT = "last_event_at"
        private const val KEY_LAST_MESSAGE_ID = "last_message_id"
        private const val KEY_LAST_TOTAL_PARTS = "last_total_parts"
        private const val KEY_LAST_SENT_PARTS = "last_sent_parts"
        private const val KEY_LAST_DELIVERED_PARTS = "last_delivered_parts"
        private const val KEY_LAST_ERROR_CODE = "last_error_code"

        private const val KEY_KEYWORD_FILTER = "keyword_filter"
        private const val KEY_KEYWORD_EXCLUDE_MODE = "keyword_exclude_mode"
        private const val KEY_CHANNEL_SMS = "channel_sms"
        private const val KEY_CHANNEL_TELEGRAM = "channel_telegram"
        private const val KEY_CHANNEL_WEBHOOK = "channel_webhook"
        private const val KEY_TELEGRAM_TOKEN = "telegram_token"
        private const val KEY_TELEGRAM_CHAT_ID = "telegram_chat_id"
        private const val KEY_WEBHOOK_URL = "webhook_url"

        const val ERROR_CODE_PRE_SEND = -1

        const val EVENT_NONE = "none"
        const val EVENT_QUEUED = "queued"
        const val EVENT_SENT = "sent"
        const val EVENT_DELIVERED = "delivered"
        const val EVENT_FAILED = "failed"
    }
}
