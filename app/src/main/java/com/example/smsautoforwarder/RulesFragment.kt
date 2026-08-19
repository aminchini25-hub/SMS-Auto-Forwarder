package com.example.smsautoforwarder

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class RulesFragment : Fragment(R.layout.fragment_rules) {

    private enum class PickTarget { SENDER, RECIPIENT }

    private lateinit var prefs: PrefsHelper
    private lateinit var editSender: TextInputEditText
    private lateinit var editRecipient: TextInputEditText
    private lateinit var editKeywords: TextInputEditText
    private lateinit var switchExclude: SwitchCompat
    private var pickTarget = PickTarget.RECIPIENT

    private val contactPicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val uri = result.data?.data ?: return@registerForActivityResult

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )

        requireContext().contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val number = if (numberIndex >= 0) cursor.getString(numberIndex).orEmpty() else ""
            val displayName = if (nameIndex >= 0) cursor.getString(nameIndex).orEmpty() else ""
            if (number.isBlank()) return@use

            when (pickTarget) {
                PickTarget.SENDER -> editSender.setText(SenderMatcher.normalizeRecipient(number))
                PickTarget.RECIPIENT -> editRecipient.setText(SenderMatcher.normalizeRecipient(number))
            }

            if (displayName.isNotBlank()) {
                Toast.makeText(requireContext(), getString(R.string.contact_selected, displayName), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PrefsHelper(requireContext())

        editSender = view.findViewById(R.id.editSender)
        editRecipient = view.findViewById(R.id.editRecipient)
        editKeywords = view.findViewById(R.id.editKeywords)
        switchExclude = view.findViewById(R.id.switchExclude)

        editSender.setText(prefs.sender)
        editRecipient.setText(prefs.recipient)
        editKeywords.setText(prefs.keywordFilter)
        switchExclude.isChecked = prefs.keywordExcludeMode

        view.findViewById<MaterialButton>(R.id.btnPickSender).setOnClickListener {
            launchContactPicker(PickTarget.SENDER)
        }
        view.findViewById<MaterialButton>(R.id.btnPickRecipient).setOnClickListener {
            launchContactPicker(PickTarget.RECIPIENT)
        }
        view.findViewById<MaterialButton>(R.id.btnSaveRule).setOnClickListener { saveRule() }
    }

    private fun launchContactPicker(target: PickTarget) {
        pickTarget = target
        val intent = Intent(
            Intent.ACTION_PICK,
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        )
        contactPicker.launch(intent)
    }

    private fun saveRule() {
        val sender = editSender.text?.toString()?.trim().orEmpty()
        val recipient = SenderMatcher.normalizeRecipient(
            editRecipient.text?.toString()?.trim().orEmpty()
        )

        if (sender.isBlank()) {
            editSender.error = getString(R.string.error_sender_required)
            return
        }
        if (prefs.channelSmsEnabled && !SenderMatcher.isPlausibleRecipient(recipient)) {
            editRecipient.error = getString(R.string.error_recipient_invalid)
            return
        }
        if (prefs.channelSmsEnabled && SenderMatcher.matches(sender, recipient)) {
            Toast.makeText(requireContext(), R.string.error_sender_recipient_same, Toast.LENGTH_LONG).show()
            return
        }

        prefs.sender = sender
        prefs.recipient = recipient
        prefs.keywordFilter = editKeywords.text?.toString()?.trim().orEmpty()
        prefs.keywordExcludeMode = switchExclude.isChecked
        editRecipient.setText(recipient)

        Toast.makeText(requireContext(), R.string.rule_saved, Toast.LENGTH_SHORT).show()
    }
}
