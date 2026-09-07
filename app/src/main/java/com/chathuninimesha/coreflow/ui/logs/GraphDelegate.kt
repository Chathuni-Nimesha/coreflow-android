package com.chathuninimesha.coreflow.ui.logs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.chathuninimesha.coreflow.R
import com.chathuninimesha.coreflow.databinding.ItemGraphBinding
import com.chathuninimesha.coreflow.model.graph.Graph
import com.chathuninimesha.coreflow.ui.common.ListItem
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.Locale

class GraphDelegate(private val context: Context) : RowDelegate {

    override fun forItem(item: ListItem) = item is Graph

    override fun getViewHolder(parent: ViewGroup, clickListener: View.OnClickListener): RecyclerView.ViewHolder {
        val binding = ItemGraphBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.graph.contentDescription = context.getString(R.string.cd_activity_chart)
        return HabitLogsAdapter.GraphViewHolder(binding)
    }

    override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder, item: ListItem) {
        val graphVH = viewHolder as HabitLogsAdapter.GraphViewHolder
        val graph = item as Graph
        val monthsMap: MutableMap<String, Int> = LinkedHashMap()
        val labelsMonth = mutableListOf<String>()
        val dataBarMonths = mutableListOf<BarEntry>()
        val dataBarDays = mutableListOf<BarEntry>()
        val labelsDays = mutableListOf<String>()
        var counter = 0f
        val sdfDays = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
        val sdfMonths = SimpleDateFormat("MMM yy", Locale.getDefault())

        graph.data.forEach { (date, y) ->
            dataBarDays.add(BarEntry(counter, y.toFloat()))
            labelsDays.add(sdfDays.format(date))
            val month = sdfMonths.format(date)
            monthsMap[month] = (monthsMap[month] ?: 0) + y
            counter++
        }
        counter = 0f
        monthsMap.forEach { (date, y) ->
            dataBarMonths.add(BarEntry(counter, y.toFloat()))
            labelsMonth.add(date)
            counter++
        }

        val barColor = context.getColor(R.color.coreflow_primary)
        val textColor = context.getColor(R.color.coreflow_on_surface)
        val barDaysDataSet = BarDataSet(dataBarDays, context.getString(R.string.activity)).apply {
            color = barColor
        }
        val barMonthsDataSet = BarDataSet(dataBarMonths, context.getString(R.string.activity)).apply {
            color = barColor
        }

        fun applyChart(dataSet: BarDataSet, labels: List<String>, size: Int) {
            with(graphVH.binding.graph) {
                data = BarData(dataSet)
                data.isHighlightEnabled = false
                setVisibleXRangeMaximum(7f)
                setVisibleXRangeMinimum(5f)
                description.isEnabled = false
                legend.isEnabled = false
                setMaxVisibleValueCount(0)
                setPinchZoom(false)
                isDoubleTapToZoomEnabled = false
                setScaleEnabled(false)
                moveViewToX(size - 7f)
                xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                xAxis.labelCount = size
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.granularity = 1f
                xAxis.setDrawGridLines(false)
                xAxis.textColor = textColor
                axisLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
                axisLeft.spaceTop = 0f
                axisLeft.axisMinimum = 0f
                axisLeft.textColor = textColor
                axisLeft.granularity = 1f
                axisRight.isEnabled = false
                invalidate()
            }
        }

        applyChart(barDaysDataSet, labelsDays, dataBarDays.size)

        graphVH.binding.rangeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            if (checkedId == R.id.toggleMonths) {
                applyChart(barMonthsDataSet, labelsMonth, dataBarMonths.size)
            } else {
                applyChart(barDaysDataSet, labelsDays, dataBarDays.size)
            }
        }
    }
}
