package com.chathuninimesha.coreflow.ui.logs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.chathuninimesha.coreflow.databinding.ItemCaseBinding
import com.chathuninimesha.coreflow.model.logs.HabitLog
import com.chathuninimesha.coreflow.ui.common.ListItem
import java.text.SimpleDateFormat
import java.util.Locale

class HabitLogDelegate : RowDelegate {
    override fun forItem(item: ListItem) = item is HabitLog

    override fun getViewHolder(parent: ViewGroup, clickListener: View.OnClickListener): RecyclerView.ViewHolder {
        val binding = ItemCaseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.moreCaseButton.setOnClickListener(clickListener)
        binding.moreCaseButton.contentDescription =
            parent.context.getString(com.chathuninimesha.coreflow.R.string.cd_log_options)
        return HabitLogsAdapter.LogViewHolder(binding)
    }

    override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder, item: ListItem) {
        val holder = viewHolder as HabitLogsAdapter.LogViewHolder
        val log = item as HabitLog
        holder.binding.moreCaseButton.tag = log
        val sdf = SimpleDateFormat("HH:mm dd MMM yyyy", Locale.getDefault())
        holder.binding.comment.text = log.comment
        holder.binding.dateTV.text = sdf.format(log.date)
    }
}
