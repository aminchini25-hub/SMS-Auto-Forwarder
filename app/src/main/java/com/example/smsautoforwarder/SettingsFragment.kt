package com.example.smsautoforwarder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private lateinit var prefs: PrefsHelper
    private lateinit var tvPermissionState: TextView
    private lateinit var tvDeviceGuide: TextView

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            updatePermissionState()
            if (permissionsOk()) {
                Toast.makeText(requireContext(), R.string.permissions_granted, Toast.LENGTH_SHORT).show()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PrefsHelper(requireContext())
        tvPermissionState = view.findViewById(R.id.tvPermissionState)
        tvDeviceGuide = view.findViewById(R.id.tvDeviceGuide)

        view.findViewById<MaterialButton>(R.id.btnRequestPermissions).setOnClickListener {
            val missing = requiredPermissions().filter {
                ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
            }
            if (missing.isEmpty()) {
                Toast.makeText(requireContext(), R.string.permissions_already_granted, Toast.LENGTH_SHORT).show()
            } else {
                permissionLauncher.launch(missing.toTypedArray())
            }
        }
        view.findViewById<MaterialButton>(R.id.btnOpenAppSettings).setOnClickListener {
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${requireContext().packageName}")
                )
            )
        }
        view.findViewById<MaterialButton>(R.id.btnInstallInfo).setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.install_warning_title)
                .setMessage(R.string.install_warning_message)
                .setPositiveButton(R.string.got_it, null)
                .show()
        }

        updatePermissionState()
        updateDeviceGuide()
    }

    override fun onResume() {
        super.onResume()
        if (::prefs.isInitialized) updatePermissionState()
    }

    private fun requiredPermissions(): List<String> = buildList {
        add(Manifest.permission.RECEIVE_SMS)
        if (prefs.channelSmsEnabled) add(Manifest.permission.SEND_SMS)
    }

    private fun permissionsOk(): Boolean = requiredPermissions().all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private fun updatePermissionState() {
        tvPermissionState.setText(
            if (permissionsOk()) R.string.permissions_ready_detail else R.string.permissions_needed_detail
        )
    }

    private fun updateDeviceGuide() {
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
        tvDeviceGuide.setText(
            when {
                manufacturer.contains("samsung") -> R.string.guide_samsung
                manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> R.string.guide_xiaomi
                else -> R.string.guide_generic
            }
        )
    }
}
