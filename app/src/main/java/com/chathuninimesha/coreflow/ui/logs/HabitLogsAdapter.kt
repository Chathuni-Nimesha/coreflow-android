package com.chathuninimesha.coreflow.ui.logs

import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.chathuninimesha.coreflow.R
import com.chathuninimesha.coreflow.databinding.ItemCaseBinding
import com.chathuninimesha.coreflow.databinding.ItemGraphBinding
import com.chathuninimesha.coreflow.databinding.ItemTimerBinding
import com.chathuninimesha.coreflow.model.logs.HabitLog
import com.chathuninimesha.coreflow.timer.ElapsedTimeCalculator
import com.chathuninimesha.coreflow.timer.SystemClock
import com.chathuninimesha.coreflow.ui.common.ListItem

interface HabitLogActionListener {
    fun deleteLog(log: HabitLog)
    fun editNote(log: HabitLog)
}

interface RowDelegate {
    fun forItem(item: ListItem): Boolean
    fun getViewHolder(parent: ViewGroup, clickListener: View.OnClickListener): ViewHolder
    fun bindViewHolder(viewHolder: ViewHolder, item: ListItem)
}

class HabitLogsAdapter(
    private val delegates: List<RowDelegate>,
    private val actionListener: HabitLogActionListener
) : RecyclerView.Adapter<ViewHolder>(), View.OnClickListener {

    var data: List<ListItem> = emptyList()
        set(newValue) {
            val diffCallback = object : DiffUtil.Callback() {
                override fun getOldListSize() = field.size
                override fun getNewListSize() = newValue.size
                override fun areItemsTheSame(o: Int, n: Int): Boolean {
                    val oldItem = field[o]
                    val newItem = newValue[n]
                    return if (oldItem is HabitLog && newItem is HabitLog) oldItem.id == newItem.id else true
                }
                override fun areContentsTheSame(o: Int, n: Int) = field[o] == newValue[n]
            }
            val result = DiffUtil.calculateDiff(diffCallback)
            field = newValue
            result.dispatchUpdatesTo(this)
        }

    class LogViewHolder(val binding: ItemCaseBinding) : ViewHolder(binding.root)
    class TimerViewHolder(val binding: ItemTimerBinding) : ViewHolder(binding.root) {
        var dateStart: Long = 0L
        fun render(nowMillis: Long) {
            val duration = ElapsedTimeCalculator.between(dateStart, nowMillis)
            binding.days.text = duration.daysLabel()
            binding.timer.text = duration.clockLabel()
        }
        fun startTicking() = binding.timer.start()
        fun stopTicking() = binding.timer.stop()
    }
    class GraphViewHolder(val binding: ItemGraphBinding) : ViewHolder(binding.root)

    var timersActive: Boolean = true

    fun pauseTimers(recyclerView: RecyclerView) {
        timersActive = false
        for (i in 0 until recyclerView.childCount) {
            val holder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i))
            if (holder is TimerViewHolder) holder.stopTicking()
        }
    }

    fun resumeTimers(recyclerView: RecyclerView) {
        timersActive = true
        for (i in 0 until recyclerView.childCount) {
            val holder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i))
            if (holder is TimerViewHolder) {
                holder.render(SystemClock.now())
                holder.startTicking()
            }
        }
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (holder is TimerViewHolder && timersActive) {
            holder.render(SystemClock.now())
            holder.startTicking()
        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        if (holder is TimerViewHolder) holder.stopTicking()
        super.onViewDetachedFromWindow(holder)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        if (holder is TimerViewHolder) {
            holder.stopTicking()
            holder.binding.timer.onChronometerTickListener = null
        }
        super.onViewRecycled(holder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        delegates[viewType].getViewHolder(parent, this)

    override fun getItemViewType(position: Int) =
        delegates.indexOfFirst { it.forItem(data[position]) }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        delegates[getItemViewType(position)].bindViewHolder(holder, data[position])
    }

    override fun getItemCount() = data.size

    override fun onClick(v: View) {
        if (v.id == R.id.moreCaseButton) showPopupMenu(v)
    }

    private fun showPopupMenu(v: View) {
        val popupMenu = PopupMenu(v.context, v)
        val log = v.tag as HabitLog
        popupMenu.menu.add(0, 0, Menu.NONE, v.context.getString(R.string.edit_note))
        popupMenu.menu.add(0, 1, Menu.NONE, v.context.getString(R.string.delete))
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                0 -> actionListener.editNote(log)
                1 -> actionListener.deleteLog(log)
            }
            true
        }
        popupMenu.show()
    }
}
