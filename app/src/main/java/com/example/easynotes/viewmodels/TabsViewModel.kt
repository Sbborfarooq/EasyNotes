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
        tabsList.add(TabInfo("all", "All", type = TabType.TEXT))
        tabsList.add(TabInfo("home", "Home", type = TabType.TEXT))
        tabsList.add(TabInfo("work", "Work", type = TabType.TEXT))
        tabsList.add(TabInfo("bookmark", null, R.drawable.ic_bookmark, TabType.ICON))

        // Update LiveData with initial tabs
        _tabs.value = tabsList.toList()
    }

    // Method to add a new tab
    fun addTab(tabInfo: TabInfo) {
        tabsList.add(tabInfo)
        _tabs.value = tabsList.toList()
    }

    // Method to remove a tab
    fun removeTab(tabId: String) {
        tabsList.removeAll { it.id == tabId }
        _tabs.value = tabsList.toList()
    }

    // Method to update a tab's title
    fun updateTabTitle(tabId: String, newTitle: String) {
        // Find the tab index
        val tabIndex = tabsList.indexOfFirst { it.id == tabId }

        // If found, update it
        if (tabIndex != -1) {
            val oldTab = tabsList[tabIndex]
            // Create a new TabInfo with updated title but keeping other properties
            val updatedTab = TabInfo(
                id = oldTab.id,
                title = newTitle,
                iconResId = oldTab.iconResId,
                type = oldTab.type
            )


            tabsList[tabIndex] = updatedTab

            // Notify observers
            _tabs.value = tabsList.toList()
        }
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

        // Convert tabs to JSON
        val gson = Gson()
        val tabsJson = gson.toJson(tabsList)

        editor.putString("saved_tabs", tabsJson)
        editor.apply()
    }

    // Method to load tabs from SharedPreferences
    fun loadTabs(context: Context) {
        val prefs = context.getSharedPreferences("tabs_prefs", Context.MODE_PRIVATE)

        val tabsJson = prefs.getString("saved_tabs", null)
        if (tabsJson != null) {
            try {
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

                // Notify observers
                _tabs.value = tabsList.toList()
            } catch (e: Exception) {
                Log.e("TabsViewModel", "Error loading tabs: ${e.message}")
                // Add default tabs in case of error
                addDefaultTabs()
            }
        } else {
            // Add default tabs if no saved tabs found
            addDefaultTabs()
        }
    }

    // Helper method to add default tabs
    private fun addDefaultTabs() {
        tabsList.clear()
        tabsList.add(TabInfo("all", "All", type = TabType.TEXT))
        tabsList.add(TabInfo("home", "Home", type = TabType.TEXT))
        tabsList.add(TabInfo("work", "Work", type = TabType.TEXT))
        tabsList.add(TabInfo("bookmark", null, R.drawable.ic_bookmark, TabType.ICON))
        _tabs.value = tabsList.toList()
    }
}