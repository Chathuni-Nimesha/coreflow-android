package com.chathuninimesha.coreflow.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.chathuninimesha.coreflow.R
import com.chathuninimesha.coreflow.auth.AuthModule
import com.chathuninimesha.coreflow.auth.AuthNavigator
import com.chathuninimesha.coreflow.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private var page = 0

    private data class Page(
        val titleRes: Int,
        val bodyRes: Int,
        val illustrationRes: Int?
    )

    private val pages by lazy {
        listOf(
            Page(R.string.onboard_title_1, R.string.onboard_body_1, R.drawable.ic_onboard_habits),
            Page(R.string.onboard_title_2, R.string.onboard_body_2, R.drawable.ic_onboard_activity),
            Page(R.string.onboard_title_3, R.string.onboard_body_3, R.drawable.ic_onboard_reminder)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        AuthModule.init(this)
        page = savedInstanceState?.getInt(KEY_PAGE) ?: 0

        binding.skipOnboarding.setOnClickListener { finishOnboarding() }
        binding.backButton.setOnClickListener {
            if (page > 0) {
                page--
                render()
            }
        }
        binding.primaryButton.setOnClickListener {
            if (page < pages.lastIndex) {
                page++
                render()
            } else {
                finishOnboarding()
            }
        }
        render()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_PAGE, page)
    }

    private fun render() {
        val current = pages[page]
        binding.titleText.setText(current.titleRes)
        binding.bodyText.setText(current.bodyRes)
        binding.progressText.text = getString(R.string.progress_page, page + 1, pages.size)
        if (current.illustrationRes != null) {
            binding.illustration.setImageResource(current.illustrationRes)
            binding.illustration.visibility = View.VISIBLE
        } else {
            binding.illustration.visibility = View.GONE
        }
        binding.backButton.visibility = if (page == 0) View.GONE else View.VISIBLE
        binding.primaryButton.setText(
            if (page == pages.lastIndex) R.string.get_started else R.string.next
        )
    }

    private fun finishOnboarding() {
        AuthNavigator.openAuthAfterOnboarding(this, AuthModule.repository())
    }

    companion object {
        private const val KEY_PAGE = "onboarding_page"
    }
}
