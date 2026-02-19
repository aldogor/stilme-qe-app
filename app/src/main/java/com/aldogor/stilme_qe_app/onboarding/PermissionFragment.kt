package com.aldogor.stilme_qe_app.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.aldogor.stilme_qe_app.PermissionHelper
import com.aldogor.stilme_qe_app.R
import com.aldogor.stilme_qe_app.databinding.FragmentPermissionBinding

/**
 * Permission request screen for usage stats and notification access.
 */
class PermissionFragment : Fragment() {

    private var _binding: FragmentPermissionBinding? = null
    private val binding get() = _binding!!

    private var navigator: OnboardingNavigator? = null
    private lateinit var permissionHelper: PermissionHelper

    private val usagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updateUI()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        updateUI()
    }

    private val batteryPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updateUI()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        navigator = context as? OnboardingNavigator
        permissionHelper = PermissionHelper(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPermissionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonGrantUsage.setOnClickListener {
            usagePermissionLauncher.launch(permissionHelper.getUsageAccessSettingsIntent())
        }

        binding.textUsageHelp.setOnClickListener {
            val helpUrl = getString(R.string.permission_usage_help_url)
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(helpUrl)))
        }

        binding.buttonGrantNotification.setOnClickListener {
            requestNotificationPermission()
        }

        binding.buttonGrantBattery.setOnClickListener {
            requestBatteryOptimizationExemption()
        }

        binding.buttonContinue.setOnClickListener {
            navigator?.navigateToQuestionnaire()
        }

        updateUI()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // Pre-Android 13: open notification settings
            startActivity(permissionHelper.getNotificationSettingsIntent())
        }
    }

    private fun requestBatteryOptimizationExemption() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${requireContext().packageName}")
            }
            batteryPermissionLauncher.launch(intent)
        } catch (e: Exception) {
            // Fallback to general battery settings
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                batteryPermissionLauncher.launch(intent)
            } catch (e2: Exception) {
                // Ignore - user will have to do it manually
            }
        }
    }

    private fun isBatteryOptimizationDisabled(): Boolean {
        val powerManager = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(requireContext().packageName)
    }

    private fun updateUI() {
        val hasUsagePermission = permissionHelper.hasUsageStatsPermission()
        val hasNotificationPermission = permissionHelper.areNotificationsEnabled()
        val hasBatteryExemption = isBatteryOptimizationDisabled()

        // Update usage permission card
        updatePermissionCard(
            hasPermission = hasUsagePermission,
            indicator = binding.usageStatusIndicator,
            statusText = binding.usageStatusText,
            button = binding.buttonGrantUsage
        )

        // Update notification permission card
        updatePermissionCard(
            hasPermission = hasNotificationPermission,
            indicator = binding.notificationStatusIndicator,
            statusText = binding.notificationStatusText,
            button = binding.buttonGrantNotification
        )

        // Update battery optimization card
        updatePermissionCard(
            hasPermission = hasBatteryExemption,
            indicator = binding.batteryStatusIndicator,
            statusText = binding.batteryStatusText,
            button = binding.buttonGrantBattery
        )

        // Enable continue button only if ALL three permissions are granted
        val canContinue = hasUsagePermission && hasNotificationPermission && hasBatteryExemption
        binding.buttonContinue.isEnabled = canContinue
        binding.buttonContinue.alpha = if (canContinue) 1.0f else 0.5f
    }

    private fun updatePermissionCard(
        hasPermission: Boolean,
        indicator: View,
        statusText: android.widget.TextView,
        button: android.widget.Button
    ) {
        if (hasPermission) {
            indicator.setBackgroundResource(R.drawable.bg_status_dot_success)
            statusText.text = getString(R.string.permission_status_granted)
            statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.success))
            button.visibility = View.GONE
        } else {
            indicator.setBackgroundResource(R.drawable.bg_status_dot_error)
            statusText.text = getString(R.string.permission_status_denied)
            statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.error))
            button.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
