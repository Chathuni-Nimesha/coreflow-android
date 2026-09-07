package com.chathuninimesha.coreflow.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.chathuninimesha.coreflow.R
import com.chathuninimesha.coreflow.databinding.FragmentHomeBinding
import com.chathuninimesha.coreflow.model.Repositories
import com.chathuninimesha.coreflow.ui.common.MainViewModelFactory
import com.chathuninimesha.coreflow.ui.main.MainActivity
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(
            this,
            MainViewModelFactory(Repositories, this)
        )[HomeViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.goToHabitsButton.setOnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.nav_habits)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: HomeUiState) {
        val name = state.greetingName
        binding.greetingText.text = if (name.isNullOrBlank()) {
            getString(R.string.greeting_fallback)
        } else {
            getString(R.string.greeting_named, name)
        }
        binding.habitsCountText.text = getString(R.string.home_habits_count, state.habitCount)
        binding.timeSinceText.text = state.timeSinceLastLog?.let {
            "${getString(R.string.home_time_since)}: $it"
        } ?: getString(R.string.home_no_recent)

        binding.recentContainer.removeAllViews()
        if (state.isEmpty) {
            binding.emptyHome.visibility = View.VISIBLE
        } else {
            binding.emptyHome.visibility = View.GONE
            val formatter = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
            state.recentLogs.forEach { item ->
                val card = MaterialCardView(requireContext()).apply {
                    layoutParams = ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).also { it.bottomMargin = (8 * resources.displayMetrics.density).toInt() }
                    radius = 16f
                    cardElevation = 2f
                    setContentPadding(32, 32, 32, 32)
                }
                val text = TextView(requireContext()).apply {
                    text = "${item.habitName}\n${item.note}\n${formatter.format(Date(item.date))}"
                    setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                }
                card.addView(text)
                binding.recentContainer.addView(card)
            }
            if (state.recentLogs.isEmpty()) {
                val empty = TextView(requireContext()).apply {
                    text = getString(R.string.home_no_recent)
                    setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                }
                binding.recentContainer.addView(empty)
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
