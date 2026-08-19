package com.example.smsautoforwarder

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class SmsStatusReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getStringExtra(EXTRA_MESSAGE_ID) ?: return
        val totalParts = intent.getIntExtra(EXTRA_TOTAL_PARTS, 1)
        val prefs = PrefsHelper(context)

        // Ignore stale callbacks from an older forwarding job.
        if (messageId != prefs.lastMessageId) return

        when (intent.action) {
            ACTION_SENT -> {
                if (resultCode == Activity.RESULT_OK) {
                    prefs.lastSentParts = (prefs.lastSentParts + 1).coerceAtMost(totalParts)
                    prefs.lastEventAt = System.currentTimeMillis()

                    if (prefs.lastSentParts >= totalParts && prefs.lastEvent != PrefsHelper.EVENT_FAILED) {
                        prefs.lastEvent = PrefsHelper.EVENT_SENT
                    }

                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "SMS part sent (${prefs.lastSentParts}/$totalParts)")
                    }
                } else {
                    prefs.lastErrorCode = resultCode
                    prefs.lastEvent = PrefsHelper.EVENT_FAILED
                    prefs.lastEventAt = System.currentTimeMillis()

                    Log.e(TAG, "SMS sending failed, resultCode=$resultCode")
                }
            }

            ACTION_DELIVERED -> {
                // Delivery reports depend on carrier support.
                prefs.lastDeliveredParts =
                    (prefs.lastDeliveredParts + 1).coerceAtMost(totalParts)
                prefs.lastEventAt = System.currentTimeMillis()

                if (prefs.lastDeliveredParts >= totalParts &&
                    prefs.lastEvent != PrefsHelper.EVENT_FAILED
                ) {
                    prefs.lastEvent = PrefsHelper.EVENT_DELIVERED
                }

                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "SMS delivery report (${prefs.lastDeliveredParts}/$totalParts)")
                }
            }
        }
    }

    companion object {
        private const val TAG = "SmsAutoForwarder"

        const val ACTION_SENT =
            "com.example.smsautoforwarder.action.SMS_SENT"
        const val ACTION_DELIVERED =
            "com.example.smsautoforwarder.action.SMS_DELIVERED"

        const val EXTRA_MESSAGE_ID = "message_id"
        const val EXTRA_PART_INDEX = "part_index"
        const val EXTRA_TOTAL_PARTS = "total_parts"
    }
}
