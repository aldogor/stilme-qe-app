package com.aldogor.stilme_qe_app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.aldogor.stilme_qe_app.databinding.ActivityPermissionRecoveryBinding

/**
 * Activity shown when mandatory permissions have been revoked.
 * User must re-grant permissions before continuing to use the app.
 */
class PermissionRecoveryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionRecoveryBinding
    private lateinit var permissionHelper: PermissionHelper

    private val usagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updateUI()
        checkAndFinish()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        updateUI()
        checkAndFinish()
    }

    private val batteryPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updateUI()
        checkAndFinish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityPermissionRecoveryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        permissionHelper = PermissionHelper(this)

        // Block back navigation - user must grant permissions
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing - user must grant permissions
            }
        })

        setupButtons()
        updateUI()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
        checkAndFinish()
    }

    private fun setupButtons() {
        binding.buttonGrantUsage.setOnClickListener {
            usagePermissionLauncher.launch(permissionHelper.getUsageAccessSettingsIntent())
        }

        binding.buttonGrantNotification.setOnClickListener {
            requestNotificationPermission()
        }

        binding.buttonGrantBattery.setOnClickListener {
            requestBatteryOptimizationExemption()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startActivity(permissionHelper.getNotificationSettingsIntent())
        }
    }

    private fun requestBatteryOptimizationExemption() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${packageName}")
            }
            batteryPermissionLauncher.launch(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                batteryPermissionLauncher.launch(intent)
            } catch (e2: Exception) {
                // Ignore
            }
        }
    }

    private fun isBatteryOptimizationDisabled(): Boolean {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(packageName)
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
            statusText.setTextColor(ContextCompat.getColor(this, R.color.success))
            button.visibility = View.GONE
        } else {
            indicator.setBackgroundResource(R.drawable.bg_status_dot_error)
            statusText.text = getString(R.string.permission_status_denied)
            statusText.setTextColor(ContextCompat.getColor(this, R.color.error))
            button.visibility = View.VISIBLE
        }
    }

    private fun checkAndFinish() {
        if (permissionHelper.hasMandatoryPermissions()) {
            // Permissions restored, go back to MainActivity
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }

}
