package com.chathuninimesha.coreflow.model.logs

import com.chathuninimesha.coreflow.ui.common.ListItem

/**
 * A single habit log entry.
 * Gson field names stay id/comment/date/habitId for existing cases_* JSON compatibility.
 */
data class HabitLog(
    val id: Long,
    val comment: String,
    val date: Long,
    val habitId: Long
) : ListItem
