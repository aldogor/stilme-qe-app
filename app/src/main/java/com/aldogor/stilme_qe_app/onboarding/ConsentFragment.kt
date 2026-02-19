package com.aldogor.stilme_qe_app.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.aldogor.stilme_qe_app.R
import com.aldogor.stilme_qe_app.databinding.FragmentConsentBinding
import com.aldogor.stilme_qe_app.study.StudyManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Consent form screen with study information.
 * Two Yes/No questions (as per DPO requirements):
 * 1. Informed consent (read + agree to participate)
 * 2. Privacy consent (data processing)
 *
 * Both must be answered "Sì" to proceed. If either is "No",
 * a warning dialog is shown explaining participation requires consent.
 */
class ConsentFragment : Fragment() {

    private var _binding: FragmentConsentBinding? = null
    private val binding get() = _binding!!

    private var navigator: OnboardingNavigator? = null
    private lateinit var studyManager: StudyManager

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
        _binding = FragmentConsentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonAccept.isEnabled = false

        setupConsentLinks()
        setupPrivacyQuestionLink()

        // Listen for changes on informed consent radio group
        binding.radioGroupInformed.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.radio_informed_no) {
                showConsentRequiredWarning()
            }
            updateAcceptButtonState()
        }

        // Listen for changes on privacy consent radio group
        binding.radioGroupPrivacy.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.radio_privacy_no) {
                showConsentRequiredWarning()
            }
            updateAcceptButtonState()
        }

        binding.buttonAccept.setOnClickListener {
            val informedConsent = binding.radioInformedYes.isChecked
            val privacyConsent = binding.radioPrivacyYes.isChecked

            studyManager.markConsentGiven(
                informedConsent = informedConsent,
                privacyConsent = privacyConsent
            )
            navigator?.navigateToPermission()
        }
    }

    /**
     * Shows a warning dialog when user selects "No" for any consent question.
     * Explains that participation requires consent but allows them to change their answer.
     * Uses Material3 dialog for modern appearance.
     */
    private fun showConsentRequiredWarning() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.consent_declined_title))
            .setMessage(getString(R.string.consent_declined_message))
            .setPositiveButton(getString(R.string.consent_declined_understood), null)
            .show()
    }

    private fun setupConsentLinks() {
        val fullText = getString(R.string.consent_full_text)
        val linkText = getString(R.string.consent_link_privacy_info) // "seguente link"

        // Find the two occurrences of "seguente link"
        val firstIndex = fullText.indexOf(linkText)
        val secondIndex = if (firstIndex != -1) {
            fullText.indexOf(linkText, firstIndex + linkText.length)
        } else -1

        if (firstIndex == -1) {
            // No links found, keep plain text
            return
        }

        val spannableString = SpannableString(fullText)

        // First link - privacy info URL
        val privacyClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val url = getString(R.string.consent_privacy_info_url)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = ContextCompat.getColor(requireContext(), R.color.primary)
                ds.isUnderlineText = true
            }
        }

        spannableString.setSpan(
            privacyClickableSpan,
            firstIndex,
            firstIndex + linkText.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Second link - download docs URL (if exists)
        if (secondIndex != -1) {
            val downloadClickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val url = getString(R.string.consent_download_docs_url)
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = ContextCompat.getColor(requireContext(), R.color.primary)
                    ds.isUnderlineText = true
                }
            }

            spannableString.setSpan(
                downloadClickableSpan,
                secondIndex,
                secondIndex + linkText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        binding.textConsentContent.text = spannableString
        binding.textConsentContent.movementMethod = LinkMovementMethod.getInstance()
    }

    /**
     * Sets up clickable link in the privacy consent question text.
     * The link "informativa sulla privacy" opens the privacy policy URL.
     */
    private fun setupPrivacyQuestionLink() {
        val fullText = getString(R.string.consent_question_privacy)
        val linkText = "informativa sulla privacy"

        val startIndex = fullText.indexOf(linkText)
        if (startIndex == -1) {
            // Link text not found, use plain text
            return
        }

        val endIndex = startIndex + linkText.length
        val spannableString = SpannableString(fullText)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val privacyUrl = getString(R.string.privacy_url)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl))
                startActivity(intent)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = ContextCompat.getColor(requireContext(), R.color.primary)
                ds.isUnderlineText = true
            }
        }

        spannableString.setSpan(
            clickableSpan,
            startIndex,
            endIndex,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.textQuestionPrivacy.text = spannableString
        binding.textQuestionPrivacy.movementMethod = LinkMovementMethod.getInstance()
    }

    /**
     * Updates the accept button state based on consent responses.
     * Button is only enabled when both questions are answered "Sì".
     */
    private fun updateAcceptButtonState() {
        binding.buttonAccept.isEnabled =
            binding.radioInformedYes.isChecked &&
            binding.radioPrivacyYes.isChecked
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
