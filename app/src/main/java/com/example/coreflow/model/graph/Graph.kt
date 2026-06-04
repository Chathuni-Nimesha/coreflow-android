package com.example.coreflow.model.graph

import com.example.coreflow.view.adapters.ItemList
import java.util.Date

data class Graph(
    val data: MutableMap<Date, Int>
) : ItemList