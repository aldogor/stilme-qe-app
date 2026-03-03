package com.aldogor.stilme_qe_app.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aldogor.stilme_qe_app.R
import com.aldogor.stilme_qe_app.databinding.FragmentIneligibleBinding

/**
 * Screen shown when participant is not eligible for the study.
 */
class IneligibleFragment : Fragment() {

    private var _binding: FragmentIneligibleBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIneligibleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val reason = arguments?.getString(ARG_REASON) ?: getString(R.string.ineligible_default_reason)
        binding.textReason.text = reason

        binding.buttonClose.setOnClickListener {
            (activity as? OnboardingNavigator)?.finishOnboarding()
                ?: activity?.finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_REASON = "reason"

        fun newInstance(reason: String): IneligibleFragment {
            return IneligibleFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_REASON, reason)
                }
            }
        }
    }
}
