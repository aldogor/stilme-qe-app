package com.aldogor.stilme_qe_app

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.aldogor.stilme_qe_app.study.StudyManager
import com.google.gson.Gson
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Unified data storage and management for STILME-QE Usage Monitor.
 * Combines encrypted storage, CSV export, and usage stats retrieval.
 */
class DataStorage(private val context: Context) {

    companion object {
        private const val TAG = "DataStorage"
        private const val PREFS_FILE_NAME = "stilme_qe_encrypted_prefs"
        private const val KEY_USAGE_DATA = "usage_data_json"
        private const val KEY_USAGE_DATA_CORRUPT_BACKUP = "usage_data_json_corrupt_backup"
        private const val KEY_LAST_SYNC = "last_sync_timestamp"
        private const val KEY_LAST_BACKGROUND_SYNC = "last_background_sync_timestamp"
        private const val KEY_STUDY_ID = "anonymous_study_id" // legacy key; cleared on withdrawal
        private const val MAX_STORAGE_DAYS = 140 // Keep max 140 days of data
    }

    private val gson = Gson()
    private val encryptedPrefs = EncryptedPrefsFactory.create(context, PREFS_FILE_NAME)
    
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    // ============================================================================
    // USAGE STATS COLLECTION
    // ============================================================================

    /**
     * Fetches usage data for the last N complete days from Android UsageStats.
     * 
     * @param days Number of complete days to retrieve (max 9)
     * @return List of daily usage data
     */
    fun getUsageDataForLastNDays(days: Int): List<DailyUsage> {
        val calendar = Calendar.getInstance()

        // End: Yesterday at 23:59:59
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val endTime = calendar.timeInMillis

        // Start: N days ago at 00:00:00
        calendar.add(Calendar.DAY_OF_YEAR, -(days - 1))
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis

        Log.d(TAG, "Fetching usage from ${Instant.ofEpochMilli(startTime)} to ${Instant.ofEpochMilli(endTime)}")

        // Get aggregated stats for time in foreground
        val usageStatsList = try {
            usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error querying usage stats", e)
            emptyList()
        }

        // Get events for app opens
        val usageEvents = try {
            queryUsageEvents(startTime, endTime)
        } catch (e: Exception) {
            Log.e(TAG, "Error querying usage events", e)
            emptyList()
        }

        // If the OS returned NO aggregated stats AND no events for the entire window, we have no
        // access to underlying data (permission silently revoked, or the OS buffer is empty).
        // Returning empty here — rather than building 0-minute/0-open rows for every day — prevents
        // fabricated zeros from being saved and submitted to REDCap as if they were real usage.
        // A genuinely abstinent participant still has stats for OTHER apps, so their true zero-social
        // rows are preserved; only the total-absence case is treated as "no data".
        if (usageStatsList.isEmpty() && usageEvents.isEmpty()) {
            Log.w(TAG, "No usage stats or events for the window — treating as no data (not zeros)")
            return emptyList()
        }

        // Group by date
        val statsByDate = usageStatsList.groupBy {
            Instant.ofEpochMilli(it.firstTimeStamp).atZone(ZoneId.systemDefault()).toLocalDate()
        }
        val eventsByDate = usageEvents.groupBy {
            Instant.ofEpochMilli(it.timeStamp).atZone(ZoneId.systemDefault()).toLocalDate()
        }

        // Build daily stats
        val dailyStats = mutableListOf<DailyUsage>()
        var currentDate = Instant.ofEpochMilli(startTime).atZone(ZoneId.systemDefault()).toLocalDate()
        val endDate = Instant.ofEpochMilli(endTime).atZone(ZoneId.systemDefault()).toLocalDate()

        while (!currentDate.isAfter(endDate)) {
            val statsForDay = statsByDate[currentDate] ?: emptyList()
            val eventsForDay = eventsByDate[currentDate] ?: emptyList()
            val appUsageMap = mutableMapOf<String, AppUsageData>()

            AppConfig.MONITORED_APPS.keys.forEach { packageName ->
                val timeInForeground = statsForDay
                    .filter { it.packageName == packageName }
                    .sumOf { it.totalTimeInForeground }
                
                val appOpens = eventsForDay.count { it.packageName == packageName }.toLong()

                appUsageMap[packageName] = AppUsageData(
                    timeMinutes = TimeUnit.MILLISECONDS.toMinutes(timeInForeground).toInt(),
                    opens = appOpens
                )
            }
            
            dailyStats.add(DailyUsage(currentDate, appUsageMap))
            currentDate = currentDate.plusDays(1)
        }

        Log.d(TAG, "Processed ${dailyStats.size} days of usage data")
        return dailyStats
    }

