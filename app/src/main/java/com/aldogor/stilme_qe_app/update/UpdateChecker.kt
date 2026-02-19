package com.aldogor.stilme_qe_app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.aldogor.stilme_qe_app.BuildConfig
import com.aldogor.stilme_qe_app.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Checks for app updates via a JSON file hosted on Google Drive.
 * Shows a non-blocking dialog when a newer version is available.
 *
 * Setup:
 * 1. Create update-info.json: {"version": "2.0", "apk_url": "https://drive.google.com/uc?export=download&id=FILE_ID"}
 * 2. Upload to Google Drive, share as "Anyone with the link"
 * 3. Set UPDATE_JSON_URL in local.properties to the direct download link:
 *    https://drive.google.com/uc?export=download&id=JSON_FILE_ID
 */
object UpdateChecker {

    private const val TAG = "UpdateChecker"
    private const val PREFS_NAME = "update_checker"
    private const val KEY_DISMISSED_VERSION = "dismissed_version"
    private const val KEY_LAST_CHECK = "last_check_time"
    private const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val gson = Gson()

    data class UpdateInfo(
        @SerializedName("version") val version: String,
        @SerializedName("apk_url") val apkUrl: String
    )

    /**
     * Checks for updates if enough time has passed since the last check.
     * Should be called from a coroutine scope (e.g., lifecycleScope).
     */
    suspend fun checkForUpdate(
        context: Context,
        onUpdateAvailable: (downloadUrl: String, version: String) -> Unit
    ) {
        val updateJsonUrl = BuildConfig.UPDATE_JSON_URL
        if (updateJsonUrl.isBlank()) {
            Log.w(TAG, "UPDATE_JSON_URL not configured in local.properties")
            return
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Throttle checks to once per day
        val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0)
        if (System.currentTimeMillis() - lastCheck < CHECK_INTERVAL_MS) {
            Log.d(TAG, "Skipping update check (checked recently)")
            return
        }

        try {
            val updateInfo = fetchUpdateInfo(updateJsonUrl) ?: return

            prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()

            val latestVersion = updateInfo.version.removePrefix("v")
            val currentVersion = BuildConfig.VERSION_NAME

            if (isNewerVersion(latestVersion, currentVersion)) {
                val dismissedVersion = prefs.getString(KEY_DISMISSED_VERSION, null)
                if (dismissedVersion == latestVersion) {
                    Log.d(TAG, "User already dismissed version $latestVersion")
                    return
                }

                withContext(Dispatchers.Main) {
                    onUpdateAvailable(updateInfo.apkUrl, latestVersion)
                }
            } else {
                Log.d(TAG, "App is up to date ($currentVersion)")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Update check failed", e)
        }
    }

    /**
     * Shows a non-blocking Material dialog informing the user of an available update.
     */
    fun showUpdateDialog(context: Context, downloadUrl: String, version: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.update_available_title))
            .setMessage(context.getString(R.string.update_available_message, version))
            .setPositiveButton(context.getString(R.string.update_download)) { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                context.startActivity(intent)
            }
            .setNegativeButton(context.getString(R.string.update_later)) { dialog, _ ->
                prefs.edit().putString(KEY_DISMISSED_VERSION, version).apply()
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    private suspend fun fetchUpdateInfo(url: String): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.w(TAG, "Update check returned ${response.code}")
                response.close()
                return@withContext null
            }

            val body = response.body?.string()
            response.close()

            if (body.isNullOrEmpty()) return@withContext null

            try {
                gson.fromJson(body, UpdateInfo::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse update info", e)
                null
            }
        }
    }

    /**
     * Compares two version strings (e.g., "2.1" vs "2.0").
     * Supports major.minor.patch format.
     */
    internal fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }

        val maxLength = maxOf(latestParts.size, currentParts.size)

        for (i in 0 until maxLength) {
            val latestPart = latestParts.getOrElse(i) { 0 }
            val currentPart = currentParts.getOrElse(i) { 0 }
            if (latestPart > currentPart) return true
            if (latestPart < currentPart) return false
        }

        return false // Equal
    }
}
