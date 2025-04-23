package com.example.easynotes.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.easynotes.R
import com.example.easynotes.models.TabInfo
import com.example.easynotes.models.TabType
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson

class TabsViewModel : ViewModel() {

    private val _tabs = MutableLiveData<List<TabInfo>>()
    val tabs: LiveData<List<TabInfo>> = _tabs

    private val tabsList = mutableListOf<TabInfo>()

    init {
        // Initialize with default tabs
        addDefaultTabs()
    }

    // Add this function
    fun initializeTabs(initialTabs: List<TabInfo>) {
        tabsList.clear()
        tabsList.addAll(initialTabs)
        _tabs.value = tabsList.toList()
    }

    // Method to add a new tab
    fun addTab(tabInfo: TabInfo) {
        // Ensure the category is set if not provided
        val tabWithCategory = if (tabInfo.category == "Uncategorized") {
            tabInfo.copy(category = tabInfo.title ?: "Uncategorized")
        } else tabInfo

        tabsList.add(tabWithCategory)
        _tabs.value = tabsList.toList()
    }

    // Method to remove a tab
    fun removeTab(tabId: String) {
        // Don't allow removal of default tabs
        if (!isDefaultTab(tabId)) {
            tabsList.removeAll { it.id == tabId }
            _tabs.value = tabsList.toList()
        }
    }

    // Method to update a tab's title
    fun updateTabTitle(tabId: String, newTitle: String) {
        // Don't allow updating default tabs
        if (!isDefaultTab(tabId)) {
            val tabIndex = tabsList.indexOfFirst { it.id == tabId }
            if (tabIndex != -1) {
                val oldTab = tabsList[tabIndex]
                val updatedTab = TabInfo(
                    id = oldTab.id,
                    title = newTitle,
                    iconResId = oldTab.iconResId,
                    type = oldTab.type,
                    category = newTitle // Update category to match new title
                )
                tabsList[tabIndex] = updatedTab
                _tabs.value = tabsList.toList()
            }
        }
    }

    private fun isDefaultTab(tabId: String): Boolean {
        return tabId in listOf("all", "home", "work", "bookmark")
    }

    fun getTabPosition(tabId: String): Int {
        return tabsList.indexOfFirst { it.id == tabId }
    }

    fun getTabById(tabId: String): TabInfo? {
        return tabsList.find { it.id == tabId }
    }

    // Method to save tabs to SharedPreferences
    fun saveTabs(context: Context) {
        val prefs = context.getSharedPreferences("tabs_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        try {
            val gson = Gson()
            val tabsJson = gson.toJson(tabsList)
            editor.putString("saved_tabs", tabsJson)
            editor.apply()
        } catch (e: Exception) {
            Log.e("TabsViewModel", "Error saving tabs: ${e.message}")
        }
    }

    // Method to load tabs from SharedPreferences
    fun loadTabs(context: Context) {
        val prefs = context.getSharedPreferences("tabs_prefs", Context.MODE_PRIVATE)

        try {
            val tabsJson = prefs.getString("saved_tabs", null)
            if (tabsJson != null) {
                val gson = Gson()
                val type = object : TypeToken<List<TabInfo>>() {}.type
                val loadedTabs = gson.fromJson<List<TabInfo>>(tabsJson, type)

                // Clear current tabs and add loaded ones
                tabsList.clear()
                tabsList.addAll(loadedTabs)

                // If no tabs were loaded, add default tabs
                if (tabsList.isEmpty()) {
                    addDefaultTabs()
                }
            } else {
                // Add default tabs if no saved tabs found
                addDefaultTabs()
            }
        } catch (e: Exception) {
            Log.e("TabsViewModel", "Error loading tabs: ${e.message}")
            addDefaultTabs()
        }

        // Notify observers
        _tabs.value = tabsList.toList()
    }

    // Helper method to add default tabs
    private fun addDefaultTabs() {
        tabsList.clear()
        tabsList.add(TabInfo("all", "All", type = TabType.TEXT, category = "all"))
        tabsList.add(TabInfo("home", "Home", type = TabType.TEXT, category = "Home"))
        tabsList.add(TabInfo("work", "Work", type = TabType.TEXT, category = "Work"))
        tabsList.add(TabInfo(
            id = "bookmark",
            title = null,
            iconResId = R.drawable.ic_bookmark,
            type = TabType.ICON,
            category = "Bookmarked"
        ))
        _tabs.value = tabsList.toList()
    }
}