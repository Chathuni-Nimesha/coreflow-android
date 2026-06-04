package com.example.coreflow.model.cases

import com.example.coreflow.view.adapters.ItemList


data class Case (
    val id: Long,
    var comment: String,
    var date: Long,
    val habitId: Long
) : ItemList