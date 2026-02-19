package com.aldogor.stilme_qe_app.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.aldogor.stilme_qe_app.MainActivity
import com.aldogor.stilme_qe_app.R
import com.aldogor.stilme_qe_app.databinding.ActivityOnboardingBinding
import com.aldogor.stilme_qe_app.questionnaire.QuestionnaireActivity
import com.aldogor.stilme_qe_app.stl_guide.GroupInstructionsFragment
import com.aldogor.stilme_qe_app.study.StudyGroup
import com.aldogor.stilme_qe_app.study.StudyManager
import com.aldogor.stilme_qe_app.study.Timepoint

/**
 * Container activity for onboarding flow.
 * Manages navigation between Welcome -> Consent -> Permission -> Baseline Questionnaire
 */
class OnboardingActivity : AppCompatActivity(), OnboardingNavigator {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var studyManager: StudyManager

    // Activity Result launcher for questionnaire
    private val questionnaireLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Check if there's an ineligibility reason from early screening
            val ineligibleReason = result.data?.getStringExtra(QuestionnaireActivity.EXTRA_INELIGIBLE_REASON)

            if (ineligibleReason != null) {
                // Early screening failure - show personalized message
                val message = when (ineligibleReason) {
                    QuestionnaireActivity.REASON_NOT_UNITO -> getString(R.string.ineligible_reason_not_unito)
                    QuestionnaireActivity.REASON_AGE -> getString(R.string.ineligible_reason_age)
                    QuestionnaireActivity.REASON_USING_STL -> getString(R.string.ineligible_reason_using_stl)
                    else -> getString(R.string.ineligible_default_reason)
                }
                navigateToIneligible(message)
                return@registerForActivityResult
            }

            // Questionnaire completed, check group assignment
            val group = studyManager.getGroup()

            when (group) {
                StudyGroup.INELIGIBLE_USING_STL.code -> {
                    navigateToIneligible(getString(R.string.ineligible_reason_using_stl))
                }
                StudyGroup.INELIGIBLE_SCREENING.code -> {
                    navigateToIneligible(getString(R.string.ineligible_default_reason))
                }
                StudyGroup.CONTROL.code,
                StudyGroup.STL_INTERVENTION.code,
                StudyGroup.PERSONAL_COMMITMENT.code -> {
                    // Show group-specific instructions for all eligible groups
                    navigateToGroupInstructions(group)
                }
                else -> {
                    // Unknown group - finish onboarding
                    finishOnboarding()
                }
            }
        }
        // If cancelled, stay in onboarding (no action needed)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display (required for Android 15+)
        enableEdgeToEdge()

        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle window insets for edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        studyManager = StudyManager(this)

        // Initialize study ID if not exists
        studyManager.getOrCreateStudyId()

        if (savedInstanceState == null) {
            navigateToWelcome()
        }

        setupBackPressHandler()
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 1) {
                    supportFragmentManager.popBackStack()
                } else {
                    // At first screen, just finish
                    finish()
                }
            }
        })
    }

    override fun navigateToWelcome() {
        replaceFragment(WelcomeFragment())
    }

    override fun navigateToConsent() {
        replaceFragment(ConsentFragment())
    }

    override fun navigateToPermission() {
        replaceFragment(PermissionFragment())
    }

    override fun navigateToQuestionnaire() {
        // Launch questionnaire activity for baseline
        val intent = Intent(this, QuestionnaireActivity::class.java).apply {
            putExtra(QuestionnaireActivity.EXTRA_TIMEPOINT, Timepoint.T0.index)
        }
        questionnaireLauncher.launch(intent)
    }

    override fun navigateToGroupInstructions(groupCode: Int) {
        replaceFragment(GroupInstructionsFragment.newInstance(groupCode, standalone = false))
    }

    @Deprecated("Use navigateToGroupInstructions instead")
    override fun navigateToSTLGuide() {
        navigateToGroupInstructions(StudyGroup.STL_INTERVENTION.code)
    }

    override fun navigateToIneligible(reason: String) {
        replaceFragment(IneligibleFragment.newInstance(reason))
    }

    override fun finishOnboarding() {
        studyManager.markOnboardingComplete()

        // Navigate to MainActivity
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragment_container, fragment)
            addToBackStack(null)
        }
    }

}

/**
 * Interface for fragment navigation.
 */
interface OnboardingNavigator {
    fun navigateToWelcome()
    fun navigateToConsent()
    fun navigateToPermission()
    fun navigateToQuestionnaire()
    fun navigateToGroupInstructions(groupCode: Int)
    @Deprecated("Use navigateToGroupInstructions instead")
    fun navigateToSTLGuide()
    fun navigateToIneligible(reason: String)
    fun finishOnboarding()
}
