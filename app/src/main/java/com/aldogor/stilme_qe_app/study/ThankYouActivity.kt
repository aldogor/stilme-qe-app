package com.aldogor.stilme_qe_app.study

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.aldogor.stilme_qe_app.R
import com.aldogor.stilme_qe_app.databinding.ActivityThankYouBinding

/**
 * Thank you screen shown after completing the study (T4).
 */
class ThankYouActivity : AppCompatActivity() {

    private lateinit var binding: ActivityThankYouBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display (required for Android 15+)
        enableEdgeToEdge()

        binding = ActivityThankYouBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle window insets for edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        binding.buttonClose.setOnClickListener {
            finishAffinity() // Close all activities
        }

        binding.buttonCounseling.setOnClickListener {
            openCounselingUrl()
        }

        // Handle back press - close all activities
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAffinity()
            }
        })
    }

    private fun openCounselingUrl() {
        val url = getString(R.string.thank_you_counseling_url)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}
