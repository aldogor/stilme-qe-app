package com.aldogor.stilme_qe_app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.aldogor.stilme_qe_app.databinding.ActivityMainBinding
import com.aldogor.stilme_qe_app.network.RedcapRepository
import com.aldogor.stilme_qe_app.onboarding.OnboardingActivity
import com.aldogor.stilme_qe_app.questionnaire.QuestionnaireActivity
import com.aldogor.stilme_qe_app.stl_guide.GroupInstructionsActivity
import com.aldogor.stilme_qe_app.study.RedcapResult
import com.aldogor.stilme_qe_app.study.StudyManager
import com.aldogor.stilme_qe_app.study.ThankYouActivity
import com.aldogor.stilme_qe_app.sync.QuestionnaireReminderWorker
import com.aldogor.stilme_qe_app.sync.SyncWorker
import com.aldogor.stilme_qe_app.update.UpdateChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Main activity for STILME-QE app.
 * Displays study status and provides access to questionnaires.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var permissionHelper: PermissionHelper
    private lateinit var dataStorage: DataStorage
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var studyManager: StudyManager
    private lateinit var redcapRepository: RedcapRepository

    private val questionnaireLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            notificationHelper.dismissQuestionnaireReminder()

            if (studyManager.isStudyComplete()) {
                startActivity(Intent(this, ThankYouActivity::class.java))
                finish()
            } else {
                MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.questionnaire_submission_success))
                    .setMessage(getString(R.string.questionnaire_see_you_next_month))
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                        updateStudyStatus()
                        updateQuestionnaireCard()
                        updateProgressIndicator()
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        permissionHelper = PermissionHelper(this)
        dataStorage = DataStorage(this)
        notificationHelper = NotificationHelper(this)
        studyManager = StudyManager(this)
        redcapRepository = RedcapRepository(this)

        checkStudyState()
    }

    private fun checkStudyState() {
        if (studyManager.isStudyComplete()) {
            startActivity(Intent(this, ThankYouActivity::class.java))
            finish()
            return
        }

        if (!studyManager.hasCompletedOnboarding()) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        setupUI()

        if (intent.getBooleanExtra("open_questionnaire", false)) {
            openQuestionnaire()
        }
    }

    private fun setupUI() {
        binding.buttonOpenQuestionnaire.setOnClickListener {
            openQuestionnaire()
        }

        // Debug buttons are hidden in production but can be re-enabled for testing.
        // To enable: set debug_buttons_container visibility to VISIBLE in activity_main.xml
        // or uncomment the lines below to enable programmatically in debug builds.
        // if (BuildConfig.DEBUG) {
        //     binding.debugButtonsContainer.visibility = View.VISIBLE
        // }
        binding.buttonExportCsv.setOnClickListener {
            exportCsvToClipboard()
        }

        binding.buttonDebugSkip.setOnClickListener {
            showDebugTimepointDialog()
        }

        binding.buttonContact.setOnClickListener {
            openEmailClient()
        }

        binding.buttonWithdraw.setOnClickListener {
            showWithdrawDialog()
        }

        binding.buttonViewInstructions.setOnClickListener {
            openGroupInstructions()
        }

        binding.buttonInvite.setOnClickListener {
            shareStudyLink()
        }

        binding.buttonMoreInfo.setOnClickListener {
            openStudyInfoLink()
        }

        ensureStudyIdGenerated()

        if (permissionHelper.hasUsageStatsPermission()) {
            ensureBackgroundWorkScheduled()
            scheduleQuestionnaireReminders()
            scheduleSyncWorker()
        }

        if (permissionHelper.needsNotificationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun scheduleQuestionnaireReminders() {
        QuestionnaireReminderWorker.scheduleDailyReminder(this)
    }

    private fun scheduleSyncWorker() {
        SyncWorker.schedulePeriodicSync(this)
    }

    private fun openQuestionnaire() {
        val timepoint = studyManager.getCurrentTimepoint()
        val intent = Intent(this, QuestionnaireActivity::class.java).apply {
            putExtra(QuestionnaireActivity.EXTRA_TIMEPOINT, timepoint.index)
        }
        questionnaireLauncher.launch(intent)
    }

    /**
     * Debug function: shows a dialog to jump forward in time for testing.
     * Allows testing questionnaire availability and notification escalation.
     */
    private fun showDebugTimepointDialog() {
        val state = studyManager.getParticipantState()
        if (state?.isStudyComplete == true) {
            Toast.makeText(this, R.string.debug_study_already_complete, Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_debug_time_jump, null)

        // Build status info
        val currentOffset = studyManager.getDebugDayOffset()
        val simulatedDate = studyManager.getSimulatedCurrentDate()
        val daysSinceWindow = studyManager.getDaysSinceWindowOpened()
        val currentTimepoint = studyManager.getCurrentTimepoint()

        val statusInfo = StringBuilder()
        statusInfo.append("Data simulata: $simulatedDate\n")
        statusInfo.append("Offset attuale: +$currentOffset giorni\n")
        statusInfo.append("Timepoint: ${currentTimepoint.displayName}\n")
        statusInfo.append("Giorni dalla finestra: $daysSinceWindow\n")
        if (studyManager.isQuestionnaireDue()) {
            statusInfo.append("Questionario: DISPONIBILE")
        } else {
            val daysUntil = studyManager.getDaysUntilWindowCloses()
            statusInfo.append("Prossimo questionario tra: $daysUntil giorni")
        }

        dialogView.findViewById<android.widget.TextView>(R.id.text_debug_status).text = statusInfo.toString()

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.debug_time_jump_title)
            .setView(dialogView)
            .setNegativeButton(R.string.cancel, null)
            .create()

        // Quick jump buttons
        dialogView.findViewById<android.view.View>(R.id.button_jump_1).setOnClickListener {
            dialog.dismiss()
            jumpDays(1)
        }
        dialogView.findViewById<android.view.View>(R.id.button_jump_7).setOnClickListener {
            dialog.dismiss()
            jumpDays(7)
        }
        dialogView.findViewById<android.view.View>(R.id.button_jump_14).setOnClickListener {
            dialog.dismiss()
            jumpDays(14)
        }
        dialogView.findViewById<android.view.View>(R.id.button_jump_30).setOnClickListener {
            dialog.dismiss()
            jumpDays(30)
        }

        // Custom jump button
        val editCustomDays = dialogView.findViewById<android.widget.EditText>(R.id.edit_custom_days)
        dialogView.findViewById<android.view.View>(R.id.button_jump_custom).setOnClickListener {
            val daysText = editCustomDays.text.toString()
            val days = daysText.toIntOrNull()
            if (days != null && days > 0) {
                dialog.dismiss()
                jumpDays(days)
            } else {
                Toast.makeText(this, "Inserisci un numero valido", Toast.LENGTH_SHORT).show()
            }
        }

        // Reset button
        dialogView.findViewById<android.view.View>(R.id.button_reset).setOnClickListener {
            dialog.dismiss()
            resetDebugOffset()
        }

        // Test notification button
        dialogView.findViewById<android.view.View>(R.id.button_test_notification).setOnClickListener {
            dialog.dismiss()
            triggerNotificationCheck()
        }

        dialog.show()
    }

    private fun triggerNotificationCheck() {
        QuestionnaireReminderWorker.triggerImmediateCheck(this)
        Toast.makeText(this, R.string.debug_notification_triggered, Toast.LENGTH_SHORT).show()
    }

    private fun jumpDays(days: Int) {
        studyManager.addDebugDays(days)

        val newOffset = studyManager.getDebugDayOffset()
        val simulatedDate = studyManager.getSimulatedCurrentDate()

        Toast.makeText(
            this,
            getString(R.string.debug_jumped_days, days, simulatedDate.toString()),
            Toast.LENGTH_LONG
        ).show()

        // Refresh UI to reflect new simulated date
        updateStudyStatus()
        updateQuestionnaireCard()
        updateProgressIndicator()
    }

    private fun resetDebugOffset() {
        studyManager.resetDebugDayOffset()

        Toast.makeText(
            this,
            getString(R.string.debug_offset_reset),
            Toast.LENGTH_SHORT
        ).show()

        // Refresh UI
        updateStudyStatus()
        updateQuestionnaireCard()
        updateProgressIndicator()
    }

    override fun onResume() {
        super.onResume()

        if (!::studyManager.isInitialized || !studyManager.hasCompletedOnboarding()) {
            return
        }

        // Check if mandatory permissions are still granted
        if (!permissionHelper.hasMandatoryPermissions()) {
            // Redirect to permission recovery screen
            startActivity(Intent(this, PermissionRecoveryActivity::class.java))
            return
        }

        updateStudyStatus()
        updateQuestionnaireCard()
        updateProgressIndicator()

        if (permissionHelper.hasUsageStatsPermission()) {
            ensureBackgroundWorkScheduled()
        }

        if (!studyManager.isQuestionnaireDue()) {
            notificationHelper.dismissQuestionnaireReminder()
        }

        // Check for app updates (throttled to once per day)
        lifecycleScope.launch {
            UpdateChecker.checkForUpdate(this@MainActivity) { downloadUrl, version ->
                UpdateChecker.showUpdateDialog(this@MainActivity, downloadUrl, version)
            }
        }
    }

    private fun updateStudyStatus() {
        val status = studyManager.getStatusSummary()
        binding.textStudyStatus.text = status
    }

    private fun updateQuestionnaireCard() {
        if (studyManager.isQuestionnaireDue()) {
            binding.cardQuestionnaire.visibility = View.VISIBLE
        } else {
            binding.cardQuestionnaire.visibility = View.GONE
        }
    }

    private fun ensureBackgroundWorkScheduled() {
        lifecycleScope.launch {
            try {
                if (!BackgroundScheduler.isWeeklyCollectionScheduled(this@MainActivity)) {
                    BackgroundScheduler.scheduleWeeklyCollection(this@MainActivity)

                    kotlinx.coroutines.delay(500)
                    if (BackgroundScheduler.isWeeklyCollectionScheduled(this@MainActivity)) {
                        withContext(Dispatchers.Main) {
                            markBackgroundWorkSetup()
                            Log.d(TAG, "Background work scheduled successfully")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling background work", e)
            }
        }
    }

    private fun markBackgroundWorkSetup() {
        getSharedPreferences("stilme_qe_prefs", MODE_PRIVATE).edit {
            putBoolean("background_work_scheduled", true)
            putLong("background_work_scheduled_time", System.currentTimeMillis())
        }
    }

    private fun ensureStudyIdGenerated() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val studyId = dataStorage.getOrCreateStudyId()
                Log.d(TAG, "Study ID: ${studyId.take(4)}***")
            } catch (e: Exception) {
                Log.e(TAG, "Error generating study ID", e)
            }
        }
    }

    private fun checkNotificationPermissions() {
        if (!permissionHelper.areNotificationsEnabled()) {
            val prefs = getSharedPreferences("stilme_qe_prefs", MODE_PRIVATE)
            val lastPrompt = prefs.getLong("last_notification_prompt", 0)

            if (System.currentTimeMillis() - lastPrompt > 24 * 60 * 60 * 1000) {
                showNotificationDialog()
                prefs.edit {
                    putLong("last_notification_prompt", System.currentTimeMillis())
                }
            }
        }
    }

    private fun showNotificationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.notification_permission_title))
            .setMessage(getString(R.string.notification_permission_message))
            .setPositiveButton(getString(R.string.enable_notifications)) { _, _ ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                    )
                } else {
                    startActivity(permissionHelper.getNotificationSettingsIntent())
                }
            }
            .setNegativeButton(getString(R.string.not_now), null)
            .setCancelable(false)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                if (permissionHelper.hasUsageStatsPermission()) {
                    ensureBackgroundWorkScheduled()
                }
            }
        }
    }

    /**
     * Debug function: exports the most up-to-date usage data to clipboard.
     * Fetches fresh data from UsageStats and merges with stored data.
     * Does NOT reset the 7-day background sync counter.
     */
    private fun exportCsvToClipboard() {
        lifecycleScope.launch {
            try {
                val usageData = withContext(Dispatchers.IO) {
                    // Fetch fresh data for last 9 days
                    val freshData = dataStorage.getUsageDataForLastNDays(9)

                    // Get stored data
                    val storedData = dataStorage.getStoredUsageData()

                    // Merge: fresh data takes priority for recent days
                    val mergedMap = storedData.associateBy { it.date }.toMutableMap()
                    freshData.forEach { daily ->
                        mergedMap[daily.date] = daily
                    }

                    mergedMap.values.sortedBy { it.date }
                }

                if (usageData.isNotEmpty()) {
                    val studyId = withContext(Dispatchers.IO) {
                        dataStorage.getOrCreateStudyId()
                    }

                    val csv = dataStorage.generateCsvFromData(usageData, studyId)

                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("STILME-QE Usage Data", csv)
                    clipboard.setPrimaryClip(clip)

                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.csv_exported, usageData.size),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.no_data_to_export),
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error exporting CSV", e)
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.csv_export_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // ========================================================================
    // GROUP INSTRUCTIONS
    // ========================================================================

    private fun openGroupInstructions() {
        val groupCode = studyManager.getGroup()
        val intent = Intent(this, GroupInstructionsActivity::class.java).apply {
            putExtra(GroupInstructionsActivity.EXTRA_GROUP_CODE, groupCode)
        }
        startActivity(intent)
    }

    // ========================================================================
    // SHARE STUDY LINK
    // ========================================================================

    private fun shareStudyLink() {
        val shareUrl = getString(R.string.welcome_info_url)
        val shareText = "Partecipa allo studio MIND TIME dell'Università di Torino! $shareUrl"

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    private fun openStudyInfoLink() {
        val url = getString(R.string.welcome_info_url)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    // ========================================================================
    // PROGRESS INDICATOR
    // ========================================================================

    private fun updateProgressIndicator() {
        val state = studyManager.getParticipantState() ?: return
        val completedCount = state.timepointsCompleted.size

        // Update progress bar (max is 5 timepoints: T0, M1, M2, M3, M4)
        binding.progressBar.progress = completedCount

        // Update progress label
        binding.textProgressLabel.text = when {
            state.isStudyComplete -> getString(R.string.study_progress_complete)
            completedCount == 0 -> getString(R.string.timepoint_baseline)
            else -> getString(R.string.study_progress_format, completedCount)
        }
    }

    // ========================================================================
    // CONTACT
    // ========================================================================

    private fun openEmailClient() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.contact_email)))
            putExtra(Intent.EXTRA_SUBJECT, "STILME-QE - Richiesta di assistenza")
        }
        try {
            startActivity(Intent.createChooser(intent, "Invia email"))
        } catch (e: Exception) {
            // If no email client available, copy email to clipboard
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Email", getString(R.string.contact_email))
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Indirizzo email copiato negli appunti", Toast.LENGTH_LONG).show()
        }
    }

    // ========================================================================
    // STUDY WITHDRAWAL
    // ========================================================================

    private var isWithdrawStep2 = false

    private fun showWithdrawDialog() {
        isWithdrawStep2 = false
        binding.textWithdrawTitle.text = getString(R.string.withdraw_dialog_title)
        binding.textWithdrawMessage.text = getString(R.string.withdraw_dialog_message)
        binding.layoutWithdrawInput.visibility = View.GONE
        binding.editWithdrawConfirm.text?.clear()
        binding.buttonWithdrawConfirm.text = getString(R.string.withdraw_confirm_step1)
        binding.withdrawOverlay.visibility = View.VISIBLE

        binding.buttonWithdrawCancel.setOnClickListener {
            hideWithdrawOverlay()
        }

        binding.buttonWithdrawConfirm.setOnClickListener {
            if (!isWithdrawStep2) {
                // Move to step 2
                showWithdrawStep2()
            } else {
                // Validate and perform withdrawal
                val input = binding.editWithdrawConfirm.text.toString().trim().uppercase()
                if (input == getString(R.string.withdraw_confirm_word)) {
                    performWithdrawal()
                } else {
                    binding.layoutWithdrawInput.error = getString(R.string.withdraw_wrong_word)
                }
            }
        }
    }

    private fun showWithdrawStep2() {
        isWithdrawStep2 = true
        binding.textWithdrawTitle.text = getString(R.string.withdraw_confirm_step2_title)
        binding.textWithdrawMessage.text = getString(R.string.withdraw_confirm_step2_message)
        binding.layoutWithdrawInput.visibility = View.VISIBLE
        binding.layoutWithdrawInput.error = null
        binding.buttonWithdrawConfirm.text = getString(R.string.withdraw_final_confirm)
        binding.editWithdrawConfirm.requestFocus()
    }

    private fun hideWithdrawOverlay() {
        binding.withdrawOverlay.visibility = View.GONE
        binding.withdrawLoadingOverlay.visibility = View.GONE
        isWithdrawStep2 = false
    }

    private fun performWithdrawal() {
        binding.withdrawOverlay.visibility = View.GONE
        binding.withdrawLoadingOverlay.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val studyId = studyManager.getStudyId()

                if (studyId != null) {
                    // Mark as withdrawn in REDCap (leaves a trace for audit trail)
                    val result = withContext(Dispatchers.IO) {
                        redcapRepository.markAsWithdrawn(studyId)
                    }

                    when (result) {
                        is RedcapResult.Success -> {
                            Log.d(TAG, "REDCap record marked as withdrawn successfully")
                        }
                        is RedcapResult.NetworkError -> {
                            Log.w(TAG, "Could not mark as withdrawn in REDCap (offline), continuing with local deletion")
                        }
                        is RedcapResult.ServerError -> {
                            Log.w(TAG, "REDCap withdrawal marking failed: ${result.message}")
                        }
                        is RedcapResult.ParseError -> {
                            Log.w(TAG, "REDCap parse error: ${result.message}")
                        }
                    }
                }

                // Clear all local data regardless of REDCap result
                withContext(Dispatchers.IO) {
                    studyManager.clearAllData()
                    dataStorage.clearAllData()
                }

                binding.withdrawLoadingOverlay.visibility = View.GONE

                // Show success message using Material3 dialog
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle(getString(R.string.withdraw_success_title))
                    .setMessage(getString(R.string.withdraw_success_message))
                    .setPositiveButton("OK") { _, _ ->
                        finishAffinity()
                    }
                    .setCancelable(false)
                    .show()

            } catch (e: Exception) {
                Log.e(TAG, "Error during withdrawal", e)
                binding.withdrawLoadingOverlay.visibility = View.GONE
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.withdraw_error),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

}
