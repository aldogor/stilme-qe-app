package com.aldogor.stilme_qe_app

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Helper class for managing app permissions.
 * Handles both usage stats permission and notification permission.
 */
class PermissionHelper(private val context: Context) {

    companion object {
        private const val TAG = "PermissionHelper"
    }

    /**
     * Checks if the app has Usage Stats permission.
     * This permission is required to access app usage statistics.
     */
    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

        return try {
            // minSdk is 29, so we can always use unsafeCheckOpNoThrow
            val mode = appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: SecurityException) {
            Log.w(TAG, "Security exception while checking usage stats permission.", e)
            // In case of any security exception, assume permission not granted
            false
        }
    }

    /**
     * Creates an intent to open Usage Access Settings.
     * User must manually grant permission from this screen.
     */
    fun getUsageAccessSettingsIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    }

    /**
     * Checks if the app needs notification permission.
     * Only required for Android 13+ (API 33+).
     */
    fun needsNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            // Pre-Android 13, notification permission is granted by default
            false
        }
    }

    /**
     * Checks if notifications are currently enabled for the app.
     * This covers both permission (Android 13+) and notification settings.
     *
     * @return true if notifications are enabled, false otherwise
     */
    fun areNotificationsEnabled(): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        // First check if notifications are enabled at all
        if (!notificationManager.areNotificationsEnabled()) {
            return false
        }

        // For Android 13+, also check the runtime permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        return true
    }

    /**
     * Creates an intent to open the app's notification settings.
     * This allows users to re-enable notifications if they've disabled them.
     */
    fun getNotificationSettingsIntent(): Intent {
        return Intent().apply {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Checks if battery optimization is disabled for the app.
     * Required to ensure reliable background work execution.
     */
    fun isBatteryOptimizationDisabled(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Checks if all mandatory permissions are granted.
     * Usage stats, notifications, and battery optimization exemption are all required.
     */
    fun hasMandatoryPermissions(): Boolean {
        return hasUsageStatsPermission() && areNotificationsEnabled() && isBatteryOptimizationDisabled()
    }
}
