package com.example.easynotes.adapters

import android.os.Bundle
import android.provider.Settings.Global.putString
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.easynotes.fragments.AllTabFragment
import com.example.easynotes.fragments.BookmarkTabFragment
import com.example.easynotes.fragments.GenericNotesFragment
import com.example.easynotes.fragments.HomeTabFragment
import com.example.easynotes.fragments.WorkTabFragment
import com.example.easynotes.models.TabInfo

class TabsPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle,
       private val tabs: List<TabInfo>) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int = tabs.size

    override fun createFragment(position: Int): Fragment {
        // Create appropriate fragment based on tab type
        return when (tabs[position].id) {
            "all" -> AllTabFragment()
            "home" -> HomeTabFragment()
            "work" -> WorkTabFragment()
            "bookmark" -> BookmarkTabFragment()
            else -> {
                // For dynamically added tabs, create a generic fragment
                val fragment = GenericNotesFragment()
                fragment.arguments = Bundle().apply {
                    putString("TAB_ID", tabs[position].id)
                    putString("TAB_TITLE", tabs[position].title)
                }
                fragment
            }
        }
    }
}