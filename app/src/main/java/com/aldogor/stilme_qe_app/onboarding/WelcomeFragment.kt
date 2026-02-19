package com.aldogor.stilme_qe_app.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aldogor.stilme_qe_app.R
import com.aldogor.stilme_qe_app.databinding.FragmentWelcomeBinding

/**
 * Welcome screen introducing the study.
 */
class WelcomeFragment : Fragment() {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

    private var navigator: OnboardingNavigator? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        navigator = context as? OnboardingNavigator
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonContinue.setOnClickListener {
            navigator?.navigateToConsent()
        }

        binding.buttonMoreInfo.setOnClickListener {
            val url = getString(R.string.welcome_info_url)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
