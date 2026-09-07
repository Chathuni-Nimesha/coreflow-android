package com.chathuninimesha.coreflow.model.graph

import com.chathuninimesha.coreflow.ui.common.ListItem
import java.util.Date

data class Graph(
    val data: Map<Date, Int>
) : ListItem
