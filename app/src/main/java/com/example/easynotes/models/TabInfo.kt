package com.example.easynotes.models

data class TabInfo(
    val id: String,
    val title: String? = null,
    val iconResId: Int? = null,
    val type: TabType = TabType.TEXT,
    val category: String = "Uncategorized"
)

enum class TabType {
    TEXT, ICON, MIXED
}
