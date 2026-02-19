package com.aldogor.stilme_qe_app.stl_guide

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.commit
import com.aldogor.stilme_qe_app.R
import com.aldogor.stilme_qe_app.study.StudyManager

/**
 * Activity for showing group instructions from the main screen.
 * Uses GroupInstructionsFragment in standalone mode.
 */
class GroupInstructionsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_GROUP_CODE = "group_code"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_group_instructions)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fragment_container)) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        if (savedInstanceState == null) {
            val groupCode = intent.getIntExtra(EXTRA_GROUP_CODE, StudyManager(this).getGroup())

            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(
                    R.id.fragment_container,
                    GroupInstructionsFragment.newInstance(groupCode, standalone = true)
                )
            }
        }
    }
}
