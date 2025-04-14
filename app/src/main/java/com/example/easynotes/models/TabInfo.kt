package com.example.easynotes.models

data class TabInfo(
    val id: String,
    val title: String? = null,
    val iconResId: Int? = null,
    val type: TabType = TabType.TEXT
)

enum class TabType {
    TEXT, ICON, MIXED
}
