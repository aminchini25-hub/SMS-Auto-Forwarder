package com.example.smsautoforwarder

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import android.util.Log
import java.util.concurrent.atomic.AtomicInteger

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val prefs = PrefsHelper(context)
        if (!prefs.isEnabled) return

        val configuredSender = prefs.sender.trim()
        if (configuredSender.isEmpty()) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val incomingSender =
            messages.firstOrNull()?.originatingAddress
                ?: messages.firstOrNull()?.displayOriginatingAddress
                ?: return

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Incoming SMS detected from sender=$incomingSender")
        }

        if (!SenderMatcher.matches(incomingSender, configuredSender)) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Sender did not match configured sender.")
            return
        }

        val body = buildString {
            for (sms in messages) {
                append(sms.messageBody ?: sms.displayMessageBody.orEmpty())
            }
        }
        if (body.isEmpty()) return

        if (!MessageFilter.matches(body, prefs.keywordFilter, prefs.keywordExcludeMode)) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Message body did not pass keyword filter.")
            return
        }

        val appContext = context.applicationContext
        val configuredRecipient = prefs.recipient.trim()

        val smsEnabled = prefs.channelSmsEnabled && configuredRecipient.isNotEmpty()
        val telegramEnabled = prefs.channelTelegramEnabled &&
            prefs.telegramBotToken.isNotBlank() && prefs.telegramChatId.isNotBlank()
        val webhookEnabled = prefs.channelWebhookEnabled && prefs.webhookUrl.isNotBlank()

        if (!smsEnabled && !telegramEnabled && !webhookEnabled) return

        if (smsEnabled) {
            forwardViaSms(appContext, configuredRecipient, body)
        }

        if (!telegramEnabled && !webhookEnabled) return

        // Network calls are async; hold the broadcast alive until they finish
        // (with an OS-provided upper bound) so the process isn't killed early.
        val pendingResult = goAsync()
        val remaining = AtomicInteger(
            (if (telegramEnabled) 1 else 0) + (if (webhookEnabled) 1 else 0)
        )

        fun onChannelDone(channel: String, success: Boolean) {
            val p = PrefsHelper(appContext)
            p.lastNetworkChannel = channel
            p.lastNetworkEvent = if (success) PrefsHelper.EVENT_SENT else PrefsHelper.EVENT_FAILED
            p.lastNetworkEventAt = System.currentTimeMillis()

            if (remaining.decrementAndGet() <= 0) {
                pendingResult.finish()
            }
        }

        if (telegramEnabled) {
            val text = "From: $incomingSender\n\n$body"
            NetworkForwarder.sendTelegram(
                prefs.telegramBotToken,
                prefs.telegramChatId,
                text
            ) { success -> onChannelDone(CHANNEL_TELEGRAM, success) }
        }

        if (webhookEnabled) {
            NetworkForwarder.sendWebhook(
                prefs.webhookUrl,
                incomingSender,
                body
            ) { success -> onChannelDone(CHANNEL_WEBHOOK, success) }
        }
    }

    private fun forwardViaSms(context: Context, recipient: String, body: String) {
        if (context.checkSelfPermission(Manifest.permission.SEND_SMS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "SEND_SMS permission is missing; cannot forward via SMS.")
            return
        }

        try {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Sender matched. Forwarding via SMS (body omitted from logs).")
            }
            SmsForwarder.forward(context, recipient, body)
        } catch (t: Throwable) {
            val prefs = PrefsHelper(context)
            prefs.lastEvent = PrefsHelper.EVENT_FAILED
            prefs.lastEventAt = System.currentTimeMillis()
            Log.e(TAG, "SMS forwarding failed.", t)
        }
    }

    companion object {
        private const val TAG = "SmsAutoForwarder"
        const val CHANNEL_TELEGRAM = "telegram"
        const val CHANNEL_WEBHOOK = "webhook"
    }
}
