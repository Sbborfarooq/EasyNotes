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

class TabsPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    private val tabs: List<TabInfo>
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int = tabs.size

    override fun createFragment(position: Int): Fragment {
        val tab = tabs[position]

        return when (tab.id) {
            "all" -> AllTabFragment().apply {
                arguments = Bundle().apply {
                    putString("ARG_TAB_ID", "all")
                    putString("ARG_TAB_TITLE", tab.title)
                }
            }
            "home" -> HomeTabFragment()
            "work" -> WorkTabFragment()
            "bookmark" -> BookmarkTabFragment()
            else -> GenericNotesFragment().apply {
                arguments = Bundle().apply {
                    putString("ARG_TAB_ID", tab.category)
                    putString("ARG_TAB_TITLE", tab.title)
                }
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return tabs[position].id.hashCode().toLong()
    }

    override fun containsItem(itemId: Long): Boolean {
        return tabs.any { it.id.hashCode().toLong() == itemId }
    }
}

