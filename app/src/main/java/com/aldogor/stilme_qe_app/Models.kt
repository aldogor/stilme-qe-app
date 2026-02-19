package com.aldogor.stilme_qe_app

import com.google.gson.annotations.SerializedName
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Data models, constants, and state classes for STILME-QE Usage Monitor.
 */

// ============================================================================
// CONSTANTS
// ============================================================================

object AppConfig {
    /**
     * Date formatter for CSV export (ISO 8601 format).
     * Example output: 2024-11-08
     */
    val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    
    /**
     * Map of monitored social media applications.
     * Key: Package name (used for querying UsageStats)
     * Value: Display name (used in CSV headers and UI)
     */
    val MONITORED_APPS: Map<String, String> = mapOf(
        "com.instagram.android" to "Instagram",
        "com.zhiliaoapp.musically" to "TikTok", 
        "com.facebook.katana" to "Facebook",
        "com.google.android.youtube" to "YouTube",
        "com.pinterest" to "Pinterest",
        "tv.twitch.android.app" to "Twitch",
        "com.bereal.ft" to "BeReal",
        "com.snapchat.android" to "Snapchat",
        "com.twitter.android" to "X (Twitter)",
        "com.linkedin.android" to "LinkedIn",
        "com.reddit.frontpage" to "Reddit"
    )
}

// ============================================================================
// USAGE DATA MODELS
// ============================================================================

/**
 * Root storage container for all usage data.
 * Stored encrypted in SharedPreferences as JSON.
 */
data class StoredUsageData(
    @SerializedName("version")
    val version: Int = 2,
    
    @SerializedName("last_updated")
    val lastUpdated: String, // ISO-8601 format
    
    @SerializedName("daily_usage")
    val dailyUsage: List<DailyUsage>
)

/**
 * Holds the processed usage statistics for a single day.
 */
data class DailyUsage(
    @SerializedName("date")
    val dateString: String, // ISO-8601 format for JSON storage
    
    @SerializedName("app_usage")
    val appUsage: Map<String, AppUsageData>
) {
    /**
     * Gets the date as LocalDate, parsing from string.
     */
    val date: LocalDate
        get() = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
    
    /**
     * Alternative constructor for programmatic creation.
     */
    constructor(date: LocalDate, appUsage: Map<String, AppUsageData>) : this(
        dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
        appUsage = appUsage
    )
}

/**
 * Holds the specific metrics for a single app on a given day.
 */
data class AppUsageData(
    @SerializedName("time_minutes")
    val timeMinutes: Int,
    
    @SerializedName("opens")
    val opens: Long
)

// ============================================================================
// OPERATION RESULTS
// ============================================================================

/**
 * Result types for data collection operations.
 */
sealed class CollectionResult {
    data class Success(
        val daysCollected: Int,
        val totalDaysStored: Int,
        val dateRange: Pair<LocalDate, LocalDate>
    ) : CollectionResult()
    
    data class NoData(val message: String) : CollectionResult()
    data class PermissionDenied(val message: String) : CollectionResult()
    data class Error(val message: String) : CollectionResult()
}

// ============================================================================
// UI STATE MODELS
// ============================================================================

/**
 * Unified UI state containing all display data.
 * Single source of truth for MainActivity UI.
 */
data class UiState(
    val isLoading: Boolean = false,
    val statusMessage: String = "",
    val csvContent: String? = null,
    val error: String? = null,
    val daysCollected: Int = 0,
    val totalDaysStored: Int = 0,
    val storageInfo: StorageInfo? = null
)

/**
 * Storage information for UI display.
 */
data class StorageInfo(
    val totalDaysStored: Int,
    val earliestDate: LocalDate?,
    val latestDate: LocalDate?,
    val lastSync: LocalDateTime?,
    val isDataAvailable: Boolean
)

// ============================================================================
// STORAGE STATISTICS
// ============================================================================

/**
 * Statistics about stored data.
 * Used for UI display and storage management.
 */
data class StorageStatistics(
    val totalDays: Int,
    val earliestDate: LocalDate?,
    val latestDate: LocalDate?,
    val lastSync: LocalDateTime?
)
