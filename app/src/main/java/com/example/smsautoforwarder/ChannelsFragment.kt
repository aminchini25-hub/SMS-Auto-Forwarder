package com.example.smsautoforwarder

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.net.URL

class ChannelsFragment : Fragment(R.layout.fragment_channels) {

    private lateinit var prefs: PrefsHelper
    private lateinit var switchSms: SwitchCompat
    private lateinit var switchTelegram: SwitchCompat
    private lateinit var switchWebhook: SwitchCompat
    private lateinit var telegramDetails: LinearLayout
    private lateinit var webhookDetails: LinearLayout
    private lateinit var editTelegramToken: TextInputEditText
    private lateinit var editTelegramChatId: TextInputEditText
    private lateinit var editWebhookUrl: TextInputEditText

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PrefsHelper(requireContext())

        switchSms = view.findViewById(R.id.switchSms)
        switchTelegram = view.findViewById(R.id.switchTelegram)
        switchWebhook = view.findViewById(R.id.switchWebhook)
        telegramDetails = view.findViewById(R.id.telegramDetails)
        webhookDetails = view.findViewById(R.id.webhookDetails)
        editTelegramToken = view.findViewById(R.id.editTelegramToken)
        editTelegramChatId = view.findViewById(R.id.editTelegramChatId)
        editWebhookUrl = view.findViewById(R.id.editWebhookUrl)

        switchSms.isChecked = prefs.channelSmsEnabled
        switchTelegram.isChecked = prefs.channelTelegramEnabled
        switchWebhook.isChecked = prefs.channelWebhookEnabled
        editTelegramToken.setText(prefs.telegramBotToken)
        editTelegramChatId.setText(prefs.telegramChatId)
        editWebhookUrl.setText(prefs.webhookUrl)

        switchTelegram.setOnCheckedChangeListener { _, checked ->
            telegramDetails.visibility = if (checked) View.VISIBLE else View.GONE
        }
        switchWebhook.setOnCheckedChangeListener { _, checked ->
            webhookDetails.visibility = if (checked) View.VISIBLE else View.GONE
        }
        updateVisibility()

        view.findViewById<MaterialButton>(R.id.btnSaveChannels).setOnClickListener { saveChannels() }
    }

    private fun updateVisibility() {
        telegramDetails.visibility = if (switchTelegram.isChecked) View.VISIBLE else View.GONE
        webhookDetails.visibility = if (switchWebhook.isChecked) View.VISIBLE else View.GONE
    }

    private fun saveChannels() {
        val sms = switchSms.isChecked
        val telegram = switchTelegram.isChecked
        val webhook = switchWebhook.isChecked

        if (!sms && !telegram && !webhook) {
            Toast.makeText(requireContext(), R.string.error_choose_channel, Toast.LENGTH_LONG).show()
            return
        }

        val token = editTelegramToken.text?.toString()?.trim().orEmpty()
        val chatId = editTelegramChatId.text?.toString()?.trim().orEmpty()
        val webhookUrl = editWebhookUrl.text?.toString()?.trim().orEmpty()

        if (sms && !SenderMatcher.isPlausibleRecipient(prefs.recipient)) {
            Toast.makeText(requireContext(), R.string.setup_recipient_first, Toast.LENGTH_LONG).show()
            return
        }
        if (telegram && (token.isBlank() || chatId.isBlank())) {
            Toast.makeText(requireContext(), R.string.error_telegram_config, Toast.LENGTH_LONG).show()
            return
        }
        if (webhook && !isValidHttpsUrl(webhookUrl)) {
            editWebhookUrl.error = getString(R.string.error_https_webhook)
            return
        }

        prefs.channelSmsEnabled = sms
        prefs.channelTelegramEnabled = telegram
        prefs.channelWebhookEnabled = webhook
        prefs.telegramBotToken = token
        prefs.telegramChatId = chatId
        prefs.webhookUrl = webhookUrl

        Toast.makeText(requireContext(), R.string.channels_saved, Toast.LENGTH_SHORT).show()
    }

    private fun isValidHttpsUrl(value: String): Boolean = try {
        val url = URL(value)
        url.protocol.equals("https", ignoreCase = true) && url.host.isNotBlank()
    } catch (_: Throwable) {
        false
    }
}
