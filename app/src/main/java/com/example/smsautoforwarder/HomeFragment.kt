package com.example.smsautoforwarder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Date

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var prefs: PrefsHelper
    private lateinit var switchEnabled: SwitchCompat
    private lateinit var tvHeroState: TextView
    private lateinit var tvHeroHint: TextView
    private lateinit var tvSender: TextView
    private lateinit var tvRecipient: TextView
    private lateinit var tvChannels: TextView
    private lateinit var tvLastEvent: TextView
    private lateinit var tvPermission: TextView
    private lateinit var statusCard: MaterialCardView
    private lateinit var tvNetworkStatusTitle: TextView
    private lateinit var networkCard: MaterialCardView
    private lateinit var rowTelegramStatus: View
    private lateinit var tvTelegramStatus: TextView
    private lateinit var dividerNetworkChannels: View
    private lateinit var rowWebhookStatus: View
    private lateinit var tvWebhookStatus: TextView
    private var suppressToggle = false

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            val granted = permissionsOk()
            prefs.isEnabled = granted
            if (!granted) {
                Toast.makeText(requireContext(), R.string.permissions_missing, Toast.LENGTH_LONG).show()
            }
            refresh()
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PrefsHelper(requireContext())

        switchEnabled = view.findViewById(R.id.switchEnabled)
        tvHeroState = view.findViewById(R.id.tvHeroState)
        tvHeroHint = view.findViewById(R.id.tvHeroHint)
        tvSender = view.findViewById(R.id.tvSenderValue)
        tvRecipient = view.findViewById(R.id.tvRecipientValue)
        tvChannels = view.findViewById(R.id.tvChannelsValue)
        tvLastEvent = view.findViewById(R.id.tvLastEventValue)
        tvPermission = view.findViewById(R.id.tvPermissionValue)
        statusCard = view.findViewById(R.id.statusCard)
        tvNetworkStatusTitle = view.findViewById(R.id.tvNetworkStatusTitle)
        networkCard = view.findViewById(R.id.networkCard)
        rowTelegramStatus = view.findViewById(R.id.rowTelegramStatus)
        tvTelegramStatus = view.findViewById(R.id.tvTelegramStatusValue)
        dividerNetworkChannels = view.findViewById(R.id.dividerNetworkChannels)
        rowWebhookStatus = view.findViewById(R.id.rowWebhookStatus)
        tvWebhookStatus = view.findViewById(R.id.tvWebhookStatusValue)

        view.findViewById<MaterialButton>(R.id.btnEditRule).setOnClickListener {
            (activity as? MainActivity)?.navigateTo(R.id.nav_rules)
        }
        view.findViewById<MaterialButton>(R.id.btnEditChannels).setOnClickListener {
            (activity as? MainActivity)?.navigateTo(R.id.nav_channels)
        }

        switchEnabled.setOnCheckedChangeListener { _, checked ->
            if (!suppressToggle) handleEnabledChanged(checked)
        }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        if (::prefs.isInitialized) refresh()
    }

    private fun handleEnabledChanged(checked: Boolean) {
        if (!checked) {
            prefs.isEnabled = false
            refresh()
            return
        }

        val problem = configurationProblem()
        if (problem != null) {
            Toast.makeText(requireContext(), problem, Toast.LENGTH_LONG).show()
            prefs.isEnabled = false
            refresh()
            return
        }

        val missing = requiredPermissions().filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            prefs.isEnabled = true
            refresh()
            return
        }

        suppressToggle = true
        switchEnabled.isChecked = false
        suppressToggle = false

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.permission_dialog_title)
            .setMessage(R.string.permission_dialog_message)
            .setNegativeButton(R.string.not_now, null)
            .setPositiveButton(R.string.continue_label) { _, _ ->
                permissionLauncher.launch(missing.toTypedArray())
            }
            .show()
    }

    private fun configurationProblem(): Int? {
        if (prefs.sender.isBlank()) return R.string.setup_sender_first
        if (!prefs.channelSmsEnabled && !prefs.channelTelegramEnabled && !prefs.channelWebhookEnabled) {
            return R.string.setup_channel_first
        }
        if (prefs.channelSmsEnabled && !SenderMatcher.isPlausibleRecipient(prefs.recipient)) {
            return R.string.setup_recipient_first
        }
        return null
    }

    private fun requiredPermissions(): List<String> = buildList {
        add(Manifest.permission.RECEIVE_SMS)
        if (prefs.channelSmsEnabled) add(Manifest.permission.SEND_SMS)
    }

    private fun permissionsOk(): Boolean = requiredPermissions().all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private fun refresh() {
        val ready = configurationProblem() == null && permissionsOk()
        val active = prefs.isEnabled && ready

        if (prefs.isEnabled && !ready) prefs.isEnabled = false

        suppressToggle = true
        switchEnabled.isChecked = active
        suppressToggle = false

        tvHeroState.setText(if (active) R.string.dashboard_active else R.string.dashboard_paused)
        tvHeroHint.setText(
            when {
                active -> R.string.dashboard_active_hint
                configurationProblem() != null -> R.string.dashboard_setup_hint
                !permissionsOk() -> R.string.dashboard_permission_hint
                else -> R.string.dashboard_paused_hint
            }
        )

        tvSender.text = prefs.sender.ifBlank { getString(R.string.status_not_set) }
        tvRecipient.text = if (prefs.channelSmsEnabled) {
            prefs.recipient.ifBlank { getString(R.string.status_not_set) }
        } else {
            getString(R.string.not_required)
        }
        tvChannels.text = enabledChannels()
        tvLastEvent.text = formatLastEvent()
        tvLastEvent.setTextColor(colorForEvent(prefs.lastEvent))
        tvPermission.setText(if (permissionsOk()) R.string.status_permissions_granted else R.string.status_permissions_missing)

        val color = ContextCompat.getColor(
            requireContext(),
            if (active) R.color.success else R.color.warning
        )
        statusCard.strokeColor = color
        statusCard.strokeWidth = resources.getDimensionPixelSize(R.dimen.card_stroke_width_accent)

        refreshNetworkStatus()
    }

    private fun refreshNetworkStatus() {
        val showTelegram = prefs.channelTelegramEnabled
        val showWebhook = prefs.channelWebhookEnabled
        val showCard = showTelegram || showWebhook

        tvNetworkStatusTitle.visibility = if (showCard) View.VISIBLE else View.GONE
        networkCard.visibility = if (showCard) View.VISIBLE else View.GONE
        rowTelegramStatus.visibility = if (showTelegram) View.VISIBLE else View.GONE
        rowWebhookStatus.visibility = if (showWebhook) View.VISIBLE else View.GONE
        dividerNetworkChannels.visibility = if (showTelegram && showWebhook) View.VISIBLE else View.GONE

        if (showTelegram) {
            val result = prefs.getChannelResult(SmsReceiver.CHANNEL_TELEGRAM)
            tvTelegramStatus.text = formatChannelStatus(result)
            tvTelegramStatus.setTextColor(colorForStatus(result.status))
        }
        if (showWebhook) {
            val result = prefs.getChannelResult(SmsReceiver.CHANNEL_WEBHOOK)
            tvWebhookStatus.text = formatChannelStatus(result)
            tvWebhookStatus.setTextColor(colorForStatus(result.status))
        }
    }

    private fun formatChannelStatus(result: PrefsHelper.ChannelStatus): String {
        val status = result.status
        if (status == null || result.at <= 0L) {
            return getString(R.string.status_no_forward_yet)
        }

        val label = if (status == ForwardStatus.HTTP_ERROR) {
            getString(R.string.network_error_http, result.httpCode ?: 0)
        } else {
            getString(ErrorReasons.networkResultLabel(status))
        }

        val whenText = DateFormat.getDateFormat(requireContext()).format(Date(result.at)) +
            "  " + DateFormat.getTimeFormat(requireContext()).format(Date(result.at))
        return getString(R.string.last_event_with_time, label, whenText)
    }

    private fun colorForEvent(event: String): Int {
        val colorRes = when (event) {
            PrefsHelper.EVENT_SENT, PrefsHelper.EVENT_DELIVERED -> R.color.success
            PrefsHelper.EVENT_FAILED -> R.color.error
            PrefsHelper.EVENT_QUEUED -> R.color.warning
            else -> R.color.text_primary
        }
        return ContextCompat.getColor(requireContext(), colorRes)
    }

    private fun colorForStatus(status: ForwardStatus?): Int {
        val colorRes = when (status) {
            ForwardStatus.SUCCESS -> R.color.success
            null -> R.color.text_primary
            else -> R.color.error
        }
        return ContextCompat.getColor(requireContext(), colorRes)
    }

    private fun enabledChannels(): String {
        val labels = buildList {
            if (prefs.channelSmsEnabled) add(getString(R.string.channel_sms_short))
            if (prefs.channelTelegramEnabled) add(getString(R.string.channel_telegram_short))
            if (prefs.channelWebhookEnabled) add(getString(R.string.channel_webhook_short))
        }
        return if (labels.isEmpty()) getString(R.string.none_selected) else labels.joinToString(" • ")
    }

    private fun formatLastEvent(): String {
        if (prefs.lastEventAt <= 0 || prefs.lastEvent == PrefsHelper.EVENT_NONE) {
            return getString(R.string.status_no_forward_yet)
        }
        val label = when (prefs.lastEvent) {
            PrefsHelper.EVENT_QUEUED -> getString(R.string.last_event_queued)
            PrefsHelper.EVENT_SENT -> getString(R.string.last_event_sent)
            PrefsHelper.EVENT_DELIVERED -> getString(R.string.last_event_delivered)
            PrefsHelper.EVENT_FAILED -> getString(
                R.string.last_event_failed,
                getString(ErrorReasons.smsResultLabel(prefs.lastErrorCode))
            )
            else -> getString(R.string.status_no_forward_yet)
        }
        val whenText = DateFormat.getDateFormat(requireContext()).format(Date(prefs.lastEventAt)) +
            "  " + DateFormat.getTimeFormat(requireContext()).format(Date(prefs.lastEventAt))
        return getString(R.string.last_event_with_time, label, whenText)
    }
}
