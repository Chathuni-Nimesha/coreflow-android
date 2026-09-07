package com.chathuninimesha.coreflow.ui.logs

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.chathuninimesha.coreflow.R
import com.chathuninimesha.coreflow.databinding.AlertdialogRedactCommentCaseBinding
import com.chathuninimesha.coreflow.databinding.FragmentCasesBinding
import com.chathuninimesha.coreflow.databinding.PartLoadingBinding
import com.chathuninimesha.coreflow.model.Repositories
import com.chathuninimesha.coreflow.model.graph.Graph
import com.chathuninimesha.coreflow.model.logs.HabitLog
import com.chathuninimesha.coreflow.model.timer.TimerCase
import com.chathuninimesha.coreflow.reminder.CancelResult
import com.chathuninimesha.coreflow.reminder.ReminderModule
import com.chathuninimesha.coreflow.ui.common.ListItem
import com.chathuninimesha.coreflow.ui.common.MainViewModelFactory
import com.chathuninimesha.coreflow.ui.main.MainActivity
import com.chathuninimesha.coreflow.util.Consts.DAY_UNIX_MILLIS
import com.chathuninimesha.coreflow.util.Consts.KEY_HABIT_ID
import kotlinx.coroutines.launch
import java.util.Date
import java.util.LinkedHashMap

class HabitLogsFragment : Fragment() {

    private var _binding: FragmentCasesBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HabitLogsViewModel
    private lateinit var adapter: HabitLogsAdapter
    private lateinit var loadingBinding: PartLoadingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = HabitLogsAdapter(
            listOf(GraphDelegate(requireContext()), TimerDelegate(), HabitLogDelegate()),
            object : HabitLogActionListener {
                override fun deleteLog(log: HabitLog) = showDeleteDialog(log)
                override fun editNote(log: HabitLog) = showEditNoteDialog(log)
            }
        )
        viewModel = ViewModelProvider(
            this,
            MainViewModelFactory(Repositories, this)
        )[HabitLogsViewModel::class.java]
        viewModel.init(requireArguments().getLong(KEY_HABIT_ID))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCasesBinding.inflate(inflater, container, false)
        loadingBinding = PartLoadingBinding.bind(binding.root)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.createCaseButton.setOnClickListener {
            (activity as? MainActivity)?.openDetail(
                AddHabitLogFragment.newInstance(requireArguments().getLong(KEY_HABIT_ID)),
                getString(R.string.add_log)
            )
        }
        loadingBinding.againButton.setOnClickListener { viewModel.reload() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiLogsState.collect { state ->
                    loadingBinding.progressBar.isVisible = false
                    loadingBinding.errorContainer.isVisible = false
                    loadingBinding.emptyContainer.isVisible = false
                    loadingBinding.emptyTitle.setText(R.string.empty_activity_title)
                    loadingBinding.emptyBody.setText(R.string.empty_activity_body)
                    when {
                        state.isError -> {
                            binding.recyclerView.isVisible = false
                            loadingBinding.errorContainer.isVisible = true
                            binding.createCaseButton.isVisible = true
                        }
                        state.isLoading && state.logs.isEmpty() -> {
                            binding.recyclerView.isVisible = false
                            loadingBinding.progressBar.isVisible = true
                        }
                        state.logs.isEmpty() -> {
                            binding.recyclerView.isVisible = false
                            loadingBinding.emptyContainer.isVisible = true
                            binding.createCaseButton.isVisible = true
                            adapter.data = emptyList()
                        }
                        else -> {
                            binding.recyclerView.isVisible = true
                            binding.createCaseButton.isVisible = true
                            val map: MutableMap<Date, Int> = LinkedHashMap()
                            for (log in state.logs.reversed()) {
                                val day = Date(log.date - log.date % DAY_UNIX_MILLIS)
                                map[day] = (map[day] ?: 0) + 1
                            }
                            adapter.data = listOf<ListItem>(
                                Graph(map),
                                TimerCase(state.logs.first().date)
                            ) + state.logs
                        }
                    }
                }
            }
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        if (_binding != null) adapter.resumeTimers(binding.recyclerView)
    }

    override fun onStop() {
        if (_binding != null) adapter.pauseTimers(binding.recyclerView)
        super.onStop()
    }

    private fun showEditNoteDialog(log: HabitLog) {
        val dialogBinding = AlertdialogRedactCommentCaseBinding.inflate(layoutInflater)
        if (log.comment.isNotBlank()) dialogBinding.commentCaseEditText.setText(log.comment)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.edit_note))
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.save), null)
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val note = dialogBinding.commentCaseEditText.text.toString()
                viewModel.updateNote(log, note)
                ReminderModule.scheduler(requireContext())
                    .schedule(ReminderModule.toRequest(log.copy(comment = note)))
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showDeleteDialog(log: HabitLog) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.sure_delete_log))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteLog(log)
                val cancel = ReminderModule.scheduler(requireContext()).cancel(log.habitId, log.id)
                if (cancel is CancelResult.Failed) {
                    Toast.makeText(requireContext(), R.string.reminder_cancel_failed, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        fun newInstance(habitId: Long): HabitLogsFragment {
            return HabitLogsFragment().apply {
                arguments = Bundle().apply { putLong(KEY_HABIT_ID, habitId) }
            }
        }
    }
}
