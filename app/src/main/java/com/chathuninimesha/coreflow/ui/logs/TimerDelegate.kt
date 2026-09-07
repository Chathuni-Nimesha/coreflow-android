package com.chathuninimesha.coreflow.ui.logs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.chathuninimesha.coreflow.databinding.ItemTimerBinding
import com.chathuninimesha.coreflow.model.timer.TimerCase
import com.chathuninimesha.coreflow.timer.SystemClock
import com.chathuninimesha.coreflow.ui.common.ListItem

class TimerDelegate : RowDelegate {
    override fun forItem(item: ListItem) = item is TimerCase

    override fun getViewHolder(parent: ViewGroup, clickListener: View.OnClickListener) =
        HabitLogsAdapter.TimerViewHolder(
            ItemTimerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder, item: ListItem) {
        val holder = viewHolder as HabitLogsAdapter.TimerViewHolder
        val timerCase = item as TimerCase
        holder.dateStart = timerCase.dateStart
        holder.binding.timer.onChronometerTickListener = null
        holder.render(SystemClock.now())
        holder.binding.timer.setOnChronometerTickListener {
            holder.render(SystemClock.now())
        }
    }
}