    private fun queryUsageEvents(startTime: Long, endTime: Long): List<UsageEvents.Event> {
        val eventList = mutableListOf<UsageEvents.Event>()
        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val monitoredPackages = AppConfig.MONITORED_APPS.keys.toSet()
        
        var eventCount = 0
        while (usageEvents.hasNextEvent() && eventCount < 100000) {
            val event = UsageEvents.Event()
            usageEvents.getNextEvent(event)
            
            if (event.packageName in monitoredPackages && 
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                eventList.add(event)
            }
            eventCount++
        }
        
        return eventList
    }

    // ============================================================================
    // ENCRYPTED STORAGE
    // ============================================================================

    /**
     * Saves usage data to encrypted storage, merging with existing data.
     * @param isBackgroundSync If true, updates background sync time; otherwise updates manual sync time
     */
    fun saveUsageData(newData: List<DailyUsage>, isBackgroundSync: Boolean = false): Boolean {
        return try {
            val rawJson = encryptedPrefs.getString(KEY_USAGE_DATA, null)
            val existingData: List<DailyUsage> = if (rawJson.isNullOrEmpty()) {
                emptyList()
            } else {
                val parsed = try {
                    gson.fromJson(rawJson, StoredUsageData::class.java)?.dailyUsage
                } catch (e: Exception) {
                    Log.e(TAG, "Stored usage data failed to parse", e)
                    null
                }
                if (parsed == null) {
                    // The stored blob is unreadable. Back it up and ABORT the save rather than
                    // overwrite: a transient parse failure must never destroy accumulated history
                    // by replacing it with just the latest 9 days.
                    encryptedPrefs.edit { putString(KEY_USAGE_DATA_CORRUPT_BACKUP, rawJson) }
                    Log.e(TAG, "Aborting save to preserve unreadable history (backed up)")
                    return false
                }
                parsed
            }
            val mergedData = mergeUsageData(existingData, newData)

            val storageData = StoredUsageData(
                version = 2,
                lastUpdated = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                dailyUsage = mergedData
            )
            
            encryptedPrefs.edit {
                putString(KEY_USAGE_DATA, gson.toJson(storageData))
                putString(KEY_LAST_SYNC, LocalDateTime.now().toString())
                if (isBackgroundSync) {
                    putString(KEY_LAST_BACKGROUND_SYNC, LocalDateTime.now().toString())
                }
            }
            
            Log.d(TAG, "Saved ${mergedData.size} days of usage data")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving usage data", e)
            false
        }
    }

    /**
     * Retrieves all stored usage data.
     */
    fun getStoredUsageData(): List<DailyUsage> {
        return try {
            val json = encryptedPrefs.getString(KEY_USAGE_DATA, null)
            if (json.isNullOrEmpty()) return emptyList()
            
            val storedData = gson.fromJson(json, StoredUsageData::class.java)
            storedData.dailyUsage
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving usage data", e)
            emptyList()
        }
    }

