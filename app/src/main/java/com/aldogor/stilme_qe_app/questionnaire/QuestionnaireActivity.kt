package com.aldogor.stilme_qe_app.questionnaire

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.aldogor.stilme_qe_app.BuildConfig
import com.aldogor.stilme_qe_app.DataStorage
import com.aldogor.stilme_qe_app.R
import com.aldogor.stilme_qe_app.databinding.ActivityQuestionnaireBinding
import com.aldogor.stilme_qe_app.network.RedcapRepository
import com.aldogor.stilme_qe_app.study.*
import com.aldogor.stilme_qe_app.sync.SubmissionRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Activity for displaying and collecting questionnaire responses.
 * Shows all questions in a single scrollable view with a progress bar.
 */
class QuestionnaireActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuestionnaireBinding
    private lateinit var studyManager: StudyManager
    private lateinit var dataStorage: DataStorage
    private lateinit var submissionRepository: SubmissionRepository
    private lateinit var redcapRepository: RedcapRepository

    private lateinit var adapter: QuestionAdapter
    private lateinit var responses: QuestionnaireResponses
    private lateinit var timepoint: Timepoint

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display (required for Android 15+)
        enableEdgeToEdge()

        binding = ActivityQuestionnaireBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle window insets for edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        studyManager = StudyManager(this)
        dataStorage = DataStorage(this)
        submissionRepository = SubmissionRepository(this)
        redcapRepository = RedcapRepository(this)

        val timepointIndex = intent.getIntExtra(EXTRA_TIMEPOINT, 0)
        timepoint = Timepoint.fromIndex(timepointIndex) ?: Timepoint.T0

        setupQuestionnaire()
        setupRecyclerView()
        setupSubmitButton()
        setupBackPressHandler()
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                MaterialAlertDialogBuilder(this@QuestionnaireActivity)
                    .setTitle(getString(R.string.exit_questionnaire_title))
                    .setMessage(getString(R.string.exit_questionnaire_message))
                    .setPositiveButton(getString(R.string.exit_questionnaire_confirm)) { _, _ ->
                        setResult(RESULT_CANCELED)
                        finish()
                    }
                    .setNegativeButton(getString(R.string.exit_questionnaire_cancel), null)
                    .show()
            }
        })
    }

    private fun setupQuestionnaire() {
        val studyId = studyManager.getOrCreateStudyId()
        responses = QuestionnaireResponses(
            studyId = studyId,
            timepoint = timepoint
        )
    }

    private fun setupRecyclerView() {
        adapter = QuestionAdapter(
            responses = responses,
            onResponseChanged = { updateProgress() },
            onEligibilityCheck = { variableName, value -> checkEligibilityScreening(variableName, value) }
        )

        binding.recyclerQuestions.apply {
            layoutManager = LinearLayoutManager(this@QuestionnaireActivity)
            adapter = this@QuestionnaireActivity.adapter
            itemAnimator = null // Disable animations for smoother updates
        }

        // Load questions
        val questions = if (timepoint == Timepoint.T0) {
            QuestionnaireData.getBaselineQuestions()
        } else {
            QuestionnaireData.getMonthlyQuestions(timepoint, studyManager.getGroup())
        }

        adapter.setQuestions(questions)
        updateProgress()
    }

    private fun setupSubmitButton() {
        binding.buttonSubmit.setOnClickListener {
            if (adapter.areAllRequiredAnswered()) {
                onQuestionnaireComplete()
            } else {
                // Show validation message and scroll to first unanswered
                binding.textValidation.visibility = View.VISIBLE
                val position = adapter.getFirstUnansweredPosition()
                if (position >= 0) {
                    binding.recyclerQuestions.smoothScrollToPosition(position)
                }
            }
        }
    }

    private fun updateProgress() {
        val answered = adapter.getAnsweredCount()
        val total = adapter.getVisibleQuestionCount()
        val progress = if (total > 0) (answered * 100) / total else 0

        binding.progressBar.progress = progress
        binding.textProgressPercent.text = "$progress%"
        binding.textProgress.text = getString(R.string.questions_answered_format, answered, total)

        // Enable submit button only when all required are answered
        val allRequiredAnswered = adapter.areAllRequiredAnswered()
        binding.buttonSubmit.isEnabled = allRequiredAnswered

        // Hide validation message when all required are answered
        if (allRequiredAnswered) {
            binding.textValidation.visibility = View.GONE
        }
    }

    private fun checkEligibilityScreening(variableName: String, value: Int) {
        when (variableName) {
            "is_unito_student" -> {
                if (value == 0) {
                    showIneligibleDialog(
                        getString(R.string.ineligible_title),
                        getString(R.string.ineligible_reason_not_unito),
                        REASON_NOT_UNITO
                    )
                }
            }
            "age_group" -> {
                if (value == 99) {
                    showIneligibleDialog(
                        getString(R.string.ineligible_title),
                        getString(R.string.ineligible_reason_age),
                        REASON_AGE
                    )
                }
            }
        }
    }

    private fun showIneligibleDialog(title: String, message: String, reasonCode: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok_button)) { _, _ ->
                studyManager.setEligibility(false)
                studyManager.setGroup(StudyGroup.INELIGIBLE_SCREENING.code)
                val resultIntent = android.content.Intent().apply {
                    putExtra(EXTRA_INELIGIBLE_REASON, reasonCode)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun onQuestionnaireComplete() {
        responses.completedAt = LocalDateTime.now()

        // Calculate scores
        val isMonthly = timepoint != Timepoint.T0
        val scores = ScoringEngine.calculateScores(responses, isMonthly)

        // Calculate group assignment for baseline
        if (timepoint == Timepoint.T0) {
            val group = ScoringEngine.calculateGroup(responses)
            studyManager.setGroup(group)

            if (!ScoringEngine.isEligible(responses)) {
                studyManager.setEligibility(false)
            }
        }

        // Show confirmation dialog before submission
        showDataExtractionConfirmation(scores)
    }

    private fun showDataExtractionConfirmation(scores: ScaleScores) {
        lifecycleScope.launch {
            // First, collect fresh usage data if we have permission
            withContext(Dispatchers.IO) {
                try {
                    // Collect the last 9 days of usage data now
                    val freshData = dataStorage.getUsageDataForLastNDays(9)
                    if (freshData.isNotEmpty()) {
                        dataStorage.saveUsageData(freshData, isBackgroundSync = false)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("QuestionnaireActivity", "Error collecting usage data", e)
                }
            }

            // Show integrated confirmation card instead of floating dialog
            binding.textConfirmationMessage.text = getString(R.string.confirm_data_extraction_message)
            binding.confirmationOverlay.visibility = View.VISIBLE

            binding.buttonCancelSend.setOnClickListener {
                binding.confirmationOverlay.visibility = View.GONE
            }

            binding.buttonConfirmSend.setOnClickListener {
                binding.confirmationOverlay.visibility = View.GONE
                lifecycleScope.launch {
                    submitData(scores)
                }
            }
        }
    }

    private suspend fun submitData(scores: ScaleScores) {
        // Show loading overlay
        binding.loadingOverlay.visibility = View.VISIBLE
        binding.buttonSubmit.isEnabled = false

        // Get usage data
        val usageData = withContext(Dispatchers.IO) {
            dataStorage.getStoredUsageData()
        }

        // Build payload
        val payload = buildPayload(scores, usageData)

        // Try to submit to REDCap
        val result = withContext(Dispatchers.IO) {
            redcapRepository.submitRecord(payload)
        }

        when (result) {
            is RedcapResult.Success -> {
                studyManager.markTimepointComplete(timepoint)
                showSuccessDialog()
            }
            is RedcapResult.NetworkError -> {
                // Queue for later
                withContext(Dispatchers.IO) {
                    submissionRepository.enqueue(
                        studyId = responses.studyId,
                        eventName = timepoint.redcapEventName,
                        payload = payload
                    )
                }
                studyManager.markTimepointComplete(timepoint)
                showSuccessDialog(offlineNote = true)
            }
            is RedcapResult.ServerError, is RedcapResult.ParseError -> {
                // Queue for later and show error
                withContext(Dispatchers.IO) {
                    submissionRepository.enqueue(
                        studyId = responses.studyId,
                        eventName = timepoint.redcapEventName,
                        payload = payload
                    )
                }
                studyManager.markTimepointComplete(timepoint)
                showSuccessDialog(offlineNote = true)
            }
        }
    }

    private fun showSuccessDialog(offlineNote: Boolean = false) {
        binding.loadingOverlay.visibility = View.GONE

        val (title, message) = when (timepoint) {
            Timepoint.T0 -> Pair(
                getString(R.string.submission_success_baseline_title),
                getString(R.string.submission_success_baseline_message)
            )
            Timepoint.T4 -> Pair(
                getString(R.string.submission_success_final_title),
                getString(R.string.submission_success_final_message)
            )
            else -> Pair(
                getString(R.string.submission_success_monthly_title),
                getString(R.string.submission_success_monthly_message)
            )
        }

        val finalMessage = if (offlineNote) {
            "$message\n\n${getString(R.string.submission_offline_note)}"
        } else {
            message
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(finalMessage)
            .setPositiveButton(getString(R.string.ok_button)) { _, _ ->
                setResult(RESULT_OK)
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun buildPayload(scores: ScaleScores, usageData: List<com.aldogor.stilme_qe_app.DailyUsage>): String {
        val data = mutableMapOf<String, Any>()

        data["record_id"] = responses.studyId
        data["redcap_event_name"] = timepoint.redcapEventName

        // Add all responses
        responses.responses.forEach { (key, value) ->
            data[key] = value.toString()
        }

        // Add scores
        val scoreSuffix = if (timepoint == Timepoint.T0) "_t0" else "_m"
        data["bsmas_score$scoreSuffix"] = scores.bsmas
        data["phq9_score$scoreSuffix"] = scores.phq9
        data["gad7_score$scoreSuffix"] = scores.gad7
        data["fomo_score$scoreSuffix"] = scores.fomo
        data["pss10_score$scoreSuffix"] = scores.pss10

        // Add group assignment for baseline
        if (timepoint == Timepoint.T0) {
            data["group_assignment"] = studyManager.getGroup()

            // Add consent data for baseline submission
            val consentData = studyManager.getConsentData()
            if (consentData != null) {
                data["consent_informed"] = if (consentData.informedConsent) 1 else 0
                data["consent_privacy"] = if (consentData.privacyConsent) 1 else 0
                data["consent_timestamp"] = consentData.timestamp ?: ""
            }
        }

        // Add usage data with timepoint-specific field names
        // Baseline uses: usage_data_json, data_baseline, app_version
        // Follow-ups use: usage_data_json_m1, data_collection_m1, etc. (no app_version)
        val usageJson = buildUsageJson(usageData)

        if (timepoint == Timepoint.T0) {
            data["usage_data_json"] = usageJson
            data["data_baseline"] = LocalDate.now().toString()
            data["app_version"] = BuildConfig.VERSION_NAME
        } else {
            val usageSuffix = when (timepoint) {
                Timepoint.T1 -> "_m1"
                Timepoint.T2 -> "_m2"
                Timepoint.T3 -> "_m3"
                Timepoint.T4 -> "_m4"
                else -> ""
            }
            data["usage_data_json$usageSuffix"] = usageJson
            data["data_collection$usageSuffix"] = LocalDate.now().toString()
        }

        return Gson().toJson(listOf(data))
    }

    private fun buildUsageJson(usageData: List<com.aldogor.stilme_qe_app.DailyUsage>): String {
        val usageMap = mapOf(
            "collection_date" to LocalDate.now().toString(),
            "days_collected" to usageData.size,
            "data" to usageData.map { day ->
                val dayMap = mutableMapOf<String, Any>("date" to day.dateString)
                day.appUsage.forEach { (packageName, usage) ->
                    val appName = com.aldogor.stilme_qe_app.AppConfig.MONITORED_APPS[packageName]
                        ?.lowercase()?.replace(" ", "_")?.replace("(", "")?.replace(")", "") ?: packageName
                    dayMap["${appName}_min"] = usage.timeMinutes
                    dayMap["${appName}_opens"] = usage.opens
                }
                dayMap
            }
        )
        return Gson().toJson(usageMap)
    }

    companion object {
        const val EXTRA_TIMEPOINT = "extra_timepoint"
        const val EXTRA_INELIGIBLE_REASON = "extra_ineligible_reason"

        // Ineligibility reason codes
        const val REASON_NOT_UNITO = "not_unito"
        const val REASON_AGE = "age"
        const val REASON_USING_STL = "using_stl"
    }
}
