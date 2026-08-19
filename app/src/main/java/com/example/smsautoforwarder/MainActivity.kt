package com.example.smsautoforwarder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: PrefsHelper

    private lateinit var editSender: TextInputEditText
    private lateinit var editRecipient: TextInputEditText
    private lateinit var switchEnable: SwitchCompat
    private lateinit var btnSave: MaterialButton
    private lateinit var btnPermissions: MaterialButton
    private lateinit var btnAppSettings: MaterialButton
    private lateinit var cardStatus: MaterialCardView
    private lateinit var cardNetworkStatus: MaterialCardView
    private lateinit var tvStatus: TextView
    private lateinit var tvNetworkStatus: TextView
    private lateinit var tvDeviceGuide: TextView

    private lateinit var editKeywords: TextInputEditText
    private lateinit var switchKeywordExclude: SwitchCompat
    private lateinit var switchChannelSms: SwitchCompat
    private lateinit var switchChannelTelegram: SwitchCompat
    private lateinit var switchChannelWebhook: SwitchCompat
    private lateinit var telegramDetailsContainer: LinearLayout
    private lateinit var webhookDetailsContainer: LinearLayout
    private lateinit var editTelegramToken: TextInputEditText
    private lateinit var editTelegramChatId: TextInputEditText
    private lateinit var editWebhookUrl: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = PrefsHelper(this)

        editSender = findViewById(R.id.editSender)
        editRecipient = findViewById(R.id.editRecipient)
        switchEnable = findViewById(R.id.switchEnable)
        btnSave = findViewById(R.id.btnSave)
        btnPermissions = findViewById(R.id.btnPermissions)
        btnAppSettings = findViewById(R.id.btnAppSettings)
        cardStatus = findViewById(R.id.cardStatus)
        cardNetworkStatus = findViewById(R.id.cardNetworkStatus)
        tvStatus = findViewById(R.id.tvStatus)
        tvNetworkStatus = findViewById(R.id.tvNetworkStatus)
        tvDeviceGuide = findViewById(R.id.tvDeviceGuide)

        editKeywords = findViewById(R.id.editKeywords)
        switchKeywordExclude = findViewById(R.id.switchKeywordExclude)
        switchChannelSms = findViewById(R.id.switchChannelSms)
        switchChannelTelegram = findViewById(R.id.switchChannelTelegram)
        switchChannelWebhook = findViewById(R.id.switchChannelWebhook)
        telegramDetailsContainer = findViewById(R.id.telegramDetailsContainer)
        webhookDetailsContainer = findViewById(R.id.webhookDetailsContainer)
        editTelegramToken = findViewById(R.id.editTelegramToken)
        editTelegramChatId = findViewById(R.id.editTelegramChatId)
        editWebhookUrl = findViewById(R.id.editWebhookUrl)

        switchChannelTelegram.setOnCheckedChangeListener { _, isChecked ->
            telegramDetailsContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        switchChannelWebhook.setOnCheckedChangeListener { _, isChecked ->
            webhookDetailsContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        loadSettings()
        updateDeviceGuide()
        updateStatus()

        btnSave.setOnClickListener { saveSettings() }
        btnPermissions.setOnClickListener { requestSmsPermissionsIfNeeded(force = true) }
        btnAppSettings.setOnClickListener { openAppSettings() }
    }

    override fun onResume() {
        super.onResume()
        if (::prefs.isInitialized) updateStatus()
    }

    private fun loadSettings() {
        editSender.setText(prefs.sender)
        editRecipient.setText(prefs.recipient)
        switchEnable.isChecked = prefs.isEnabled

        editKeywords.setText(prefs.keywordFilter)
        switchKeywordExclude.isChecked = prefs.keywordExcludeMode

        switchChannelSms.isChecked = prefs.channelSmsEnabled
        switchChannelTelegram.isChecked = prefs.channelTelegramEnabled
        switchChannelWebhook.isChecked = prefs.channelWebhookEnabled
        editTelegramToken.setText(prefs.telegramBotToken)
        editTelegramChatId.setText(prefs.telegramChatId)
        editWebhookUrl.setText(prefs.webhookUrl)

        telegramDetailsContainer.visibility =
            if (prefs.channelTelegramEnabled) View.VISIBLE else View.GONE
        webhookDetailsContainer.visibility =
            if (prefs.channelWebhookEnabled) View.VISIBLE else View.GONE
    }

    private fun saveSettings() {
        val sender = editSender.text?.toString()?.trim().orEmpty()
        val recipientRaw = editRecipient.text?.toString()?.trim().orEmpty()
        val recipient = SenderMatcher.normalizeRecipient(recipientRaw)

        val channelSms = switchChannelSms.isChecked
        val channelTelegram = switchChannelTelegram.isChecked
        val channelWebhook = switchChannelWebhook.isChecked

        val telegramToken = editTelegramToken.text?.toString()?.trim().orEmpty()
        val telegramChatId = editTelegramChatId.text?.toString()?.trim().orEmpty()
        val webhookUrl = editWebhookUrl.text?.toString()?.trim().orEmpty()

        if (switchEnable.isChecked && !channelSms && !channelTelegram && !channelWebhook) {
            Toast.makeText(this, R.string.error_channel_required, Toast.LENGTH_LONG).show()
            return
        }

        if (sender.isBlank()) {
            editSender.error = getString(R.string.error_sender_required)
            return
        }

        if (channelSms) {
            if (!SenderMatcher.isPlausibleRecipient(recipient)) {
                editRecipient.error = getString(R.string.error_recipient_invalid)
                return
            }

            // Avoid obvious self-configuration mistakes.
            if (SenderMatcher.matches(sender, recipient)) {
                Toast.makeText(
                    this,
                    R.string.error_sender_recipient_same,
                    Toast.LENGTH_LONG
                ).show()
                return
            }
        }

        if (channelTelegram && (telegramToken.isBlank() || telegramChatId.isBlank())) {
            Toast.makeText(this, R.string.telegram_token_hint, Toast.LENGTH_LONG).show()
            return
        }

        if (channelWebhook && webhookUrl.isBlank()) {
            Toast.makeText(this, R.string.webhook_url_hint, Toast.LENGTH_LONG).show()
            return
        }

        if (channelWebhook && !NetworkForwarder.isValidHttpsUrl(webhookUrl)) {
            Toast.makeText(this, R.string.error_webhook_https, Toast.LENGTH_LONG).show()
            return
        }

        prefs.sender = sender
        prefs.recipient = recipient
        prefs.isEnabled = switchEnable.isChecked

        prefs.keywordFilter = editKeywords.text?.toString()?.trim().orEmpty()
        prefs.keywordExcludeMode = switchKeywordExclude.isChecked

        prefs.channelSmsEnabled = channelSms
        prefs.channelTelegramEnabled = channelTelegram
        prefs.channelWebhookEnabled = channelWebhook
        prefs.telegramBotToken = telegramToken
        prefs.telegramChatId = telegramChatId
        prefs.webhookUrl = webhookUrl

        editRecipient.setText(recipient)

        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()

        if (prefs.isEnabled) {
            requestSmsPermissionsIfNeeded(force = false)
        }

        updateStatus()
    }

    private fun requestSmsPermissionsIfNeeded(force: Boolean) {
        val missing = requiredPermissions().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            if (force) {
                Toast.makeText(this, R.string.permissions_already_granted, Toast.LENGTH_SHORT).show()
            }
            updateStatus()
            return
        }

        ActivityCompat.requestPermissions(
            this,
            missing.toTypedArray(),
            SMS_PERMISSION_REQUEST
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == SMS_PERMISSION_REQUEST) {
            val allGranted = requiredPermissions().all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }

            Toast.makeText(
                this,
                if (allGranted) R.string.permissions_granted else R.string.permissions_missing,
                Toast.LENGTH_SHORT
            ).show()

            updateStatus()
        }
    }

    private fun requiredPermissions(): List<String> {
        val permissions = mutableListOf(Manifest.permission.RECEIVE_SMS)
        if (switchChannelSms.isChecked) permissions += Manifest.permission.SEND_SMS
        return permissions
    }

    private fun updateStatus() {
        val receiveGranted =
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) ==
                PackageManager.PERMISSION_GRANTED

        val sendGranted =
            ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) ==
                PackageManager.PERMISSION_GRANTED

        val permissionsOk = receiveGranted && (!prefs.channelSmsEnabled || sendGranted)

        val permissionText =
            if (permissionsOk) getString(R.string.status_permissions_granted)
            else getString(R.string.status_permissions_missing)

        val activeText =
            if (prefs.isEnabled) getString(R.string.status_active)
            else getString(R.string.status_inactive)

        val senderText =
            prefs.sender.ifBlank { getString(R.string.status_not_set) }

        val recipientText =
            prefs.recipient.ifBlank { getString(R.string.status_not_set) }

        val lastText = formatLastEvent()

        tvStatus.text = getString(
            R.string.status_template,
            activeText,
            senderText,
            recipientText,
            permissionText,
            lastText
        )

        setCardAccent(
            cardStatus,
            when {
                !prefs.isEnabled -> Accent.NEUTRAL
                permissionsOk && prefs.lastEvent != PrefsHelper.EVENT_FAILED -> Accent.SUCCESS
                else -> Accent.ERROR
            }
        )

        updateNetworkStatus()
    }

    private fun updateNetworkStatus() {
        val telegramConfigured =
            prefs.channelTelegramEnabled &&
                prefs.telegramBotToken.isNotBlank() &&
                prefs.telegramChatId.isNotBlank()

        val webhookConfigured =
            prefs.channelWebhookEnabled && prefs.webhookUrl.isNotBlank()

        val telegramText = getString(
            if (telegramConfigured) R.string.channel_configured else R.string.channel_not_configured
        )
        val webhookText = getString(
            if (webhookConfigured) R.string.channel_configured else R.string.channel_not_configured
        )

        val lastNetworkText = formatLastNetworkEvent()

        tvNetworkStatus.text = getString(
            R.string.network_status_template,
            telegramText,
            webhookText,
            lastNetworkText
        )

        setCardAccent(
            cardNetworkStatus,
            when {
                !telegramConfigured && !webhookConfigured -> Accent.NEUTRAL
                prefs.lastNetworkEvent == PrefsHelper.EVENT_FAILED -> Accent.ERROR
                prefs.lastNetworkEvent == PrefsHelper.EVENT_SENT -> Accent.SUCCESS
                else -> Accent.NEUTRAL
            }
        )
    }

    private enum class Accent { SUCCESS, ERROR, NEUTRAL }

    private fun setCardAccent(card: MaterialCardView, accent: Accent) {
        val colorRes = when (accent) {
            Accent.SUCCESS -> R.color.success_text
            Accent.ERROR -> R.color.error_text
            Accent.NEUTRAL -> R.color.neutral_stroke
        }
        val color = ContextCompat.getColor(this, colorRes)
        card.strokeColor = color
        card.strokeWidth = if (accent == Accent.NEUTRAL) {
            resources.getDimensionPixelSize(R.dimen.card_stroke_width)
        } else {
            resources.getDimensionPixelSize(R.dimen.card_stroke_width_accent)
        }
    }

    private fun formatLastEvent(): String {
        if (prefs.lastEventAt <= 0L || prefs.lastEvent == PrefsHelper.EVENT_NONE) {
            return getString(R.string.status_no_forward_yet)
        }

        val event = when (prefs.lastEvent) {
            PrefsHelper.EVENT_QUEUED -> getString(R.string.last_event_queued)
            PrefsHelper.EVENT_SENT -> getString(R.string.last_event_sent)
            PrefsHelper.EVENT_DELIVERED -> getString(R.string.last_event_delivered)
            PrefsHelper.EVENT_FAILED -> getString(
                R.string.last_event_failed,
                prefs.lastErrorCode
            )
            else -> getString(R.string.status_no_forward_yet)
        }

        return getString(R.string.last_event_with_time, event, formatTimestamp(prefs.lastEventAt))
    }

    private fun formatLastNetworkEvent(): String {
        if (prefs.lastNetworkEventAt <= 0L || prefs.lastNetworkEvent == PrefsHelper.EVENT_NONE) {
            return getString(R.string.network_no_attempt_yet)
        }

        val event = when (prefs.lastNetworkEvent) {
            PrefsHelper.EVENT_SENT -> getString(R.string.last_event_sent)
            PrefsHelper.EVENT_FAILED -> getString(R.string.last_event_failed, 0)
            else -> getString(R.string.network_no_attempt_yet)
        }

        val label = "${prefs.lastNetworkChannel}: $event"
        return getString(R.string.last_event_with_time, label, formatTimestamp(prefs.lastNetworkEventAt))
    }

    private fun formatTimestamp(millis: Long): String {
        return DateFormat.getDateFormat(this).format(Date(millis)) +
            " " +
            DateFormat.getTimeFormat(this).format(Date(millis))
    }

    private fun updateDeviceGuide() {
        val manufacturer = Build.MANUFACTURER
            .lowercase(Locale.ROOT)
            .trim()

        val guideRes = when {
            manufacturer.contains("samsung") -> R.string.guide_samsung
            manufacturer.contains("xiaomi") ||
                manufacturer.contains("redmi") ||
                manufacturer.contains("poco") -> R.string.guide_xiaomi
            else -> R.string.guide_generic
        }

        tvDeviceGuide.text = getString(guideRes)
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    companion object {
        private const val SMS_PERMISSION_REQUEST = 1001
    }
}