    /**
     * Gets the last sync timestamp (manual or background).
     */
    fun getLastSyncTime(): LocalDateTime? {
        return try {
            encryptedPrefs.getString(KEY_LAST_SYNC, null)?.let {
                LocalDateTime.parse(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving last sync time", e)
            null
        }
    }
    
    /**
     * Gets the last background sync timestamp specifically.
     */
    fun getLastBackgroundSyncTime(): LocalDateTime? {
        return try {
            encryptedPrefs.getString(KEY_LAST_BACKGROUND_SYNC, null)?.let {
                LocalDateTime.parse(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving last background sync time", e)
            null
        }
    }

    /**
     * Gets statistics about stored data.
     */
    fun getStorageStatistics(): StorageStatistics {
        val data = getStoredUsageData()
        return if (data.isNotEmpty()) {
            StorageStatistics(
                totalDays = data.size,
                earliestDate = data.minByOrNull { it.date }?.date,
                latestDate = data.maxByOrNull { it.date }?.date,
                lastSync = getLastSyncTime()
            )
        } else {
            StorageStatistics(0, null, null, null)
        }
    }

    /**
     * Clears all stored data.
     */
    fun clearAllData() {
        try {
            encryptedPrefs.edit {
                remove(KEY_USAGE_DATA)
                remove(KEY_LAST_SYNC)
                remove(KEY_LAST_BACKGROUND_SYNC)
                remove(KEY_STUDY_ID)
            }
            Log.d(TAG, "All data cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing data", e)
        }
    }

    private fun mergeUsageData(
        existing: List<DailyUsage>,
        new: List<DailyUsage>
    ): List<DailyUsage> {
        val mergedMap = existing.associateBy { it.date }.toMutableMap()
        
        new.forEach { newDaily ->
            val existingDaily = mergedMap[newDaily.date]
            mergedMap[newDaily.date] = if (existingDaily == null) {
                newDaily
            } else {
                mergeDaily(existingDaily, newDaily)
            }
        }
        
        return mergedMap.values
            .sortedByDescending { it.date }
            .take(MAX_STORAGE_DAYS)
            .sortedBy { it.date }
    }
    
    private fun mergeDaily(existing: DailyUsage, new: DailyUsage): DailyUsage {
        if (existing == new) return existing
        
        val mergedAppUsage = existing.appUsage.toMutableMap()
        
        new.appUsage.forEach { (appName, appData) ->
            val currentData = mergedAppUsage[appName]
            mergedAppUsage[appName] = if (currentData == null) {
                appData
            } else {
                chooseMoreCompleteAppData(currentData, appData)
            }
        }
        
        return DailyUsage(existing.date, mergedAppUsage)
    }
    
    private fun chooseMoreCompleteAppData(
        existing: AppUsageData,
        new: AppUsageData
    ): AppUsageData {
        if (existing == new) return existing
        
        val existingScore = existing.timeMinutes * 100 + existing.opens.toInt() * 10
        val newScore = new.timeMinutes * 100 + new.opens.toInt() * 10
        
        return if (newScore > existingScore) new else existing
    }

    // ============================================================================
    // STUDY ID MANAGEMENT
    // ============================================================================

    /**
     * Gets or creates the anonymous study ID.
     *
     * Delegates to [StudyManager] so there is a SINGLE source of truth for the ID. Previously
     * DataStorage generated its own ID (used by CSV export) that differed from StudyManager's
     * (used for every REDCap submission), making exported data impossible to link to REDCap records.
     */
    fun getOrCreateStudyId(): String {
        return StudyManager(context).getOrCreateStudyId()
    }

    // ============================================================================
    // CSV EXPORT
    // ============================================================================

    /**
     * Generates CSV string from stored data with study ID.
     */
    fun generateCsvString(): String? {
        val dailyData = getStoredUsageData()
        if (dailyData.isEmpty()) {
            Log.w(TAG, "No data to export")
            return null
        }

        val studyId = getOrCreateStudyId()
        return generateCsvFromData(dailyData, studyId)
    }

    /**
     * Generates CSV from provided data.
     */
    fun generateCsvFromData(dailyData: List<DailyUsage>, studyId: String): String {
        if (dailyData.isEmpty()) return ""

        Log.d(TAG, "Generating CSV for ${dailyData.size} days")

        val csv = StringBuilder(dailyData.size * 400)
        val appPackages = AppConfig.MONITORED_APPS.keys.toList()
        val appNames = AppConfig.MONITORED_APPS.values

        // BOM for Excel UTF-8
        csv.append('\uFEFF')

        // Study ID
        csv.append("ID: ").append(studyId).append("\r\n")

        // Header
        csv.append("Data")
        appNames.forEach { appName ->
            csv.append(';').append(appName).append(" Tempo (min)")
            csv.append(';').append(appName).append(" Aperture")
        }
        csv.append("\r\n")

        // Data rows
        dailyData.sortedBy { it.date }.forEach { daily ->
            csv.append(daily.date.format(AppConfig.DATE_FORMATTER))

            appPackages.forEach { pkg ->
                val usage = daily.appUsage[pkg] ?: AppUsageData(0, 0)
                csv.append(';').append(usage.timeMinutes)
                csv.append(';').append(usage.opens)
            }

            csv.append("\r\n")
        }

        return csv.toString()
    }
}
