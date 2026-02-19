package com.aldogor.stilme_qe_app.stl_guide

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.aldogor.stilme_qe_app.R
import com.aldogor.stilme_qe_app.databinding.FragmentGroupInstructionsBinding
import com.aldogor.stilme_qe_app.onboarding.OnboardingNavigator
import com.aldogor.stilme_qe_app.study.StudyGroup
import com.aldogor.stilme_qe_app.study.StudyManager

/**
 * Group-specific instructions screen shown after baseline questionnaire.
 * Adapts content based on assigned study group.
 */
class GroupInstructionsFragment : Fragment() {

    private var _binding: FragmentGroupInstructionsBinding? = null
    private val binding get() = _binding!!

    private var navigator: OnboardingNavigator? = null
    private lateinit var studyManager: StudyManager

    companion object {
        private const val ARG_GROUP_CODE = "group_code"
        private const val ARG_STANDALONE = "standalone"

        fun newInstance(groupCode: Int, standalone: Boolean = false): GroupInstructionsFragment {
            return GroupInstructionsFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_GROUP_CODE, groupCode)
                    putBoolean(ARG_STANDALONE, standalone)
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        navigator = context as? OnboardingNavigator
        studyManager = StudyManager(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupInstructionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val groupCode = arguments?.getInt(ARG_GROUP_CODE) ?: studyManager.getGroup()
        val isStandalone = arguments?.getBoolean(ARG_STANDALONE) ?: false

        setupContentForGroup(groupCode)

        binding.buttonMoreInfo.setOnClickListener {
            val url = getString(R.string.welcome_info_url)
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        binding.buttonContinue.setOnClickListener {
            if (isStandalone) {
                // Close the activity when viewing from main screen
                activity?.finish()
            } else {
                // Continue onboarding flow
                navigator?.finishOnboarding()
            }
        }
    }

    private fun setupContentForGroup(groupCode: Int) {
        when (groupCode) {
            StudyGroup.CONTROL.code -> setupControlGroup()
            StudyGroup.STL_INTERVENTION.code -> setupSTLGroup()
            StudyGroup.PERSONAL_COMMITMENT.code -> setupCommitmentGroup()
        }
    }

    private fun setupControlGroup() {
        binding.apply {
            textTitle.text = getString(R.string.group_control_title)
            textSubtitle.text = getString(R.string.group_control_subtitle)
            textInstructionsTitle.text = getString(R.string.group_control_instructions_title)
            textInstructions.text = getString(R.string.group_control_instructions)

            // Hide STL-specific cards
            cardStlSetup.isVisible = false
            cardAppsList.isVisible = false
            cardImportant.isVisible = false
        }
    }

    private fun setupSTLGroup() {
        binding.apply {
            textTitle.text = getString(R.string.group_stl_title)
            textSubtitle.text = getString(R.string.group_stl_subtitle)
            textInstructionsTitle.text = getString(R.string.group_stl_instructions_title)
            textInstructions.text = getString(R.string.group_stl_instructions)

            // Show STL-specific cards
            cardStlSetup.isVisible = true
            cardAppsList.isVisible = true
            cardImportant.isVisible = true

            // Show PDF download button
            buttonDownloadPdf.isVisible = true
            buttonDownloadPdf.setOnClickListener {
                val pdfUrl = getString(R.string.group_stl_pdf_url)
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl)))
            }

            textSetupTitle.text = getString(R.string.group_stl_setup_title)
            textSetupInstructions.text = getString(R.string.group_stl_setup_instructions)
            textAppsListTitle.text = getString(R.string.group_stl_apps_list_title)
            textAppsList.text = getString(R.string.group_stl_apps_list)
            textImportantTitle.text = getString(R.string.group_stl_important)
            textImportantContent.text = getString(R.string.group_stl_important_content)
        }
    }

    private fun setupCommitmentGroup() {
        binding.apply {
            textTitle.text = getString(R.string.group_commitment_title)
            textSubtitle.text = getString(R.string.group_commitment_subtitle)
            textInstructionsTitle.text = getString(R.string.group_commitment_instructions_title)
            textInstructions.text = getString(R.string.group_commitment_instructions)

            // Hide STL-specific cards
            cardStlSetup.isVisible = false
            cardAppsList.isVisible = false
            cardImportant.isVisible = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
