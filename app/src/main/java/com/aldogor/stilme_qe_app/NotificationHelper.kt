package com.aldogor.stilme_qe_app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Helper class for managing notifications during background data collection.
 * 
 * @property context Application context for notification operations
 */
class NotificationHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "NotificationHelper"

        // Notification channel constants - separate channels for different purposes
        private const val CHANNEL_COLLECTION_ID = "usage_data_collection"
        private const val CHANNEL_COLLECTION_NAME = "Raccolta Dati"
        private const val CHANNEL_COLLECTION_DESCRIPTION =
            "Notifiche sulla raccolta automatica dei dati di utilizzo"

        private const val CHANNEL_QUESTIONNAIRE_ID = "questionnaire_reminders"
        private const val CHANNEL_QUESTIONNAIRE_NAME = "Promemoria Questionari"
        private const val CHANNEL_QUESTIONNAIRE_DESCRIPTION =
            "Promemoria per completare i questionari mensili dello studio"

        // Notification IDs
        const val COLLECTION_NOTIFICATION_ID = 1001
        const val COLLECTION_COMPLETE_ID = 1002
        const val BACKGROUND_STARTED_ID = 1003
        const val QUESTIONNAIRE_REMINDER_ID = 1004
        const val INVITE_FRIEND_ID = 1005

        // Notification content
        private const val NOTIFICATION_TITLE = "Studio MIND TIME"
        private const val NOTIFICATION_TEXT_COLLECTING =
            "Raccolta dati di utilizzo in corso. I dati rimangono sul tuo dispositivo."
        private const val NOTIFICATION_TEXT_COMPLETE =
            "Raccolta dati completata. I dati sono salvati in modo sicuro sul tuo dispositivo."
        private const val NOTIFICATION_TEXT_ERROR =
            "Errore durante la raccolta dati. Riproveremo più tardi."
        private const val NOTIFICATION_TEXT_BACKGROUND_STARTED =
            "La raccolta automatica dei dati è stata programmata con successo"
    }
    
    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Creates notification channels.
     * This must be called before showing any notifications.
     */
    private fun createNotificationChannel() {
        // minSdk is 29 (Android 10), so notification channels are always required

        // Channel for data collection notifications (low priority, silent)
        val collectionChannel = NotificationChannel(
            CHANNEL_COLLECTION_ID,
            CHANNEL_COLLECTION_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = CHANNEL_COLLECTION_DESCRIPTION
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
        }

        // Channel for questionnaire reminders (default priority, can alert)
        val questionnaireChannel = NotificationChannel(
            CHANNEL_QUESTIONNAIRE_ID,
            CHANNEL_QUESTIONNAIRE_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = CHANNEL_QUESTIONNAIRE_DESCRIPTION
            setShowBadge(true)
            enableLights(true)
            enableVibration(true)
        }

        notificationManager.createNotificationChannel(collectionChannel)
        notificationManager.createNotificationChannel(questionnaireChannel)
        Log.d(TAG, "Notification channels created")
    }
    
    /**
     * Shows a notification indicating that data collection is in progress.
     * This notification is non-dismissible and shows a progress indicator.
     * 
     * @param showProgress If true, shows an indeterminate progress bar
     * @return The notification ID for reference
     */
    fun showCollectionNotification(showProgress: Boolean = true): Int {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_COLLECTION_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details) // Using system icon
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText(NOTIFICATION_TEXT_COLLECTING)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(NOTIFICATION_TEXT_COLLECTING))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Cannot be dismissed by user
            .setSilent(true) // No sound
            .apply {
                if (showProgress) {
                    setProgress(0, 0, true) // Indeterminate progress
                }
            }
            .build()
        
        try {
            notificationManager.notify(COLLECTION_NOTIFICATION_ID, notification)
            Log.d(TAG, "Collection notification shown")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception showing notification", e)
        }
        
        return COLLECTION_NOTIFICATION_ID
    }
    
    /**
     * Shows a notification indicating that data collection completed successfully.
     * This notification is dismissible and disappears after a short time.
     */
    fun showCollectionCompleteNotification() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_COLLECTION_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText(NOTIFICATION_TEXT_COMPLETE)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(NOTIFICATION_TEXT_COMPLETE))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Dismiss when tapped
            .setSilent(true)
            // No timeout - user must dismiss manually
            .build()
        
        try {
            // First dismiss the progress notification
            dismissNotification(COLLECTION_NOTIFICATION_ID)
            
            // Show completion notification
            notificationManager.notify(COLLECTION_COMPLETE_ID, notification)
            Log.d(TAG, "Collection complete notification shown")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception showing notification", e)
        }
    }
    
    /**
     * Shows an error notification if collection fails.
     * This helps maintain transparency even when things go wrong.
     */
    fun showCollectionErrorNotification() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_COLLECTION_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText(NOTIFICATION_TEXT_ERROR)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(NOTIFICATION_TEXT_ERROR))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Slightly higher for errors
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        try {
            // Dismiss any ongoing notification
            dismissNotification(COLLECTION_NOTIFICATION_ID)
            
            // Show error notification
            notificationManager.notify(COLLECTION_COMPLETE_ID, notification)
            Log.d(TAG, "Collection error notification shown")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception showing notification", e)
        }
    }
    
    /**
     * Dismisses a specific notification.
     * 
     * @param notificationId The ID of the notification to dismiss
     */
    fun dismissNotification(notificationId: Int) {
        try {
            notificationManager.cancel(notificationId)
            Log.d(TAG, "Notification dismissed: $notificationId")
        } catch (e: Exception) {
            Log.e(TAG, "Error dismissing notification", e)
        }
    }
    
    /**
     * Dismisses all notifications from this app.
     */
    fun dismissAllNotifications() {
        try {
            notificationManager.cancelAll()
            Log.d(TAG, "All notifications dismissed")
        } catch (e: Exception) {
            Log.e(TAG, "Error dismissing all notifications", e)
        }
    }
    
    /**
     * Shows a notification indicating that background work has been scheduled.
     * This is shown when background collection is started for the first time
     * or restarted after being stopped.
     */
    fun showBackgroundWorkStartedNotification() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_COLLECTION_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText(NOTIFICATION_TEXT_BACKGROUND_STARTED)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(NOTIFICATION_TEXT_BACKGROUND_STARTED))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Default priority - appears in drawer
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Dismissible by user
            .setSilent(true) // No sound
            .build()
        
        try {
            notificationManager.notify(BACKGROUND_STARTED_ID, notification)
            Log.d(TAG, "Background work started notification shown")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception showing notification", e)
        }
    }
    
    /**
     * Checks if notifications are enabled for the app.
     *
     * @return True if notifications are enabled, false otherwise
     */
    fun areNotificationsEnabled(): Boolean {
        return notificationManager.areNotificationsEnabled()
    }

    // ========================================================================
    // QUESTIONNAIRE REMINDER NOTIFICATIONS
    // ========================================================================

    /**
     * Notification type based on days since questionnaire became available.
     */
    enum class ReminderType {
        INITIAL,           // Day 0-2: "È il momento di MIND TIME"
        FORGOT,            // Day 3-10: "5 minuti per il questionario..."
        COMPLETELY_FORGOT  // Day 10-30: "Tutto, ma non il ghosting"
    }

    /**
     * Shows a questionnaire reminder notification based on days since window opened.
     *
     * @param daysSinceWindowOpened Days since the questionnaire window opened
     */
    fun showQuestionnaireReminder(daysSinceWindowOpened: Int) {
        val reminderType = when {
            daysSinceWindowOpened <= 2 -> ReminderType.INITIAL
            daysSinceWindowOpened <= 10 -> ReminderType.FORGOT
            else -> ReminderType.COMPLETELY_FORGOT
        }

        val (title, message) = when (reminderType) {
            ReminderType.INITIAL -> Pair(
                context.getString(R.string.notification_initial_title),
                context.getString(R.string.notification_initial_message)
            )
            ReminderType.FORGOT -> Pair(
                context.getString(R.string.notification_forgot_title),
                context.getString(R.string.notification_forgot_message)
            )
            ReminderType.COMPLETELY_FORGOT -> Pair(
                context.getString(R.string.notification_completely_forgot_title),
                context.getString(R.string.notification_completely_forgot_message)
            )
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_questionnaire", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            QUESTIONNAIRE_REMINDER_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Higher priority for urgent reminders (day 10+)
        val priority = if (reminderType == ReminderType.COMPLETELY_FORGOT) {
            NotificationCompat.PRIORITY_HIGH
        } else {
            NotificationCompat.PRIORITY_DEFAULT
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_QUESTIONNAIRE_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Cannot be dismissed by user
            .setAutoCancel(false)
            .build()

        try {
            notificationManager.notify(QUESTIONNAIRE_REMINDER_ID, notification)
            Log.d(TAG, "Questionnaire reminder shown: $reminderType (day $daysSinceWindowOpened)")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception showing questionnaire reminder", e)
        }
    }

    /**
     * Shows a notification inviting the user to share the study with friends.
     * This is shown 5 days after completing a questionnaire.
     */
    fun showInviteFriendNotification() {
        val title = context.getString(R.string.notification_invite_title)
        val message = context.getString(R.string.notification_invite_message)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            INVITE_FRIEND_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_QUESTIONNAIRE_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Dismissible
            .build()

        try {
            notificationManager.notify(INVITE_FRIEND_ID, notification)
            Log.d(TAG, "Invite friend notification shown")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception showing invite friend notification", e)
        }
    }

    /**
     * Dismisses the questionnaire reminder notification.
     */
    fun dismissQuestionnaireReminder() {
        dismissNotification(QUESTIONNAIRE_REMINDER_ID)
    }

    /**
     * Dismisses the invite friend notification.
     */
    fun dismissInviteFriendNotification() {
        dismissNotification(INVITE_FRIEND_ID)
    }
}
