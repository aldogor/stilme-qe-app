package com.aldogor.stilme_qe_app

import android.content.Context
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Background operations for STILME-QE Usage Monitor.
 */

// ============================================================================
// WORKER IMPLEMENTATION
// ============================================================================

/**
 * Background worker that collects usage statistics data weekly.
 * Runs approximately once a week to ensure continuous data collection for the study.
 */
class UsageDataWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "UsageDataWorker"
        const val WORK_NAME = "weekly_usage_sync"
        private const val COLLECTION_DAYS = 9 // Android keeps 10 days including today

        // Worker input/output keys
        const val KEY_FORCE_SYNC = "force_sync"
        const val KEY_DAYS_COLLECTED = "days_collected"
        const val KEY_TOTAL_DAYS_STORED = "total_days_stored"
        const val KEY_ERROR_MESSAGE = "error_message"
    }

    private val dataStorage = DataStorage(applicationContext)

    /**
     * Main work execution - collects and saves usage data silently.
     */
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting usage data collection")

            // Check if we should run (unless forced)
            val forceSync = inputData.getBoolean(KEY_FORCE_SYNC, false)
            if (!forceSync && !shouldCollectData()) {
                Log.d(TAG, "Skipping - not enough time since last sync")
                return@withContext Result.success()
            }

            // Collect data silently (no notifications)
            val result = collectAndSaveData()

            // Handle result
            when (result) {
                is CollectionResult.Success -> {
                    Log.d(TAG, "Collected ${result.daysCollected} days, total: ${result.totalDaysStored}")

                    // Ensure next run is scheduled
                    BackgroundScheduler.ensureScheduled(applicationContext)

                    Result.success(
                        Data.Builder()
                            .putInt(KEY_DAYS_COLLECTED, result.daysCollected)
                            .putInt(KEY_TOTAL_DAYS_STORED, result.totalDaysStored)
                            .build()
                    )
                }

                is CollectionResult.PermissionDenied -> {
                    Log.e(TAG, "Permission denied")
                    Result.failure(
                        Data.Builder()
                            .putString(KEY_ERROR_MESSAGE, result.message)
                            .build()
                    )
                }

                is CollectionResult.NoData -> {
                    Log.w(TAG, "No data available")
                    Result.success()
                }

                is CollectionResult.Error -> {
                    Log.e(TAG, "Collection error: ${result.message}")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error", e)
            Result.retry()
        }
    }

    /**
     * Collects and saves usage data to encrypted storage.
     */
    private fun collectAndSaveData(): CollectionResult {
        return try {
            // Collect last 9 complete days
            val freshData = dataStorage.getUsageDataForLastNDays(COLLECTION_DAYS)

            if (freshData.isEmpty()) {
                return CollectionResult.NoData("No usage data found")
            }

            // Save to storage (handles merging) - mark as background sync
            if (dataStorage.saveUsageData(freshData, isBackgroundSync = true)) {
                val stats = dataStorage.getStorageStatistics()
                CollectionResult.Success(
                    daysCollected = freshData.size,
                    totalDaysStored = stats.totalDays,
                    dateRange = Pair(
                        stats.earliestDate ?: LocalDate.now(),
                        stats.latestDate ?: LocalDate.now()
                    )
                )
            } else {
                CollectionResult.Error("Failed to save data")
            }
        } catch (_: SecurityException) {
            CollectionResult.PermissionDenied("Usage stats permission denied")
        } catch (e: Exception) {
            CollectionResult.Error("Collection error: ${e.localizedMessage}")
        }
    }

    /**
     * Checks if enough time has passed for collection (weekly).
     * Uses background sync time to avoid interference from manual extractions.
     */
    private fun shouldCollectData(): Boolean {
        val lastBackgroundSync = dataStorage.getLastBackgroundSyncTime()
        return lastBackgroundSync?.let {
            val daysSinceLastSync = java.time.Duration.between(it, LocalDateTime.now()).toDays()
            Log.d(TAG, "Days since last background sync: $daysSinceLastSync")
            daysSinceLastSync >= 7
        } ?: true // First run - always collect
    }
}

// ============================================================================
// SCHEDULER
// ============================================================================

/**
 * Manages scheduling of background usage data collection.
 */
object BackgroundScheduler {

    private const val TAG = "BackgroundScheduler"

    /**
     * Schedules weekly data collection work.
     * This method is idempotent - safe to call multiple times.
     */
    fun scheduleWeeklyCollection(context: Context, replace: Boolean = false) {
        Log.d(TAG, "Scheduling weekly collection (replace: $replace)")

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<UsageDataWorker>(
            7, TimeUnit.DAYS,  // Repeat every 7 days
            1, TimeUnit.HOURS  // Flex interval
        )
            .setConstraints(constraints)
            .addTag("usage_collection")
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30, TimeUnit.MINUTES
            )
            .build()

        val policy = if (replace) {
            ExistingPeriodicWorkPolicy.UPDATE
        } else {
            ExistingPeriodicWorkPolicy.KEEP
        }

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UsageDataWorker.WORK_NAME,
            policy,
            workRequest
        )

        Log.d(TAG, "Weekly collection scheduled")
    }

    /**
     * Cancels scheduled collection work.
     */
    fun cancelWeeklyCollection(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UsageDataWorker.WORK_NAME)
        Log.d(TAG, "Weekly collection cancelled")
    }

    /**
     * Checks if weekly collection is scheduled.
     */
    fun isWeeklyCollectionScheduled(context: Context): Boolean {
        return try {
            val workInfos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(UsageDataWorker.WORK_NAME)
                .get()

            workInfos.any { info ->
                info.state in listOf(
                    WorkInfo.State.ENQUEUED,
                    WorkInfo.State.RUNNING,
                    WorkInfo.State.BLOCKED
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking work status", e)
            false
        }
    }

    /**
     * Forces immediate data collection (for testing or manual sync).
     */
    fun forceImmediateCollection(context: Context): UUID {
        Log.d(TAG, "Forcing immediate collection")

        val workRequest = OneTimeWorkRequestBuilder<UsageDataWorker>()
            .setInputData(
                Data.Builder()
                    .putBoolean(UsageDataWorker.KEY_FORCE_SYNC, true)
                    .build()
            )
            .addTag("usage_collection")
            .addTag("forced_sync")
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)

        return workRequest.id
    }

    /**
     * Ensures weekly collection is scheduled if not already.
     */
    fun ensureScheduled(context: Context) {
        if (!isWeeklyCollectionScheduled(context)) {
            Log.d(TAG, "Scheduling missing weekly collection")
            scheduleWeeklyCollection(context)
        }
    }

    /**
     * Gets current work status for debugging.
     */
    fun getWorkInfo(context: Context): List<WorkInfo> {
        return try {
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(UsageDataWorker.WORK_NAME)
                .get()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting work info", e)
            emptyList()
        }
    }
}

