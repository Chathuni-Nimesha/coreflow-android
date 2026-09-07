package com.chathuninimesha.coreflow.reminder

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

object NotificationAccess {
    private const val PREFS = "coreflow_reminder_prefs"
    private const val KEY_ASKED = "notification_permission_asked"

    fun needsRuntimePermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun hasPermission(context: Context): Boolean {
        if (!needsRuntimePermission()) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun state(fragment: Fragment): NotificationAccessState {
        val context = fragment.requireContext()
        val granted = hasPermission(context)
        val shouldExplain = needsRuntimePermission() &&
            fragment.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        return NotificationAccessState(
            granted = granted,
            shouldExplain = shouldExplain,
            askedBefore = prefs(context).getBoolean(KEY_ASKED, false)
        )
    }

    fun markAsked(context: Context) {
        prefs(context).edit().putBoolean(KEY_ASKED, true).apply()
    }

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
