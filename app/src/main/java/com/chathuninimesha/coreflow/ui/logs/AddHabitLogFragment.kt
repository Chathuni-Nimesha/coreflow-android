package com.chathuninimesha.coreflow.ui.logs

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.chathuninimesha.coreflow.R
import com.chathuninimesha.coreflow.databinding.FragmentAddCaseBinding
import com.chathuninimesha.coreflow.model.Repositories
import com.chathuninimesha.coreflow.model.logs.HabitLog
import com.chathuninimesha.coreflow.reminder.NotificationAccess
import com.chathuninimesha.coreflow.reminder.ReminderModule
import com.chathuninimesha.coreflow.reminder.ScheduleResult
import com.chathuninimesha.coreflow.ui.common.MainViewModelFactory
import com.chathuninimesha.coreflow.util.Consts.KEY_HABIT_ID
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.GregorianCalendar

class AddHabitLogFragment : Fragment() {

    private lateinit var binding: FragmentAddCaseBinding
    private lateinit var viewModel: AddHabitLogViewModel
    private var pickedDate: Long = 0
    private var pendingSave: PendingLog? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val pending = pendingSave
        pendingSave = null
        if (!granted) {
            Toast.makeText(
                requireContext(),
                R.string.reminder_permission_denied,
                Toast.LENGTH_LONG
            ).show()
        }
        if (pending != null) {
            saveLog(pending)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(
            this,
            MainViewModelFactory(Repositories, this)
        )[AddHabitLogViewModel::class.java]
        viewModel.init(requireArguments().getLong(KEY_HABIT_ID))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddCaseBinding.inflate(inflater, container, false)

        val calendar = Calendar.getInstance()
        val currentHours = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMin = calendar.get(Calendar.MINUTE)

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        pickedDate = calendar.timeInMillis

        binding.currentTime.text = formatTime(currentHours, currentMin)

        with(binding) {
            cancelButton.setOnClickListener {
                activity?.supportFragmentManager?.popBackStack()
            }
            calendarViem.setOnDateChangeListener { _, year, month, dayOfMonth ->
                pickedDate = GregorianCalendar(year, month, dayOfMonth).timeInMillis
            }
            changeTimeButton.setOnClickListener {
                openDialog(currentMin, currentHours)
            }
            saveButton.setOnClickListener { onSaveClicked() }
        }

        return binding.root
    }

    private fun onSaveClicked() {
        if (!binding.saveButton.isEnabled) return
        val pending = parseInput() ?: return
        val access = NotificationAccess.state(this)
        val needsReminder = pending.date > System.currentTimeMillis()
        if (needsReminder && !access.granted && NotificationAccess.needsRuntimePermission()) {
            if (access.permanentlyDenied) {
                showSettingsRequiredDialog(pending)
                return
            }
            if (access.shouldExplain) {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.reminder_permission_title)
                    .setMessage(R.string.reminder_permission_rationale)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.set) { _, _ ->
                        requestNotificationPermission(pending)
                    }
                    .show()
                return
            }
            requestNotificationPermission(pending)
            return
        }
        saveLog(pending)
    }

    private fun requestNotificationPermission(pending: PendingLog) {
        pendingSave = pending
        NotificationAccess.markAsked(requireContext())
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun showSettingsRequiredDialog(pending: PendingLog) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.reminder_permission_title)
            .setMessage(R.string.reminder_permission_settings)
            .setNegativeButton(R.string.cancel) { _, _ -> saveLog(pending) }
            .setPositiveButton(R.string.open_settings) { _, _ ->
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", requireContext().packageName, null)
                    }
                )
                saveLog(pending)
            }
            .show()
    }

    private fun parseInput(): PendingLog? {
        val comment = binding.commentET.text.toString()
        if (comment.isBlank()) {
            Toast.makeText(context, getString(R.string.enter_note), Toast.LENGTH_SHORT).show()
            return null
        }
        val parts = binding.currentTime.text.toString().split(":")
        if (parts.size != 2) {
            Toast.makeText(context, R.string.reminder_invalid_time, Toast.LENGTH_SHORT).show()
            return null
        }
        val hours = parts[0].toIntOrNull()
        val minutes = parts[1].toIntOrNull()
        if (hours == null || minutes == null || hours !in 0..23 || minutes !in 0..59) {
            Toast.makeText(context, R.string.reminder_invalid_time, Toast.LENGTH_SHORT).show()
            return null
        }
        if (pickedDate <= 0L) {
            Toast.makeText(context, R.string.reminder_invalid_time, Toast.LENGTH_SHORT).show()
            return null
        }
        val date = pickedDate + hours * 60L * 60L * 1000L + minutes * 60L * 1000L
        return PendingLog(comment, date)
    }

    private fun saveLog(pending: PendingLog) {
        binding.saveButton.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = viewModel.createLog(
                    HabitLog(
                        0,
                        pending.comment,
                        pending.date,
                        requireArguments().getLong(KEY_HABIT_ID)
                    )
                )
                val created = result.getOrNull()
                if (created == null) {
                    Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show()
                    return@launch
                }
                when (ReminderModule.scheduler(requireContext()).schedule(
                    ReminderModule.toRequest(created)
                )) {
                    ScheduleResult.ScheduledExact -> Unit
                    ScheduleResult.ScheduledInexact -> showInexactAlarmMessage()
                    ScheduleResult.SkippedExpired -> Toast.makeText(
                        context,
                        R.string.reminder_skipped_past,
                        Toast.LENGTH_SHORT
                    ).show()
                    ScheduleResult.InvalidTime -> Toast.makeText(
                        context,
                        R.string.reminder_invalid_time,
                        Toast.LENGTH_SHORT
                    ).show()
                    ScheduleResult.MissingId -> Toast.makeText(
                        context,
                        R.string.reminder_missing_id,
                        Toast.LENGTH_SHORT
                    ).show()
                    ScheduleResult.Failed, ScheduleResult.ExactAlarmUnavailable -> Toast.makeText(
                        context,
                        R.string.reminder_schedule_failed,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                activity?.supportFragmentManager?.popBackStack()
            } finally {
                if (isAdded) binding.saveButton.isEnabled = true
            }
        }
    }

    private fun showInexactAlarmMessage() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.exact_alarm_title)
            .setMessage(R.string.exact_alarm_unavailable)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                }
            }
            .show()
    }

    private fun openDialog(min: Int, hours: Int) {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                binding.currentTime.text = formatTime(hourOfDay, minute)
            },
            hours,
            min,
            true
        ).show()
    }

    private fun formatTime(hours: Int, minutes: Int): String =
        "$hours:${minutes.toString().padStart(2, '0')}"

    private data class PendingLog(val comment: String, val date: Long)

    companion object {
        fun newInstance(habitId: Long): AddHabitLogFragment {
            val args = Bundle().apply {
                putLong(KEY_HABIT_ID, habitId)
            }
            val fragment = AddHabitLogFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
