package com.aldogor.stilme_qe_app.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.aldogor.stilme_qe_app.NotificationHelper
import com.aldogor.stilme_qe_app.network.RedcapRepository
import com.aldogor.stilme_qe_app.study.RedcapResult
import com.aldogor.stilme_qe_app.study.StudyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Worker for synchronizing queued submissions to REDCap.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val submissionRepository = SubmissionRepository(context)
    private val redcapRepository = RedcapRepository(context)
    private val studyManager = StudyManager(context)

    companion object {
        private const val TAG = "SyncWorker"
        private const val MAX_RETRIES = 5
        private const val UNIQUE_WORK_NAME = "redcap_sync"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                1, TimeUnit.HOURS,
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30, TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )

            Log.d(TAG, "Periodic sync scheduled")
        }

        fun triggerImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(request)
            Log.d(TAG, "Immediate sync triggered")
        }

        fun cancelSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
            Log.d(TAG, "Sync cancelled")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "SyncWorker starting")

        if (studyManager.isStudyComplete()) {
            Log.d(TAG, "Study complete, skipping sync")
            return Result.success()
        }

        return withContext(Dispatchers.IO) {
            try {
                syncPendingSubmissions()
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed", e)
                Result.retry()
            }
        }
    }

    private suspend fun syncPendingSubmissions(): Result {
        // Recover any rows orphaned in SUBMITTING by a previously-killed worker run,
        // otherwise they would never be retried and the questionnaire would be lost.
        submissionRepository.resetStuckSubmitting()

        val pending = submissionRepository.getPendingAndFailed()

        if (pending.isEmpty()) {
            Log.d(TAG, "No pending submissions")
            return Result.success()
        }

        Log.d(TAG, "Found ${pending.size} pending submissions")

        var hasNetworkError = false

        for (submission in pending) {
            if (submission.retryCount >= MAX_RETRIES) {
                // Do NOT delete: preserve the payload as EXPIRED so the data is recoverable.
                Log.e(TAG, "Submission ${submission.id} exceeded max retries; marking EXPIRED (data kept)")
                submissionRepository.markExpired(submission.id)
                continue
            }

            submissionRepository.markSubmitting(submission.id)

            val result = redcapRepository.submitRecord(submission.payload)

            when (result) {
                is RedcapResult.Success -> {
                    Log.d(TAG, "Submission ${submission.id} succeeded")
                    submissionRepository.markSuccess(submission.id)
                }
                is RedcapResult.NetworkError -> {
                    Log.w(TAG, "Network error for submission ${submission.id}: ${result.message}")
                    submissionRepository.markFailed(submission.id, result.message)
                    hasNetworkError = true
                }
                is RedcapResult.ServerError -> {
                    Log.e(TAG, "Server error for submission ${submission.id}: ${result.code} - ${result.message}")
                    submissionRepository.markFailed(submission.id, "Server error: ${result.code}")
                }
                is RedcapResult.ParseError -> {
                    Log.e(TAG, "Parse error for submission ${submission.id}: ${result.message}")
                    submissionRepository.markFailed(submission.id, result.message)
                }
            }
        }

        return if (hasNetworkError) Result.retry() else Result.success()
    }
}

/**
 * Worker for checking and sending questionnaire reminders.
 */
class QuestionnaireReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val studyManager = StudyManager(context)
    private val notificationHelper = NotificationHelper(context)

    companion object {
        private const val TAG = "QuestionnaireReminderWorker"
        private const val UNIQUE_WORK_NAME = "questionnaire_reminder"
        private const val INVITE_WORK_NAME = "invite_friend_reminder"

        fun scheduleDailyReminder(context: Context) {
            val request = PeriodicWorkRequestBuilder<QuestionnaireReminderWorker>(
                1, TimeUnit.DAYS,
                1, TimeUnit.HOURS
            )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )

            Log.d(TAG, "Daily reminder scheduled")
        }

        fun cancelReminders(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
            Log.d(TAG, "Reminders cancelled")
        }

        /**
         * Debug function: immediately triggers the reminder check.
         * Use this when testing with debug day offset.
         */
        fun triggerImmediateCheck(context: Context) {
            val request = OneTimeWorkRequestBuilder<QuestionnaireReminderWorker>()
                .build()

            WorkManager.getInstance(context).enqueue(request)
            Log.d(TAG, "Immediate reminder check triggered")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "QuestionnaireReminderWorker starting")

        if (studyManager.isStudyComplete()) {
            Log.d(TAG, "Study complete, cancelling reminders")
            notificationHelper.dismissQuestionnaireReminder()
            return Result.success()
        }

        if (studyManager.isQuestionnaireDue()) {
            // Calculate days since questionnaire window opened
            val daysSinceWindowOpened = studyManager.getDaysSinceWindowOpened()

            Log.d(TAG, "Questionnaire due, days since window opened: $daysSinceWindowOpened")

            notificationHelper.showQuestionnaireReminder(daysSinceWindowOpened)

            // Intentionally does NOT write participant state here. The pendingQuestionnaire flag is
            // never read for logic, and writing it from this daily worker could clobber a concurrent
            // state update from the questionnaire flow (e.g. a just-completed timepoint).
        } else {
            Log.d(TAG, "No questionnaire due")
            notificationHelper.dismissQuestionnaireReminder()

            // Check if we should show invite friend notification (5+ days after completion, once per timepoint)
            val daysSinceLastCompletion = studyManager.getDaysSinceLastCompletion()
            val currentTimepointIndex = studyManager.getCurrentTimepoint().index
            if (daysSinceLastCompletion >= 5 && !studyManager.hasInviteBeenShownForTimepoint(currentTimepointIndex)) {
                Log.d(TAG, "Showing invite friend notification")
                notificationHelper.showInviteFriendNotification()
                studyManager.markInviteShownForTimepoint(currentTimepointIndex)
            }
        }

        return Result.success()
    }
}
