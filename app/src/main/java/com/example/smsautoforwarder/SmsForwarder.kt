package com.example.smsautoforwarder

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import java.util.UUID

object SmsForwarder {
    private const val TAG = "SmsAutoForwarder"

    fun forward(context: Context, recipientInput: String, message: String) {
        val recipient = SenderMatcher.normalizeRecipient(recipientInput)
        require(recipient.isNotBlank()) { "Recipient is empty after normalization." }
        require(message.isNotEmpty()) { "Message is empty." }

        val smsManager = context.getSystemService(SmsManager::class.java)
            ?: error("SmsManager is unavailable on this device.")

        val parts = smsManager.divideMessage(message)
        val safeParts = if (parts.isNullOrEmpty()) arrayListOf(message) else parts
        val messageId = UUID.randomUUID().toString()
        val prefs = PrefsHelper(context)

        prefs.beginForward(messageId, safeParts.size)

        val sentIntents = ArrayList<PendingIntent>(safeParts.size)
        val deliveryIntents = ArrayList<PendingIntent>(safeParts.size)

        safeParts.indices.forEach { index ->
            sentIntents += statusPendingIntent(
                context = context,
                action = SmsStatusReceiver.ACTION_SENT,
                messageId = messageId,
                partIndex = index,
                totalParts = safeParts.size,
                requestCode = requestCode(messageId, index, 1)
            )

            deliveryIntents += statusPendingIntent(
                context = context,
                action = SmsStatusReceiver.ACTION_DELIVERED,
                messageId = messageId,
                partIndex = index,
                totalParts = safeParts.size,
                requestCode = requestCode(messageId, index, 2)
            )
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Forward queued to configured recipient; parts=${safeParts.size}")
        }

        if (safeParts.size == 1) {
            smsManager.sendTextMessage(
                recipient,
                null,
                safeParts[0],
                sentIntents[0],
                deliveryIntents[0]
            )
        } else {
            smsManager.sendMultipartTextMessage(
                recipient,
                null,
                safeParts,
                sentIntents,
                deliveryIntents
            )
        }
    }

    private fun statusPendingIntent(
        context: Context,
        action: String,
        messageId: String,
        partIndex: Int,
        totalParts: Int,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, SmsStatusReceiver::class.java).apply {
            this.action = action
            putExtra(SmsStatusReceiver.EXTRA_MESSAGE_ID, messageId)
            putExtra(SmsStatusReceiver.EXTRA_PART_INDEX, partIndex)
            putExtra(SmsStatusReceiver.EXTRA_TOTAL_PARTS, totalParts)
        }

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCode(messageId: String, partIndex: Int, kind: Int): Int {
        val base = messageId.hashCode() and 0x3fffffff
        return (base + partIndex * 4 + kind) and 0x7fffffff
    }
}
