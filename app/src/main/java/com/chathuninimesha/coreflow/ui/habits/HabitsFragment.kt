package com.chathuninimesha.coreflow.ui.habits

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.chathuninimesha.coreflow.R
import com.chathuninimesha.coreflow.databinding.AlertdialogAddRenameHabitBinding
import com.chathuninimesha.coreflow.databinding.FragmentHabitsBinding
import com.chathuninimesha.coreflow.databinding.PartLoadingBinding
import com.chathuninimesha.coreflow.model.Repositories
import com.chathuninimesha.coreflow.model.habits.Habit
import com.chathuninimesha.coreflow.reminder.ReminderModule
import com.chathuninimesha.coreflow.ui.common.MainViewModelFactory
import com.chathuninimesha.coreflow.ui.logs.HabitLogsFragment
import com.chathuninimesha.coreflow.ui.main.MainActivity
import kotlinx.coroutines.launch

class HabitsFragment : Fragment() {

    private var _binding: FragmentHabitsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: HabitsAdapter
    private lateinit var viewModel: HabitViewModel
    private lateinit var loadingBinding: PartLoadingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = HabitsAdapter(object : HabitActionListener {
            override fun deleteHabit(habit: Habit) = showDeleteDialog(habit)
            override fun renameHabit(habit: Habit) = showNameDialog(habit)
            override fun pickHabit(habit: Habit) {
                (activity as? MainActivity)?.openDetail(
                    HabitLogsFragment.newInstance(habit.id),
                    habit.name
                )
            }
        })
        viewModel = ViewModelProvider(
            this,
            MainViewModelFactory(Repositories, this)
        )[HabitViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHabitsBinding.inflate(inflater, container, false)
        loadingBinding = PartLoadingBinding.bind(binding.root)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.createHabitButton.setOnClickListener { showNameDialog(null) }
        loadingBinding.againButton.setOnClickListener { viewModel.updateHabits() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiHabitsState.collect { state ->
                    binding.mutationProgress.isVisible = state.isMutating
                    loadingBinding.progressBar.isVisible = false
                    loadingBinding.errorContainer.isVisible = false
                    loadingBinding.emptyContainer.isVisible = false
                    loadingBinding.emptyTitle.setText(R.string.empty_habits_title)
                    loadingBinding.emptyBody.setText(R.string.empty_habits_body)
                    when {
                        state.isError -> {
                            binding.recyclerView.isVisible = false
                            loadingBinding.errorContainer.isVisible = true
                            binding.createHabitButton.isVisible = true
                        }
                        state.isLoading && state.habits.isEmpty() -> {
                            binding.recyclerView.isVisible = false
                            loadingBinding.progressBar.isVisible = true
                        }
                        state.habits.isEmpty() -> {
                            binding.recyclerView.isVisible = false
                            loadingBinding.emptyContainer.isVisible = true
                            binding.createHabitButton.isVisible = true
                            adapter.data = emptyList()
                        }
                        else -> {
                            binding.recyclerView.isVisible = true
                            binding.createHabitButton.isVisible = true
                            adapter.data = state.habits
                        }
                    }
                }
            }
        }
        return binding.root
    }

    private fun showNameDialog(habit: Habit?) {
        val dialogBinding = AlertdialogAddRenameHabitBinding.inflate(layoutInflater)
        if (habit != null) dialogBinding.nameHabitEditText.setText(habit.name)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.enter_name))
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.save), null)
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val entered = dialogBinding.nameHabitEditText.text.toString()
                if (entered.isBlank()) {
                    dialogBinding.nameHabitEditText.error = getString(R.string.empty)
                    return@setOnClickListener
                }
                if (habit != null) viewModel.renameHabit(habit.id, entered)
                else viewModel.addHabit(entered)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showDeleteDialog(habit: Habit) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.sure_delete_habit))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                val scheduler = ReminderModule.scheduler(requireContext())
                viewModel.snapshotLogs(habit.id).forEach { log ->
                    scheduler.cancel(habit.id, log.id)
                }
                viewModel.deleteHabit(habit)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
