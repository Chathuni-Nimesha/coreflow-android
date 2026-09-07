package com.chathuninimesha.coreflow.ui.habits

import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.chathuninimesha.coreflow.R
import com.chathuninimesha.coreflow.databinding.ItemHabitBinding
import com.chathuninimesha.coreflow.model.habits.Habit

interface HabitActionListener {
    fun deleteHabit(habit: Habit)
    fun renameHabit(habit: Habit)
    fun pickHabit(habit: Habit)
}

class HabitsAdapter(
    private val actionListener: HabitActionListener
) : RecyclerView.Adapter<HabitsAdapter.HabitViewHolder>(), View.OnClickListener {

    var data: List<Habit> = emptyList()
        set(newValue) {
            val diffCallback = object : DiffUtil.Callback() {
                override fun getOldListSize() = field.size
                override fun getNewListSize() = newValue.size
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                    field[oldItemPosition].id == newValue[newItemPosition].id
                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                    field[oldItemPosition] == newValue[newItemPosition]
            }
            val result = DiffUtil.calculateDiff(diffCallback)
            field = newValue
            result.dispatchUpdatesTo(this)
        }

    class HabitViewHolder(val binding: ItemHabitBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val binding = ItemHabitBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.root.setOnClickListener(this)
        binding.moreButton.setOnClickListener(this)
        return HabitViewHolder(binding)
    }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = data[position]
        holder.itemView.tag = habit
        holder.binding.moreButton.tag = habit
        holder.binding.nameTextView.text = habit.name
    }

    override fun onClick(v: View) {
        val habit = v.tag as Habit
        if (v.id == R.id.moreButton) showPopupMenu(v) else actionListener.pickHabit(habit)
    }

    private fun showPopupMenu(v: View) {
        val popupMenu = PopupMenu(v.context, v)
        val habit = v.tag as Habit
        popupMenu.menu.add(0, 1, Menu.NONE, v.context.getString(R.string.rename))
        popupMenu.menu.add(0, 2, Menu.NONE, v.context.getString(R.string.delete))
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                1 -> actionListener.renameHabit(habit)
                2 -> actionListener.deleteHabit(habit)
            }
            true
        }
        popupMenu.show()
    }
}
