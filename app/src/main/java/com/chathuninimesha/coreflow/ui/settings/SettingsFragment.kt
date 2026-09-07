package com.chathuninimesha.coreflow.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.chathuninimesha.coreflow.R
import com.chathuninimesha.coreflow.auth.AuthModule
import com.chathuninimesha.coreflow.auth.AuthNavigator
import com.chathuninimesha.coreflow.databinding.FragmentSettingsBinding
import com.chathuninimesha.coreflow.reminder.NotificationAccess

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val profile = AuthModule.repository().currentProfile()
        binding.profileName.text = profile?.name ?: getString(R.string.greeting_fallback)
        binding.profileEmail.text = profile?.email.orEmpty()
        binding.notificationStatus.text = if (NotificationAccess.hasPermission(requireContext())) {
            getString(R.string.notifications_enabled)
        } else {
            getString(R.string.notifications_disabled)
        }
        binding.notificationSettingsButton.setOnClickListener {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", requireContext().packageName, null)
                }
            )
        }
        binding.logoutButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.logout_confirm_title)
                .setMessage(R.string.logout_confirm_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.log_out) { _, _ ->
                    AuthModule.repository().signOut()
                    AuthNavigator.openSignIn(requireActivity())
                }
                .show()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
