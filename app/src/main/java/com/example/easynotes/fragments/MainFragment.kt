package com.example.easynotes.fragments

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.easynotes.R
import com.example.easynotes.adapters.TabsPagerAdapter
import com.example.easynotes.databinding.FragmentMainBinding
import com.example.easynotes.models.TabInfo
import com.example.easynotes.models.TabType
import com.example.easynotes.viewmodels.TabsViewModel
import com.google.android.material.tabs.TabLayout
import android.text.SpannableString
import kotlin.math.min
import android.text.style.ForegroundColorSpan
import android.widget.LinearLayout
import androidx.fragment.app.DialogFragment


class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TabsViewModel by viewModels()
    private var isAddTabDialogShowing = false
    private lateinit var tabSelectionListener: TabLayout.OnTabSelectedListener


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val drawerLayout = binding.drawerlayout
        val toolbar = binding.materialToolbar

        // Load saved tabs when the view is created
        viewModel.loadTabs(requireContext())

        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)//to left side
        }

        viewModel.tabs.observe(viewLifecycleOwner) { tabs ->
            setupTabsWithViewPager(tabs)
            attachLongPressListenersToTabs(tabs)
        }

        val navigationView = binding.navView

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_all_notes -> {
                    binding.viewPager.currentItem = 0
                    binding.tabLayout.getTabAt(0)?.select()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }

                R.id.nav_calendar -> {
                    // Navigate to the Calendar Fragment using NavController
                    findNavController().navigate(R.id.action_mainFragment_to_calenderFragment)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }

                else -> false
            }
        }

        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_writeFragment)
        }

        // Optional: Add long press menu to tabs for management
        setupTabLongPressMenu()
    }

    // Add this method to save tabs when the fragment is paused
    override fun onPause() {
        super.onPause()
        // Save tabs when the fragment is paused (app going to background)
        viewModel.saveTabs(requireContext())
    }

    override fun onStop() {
        super.onStop()
        viewModel.saveTabs(requireContext())
    }

    private fun saveTabsState() {
        viewModel.saveTabs(requireContext())
    }




    private fun setupTabsWithViewPager(tabs: List<TabInfo>) {
        // Create the adapter with tabs from ViewModel
        val tabsPagerAdapter = TabsPagerAdapter(childFragmentManager, lifecycle, tabs)
        binding.viewPager.adapter = tabsPagerAdapter

        // Disable ViewPager2 swiping to prevent accidental switching
        binding.viewPager.isUserInputEnabled = false

        // Set tab icon tint
        val tabIconTint = ContextCompat.getColorStateList(requireContext(), R.color.black)
        binding.tabLayout.tabIconTint = tabIconTint

        // Configure TabLayout properties
        binding.tabLayout.apply {
            tabMode = TabLayout.MODE_SCROLLABLE
            tabGravity = TabLayout.GRAVITY_START
        }

        // This gives us full control over tab behavior
        binding.tabLayout.removeAllTabs() // Clear any existing tabs

        // Add regular tabs
        tabs.forEachIndexed { index, tabInfo ->
            val newTab = binding.tabLayout.newTab()

            when (tabInfo.type) {
                TabType.TEXT -> newTab.text = tabInfo.title
                TabType.ICON -> newTab.icon = tabInfo.iconResId?.let {
                    ContextCompat.getDrawable(requireContext(), it)
                }

                TabType.MIXED -> {
                    newTab.text = tabInfo.title
                    newTab.icon = tabInfo.iconResId?.let {
                        ContextCompat.getDrawable(requireContext(), it)
                    }
                }
            }

            binding.tabLayout.addTab(newTab)
        }

        // Add the "+" tab
        val plusTab = binding.tabLayout.newTab().setText("+")
        binding.tabLayout.addTab(plusTab)

        // Style tabs
        styleTabs(tabs)

        // Select the first tab initially
        if (tabs.isNotEmpty()) {
            binding.viewPager.currentItem = 0
            binding.tabLayout.selectTab(binding.tabLayout.getTabAt(0))
        }

        // IMPORTANT: Use a variable in the fragment class to track dialog state
        // Add this as a class member:
        // private var isAddTabDialogShowing = false

        // Remove any existing listeners to avoid duplicates
        clearTabSelectionListeners()

        // Add our tab selection listener
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val position = tab.position

                // If the + tab is selected and dialog is not showing
                if (position == binding.tabLayout.tabCount - 1) {
                    if (!isAddTabDialogShowing) {
                        // Set flag to prevent multiple dialogs
                        isAddTabDialogShowing = true

                        // Show the dialog
                        showAddTabDialog { wasTabAdded ->
                            // Reset flag when dialog is dismissed
                            isAddTabDialogShowing = false

                            if (wasTabAdded) {
                                // The dialog will handle selecting the new tab
                            } else {
                                // If no tab was added, go back to previous tab
                                val currentTabPosition = binding.viewPager.currentItem
                                binding.tabLayout.getTabAt(currentTabPosition)?.select()
                            }
                        }
                    } else {
                        // If dialog is already showing, don't do anything with the + tab
                        // This prevents the dialog from appearing again
                    }
                } else {
                    // For regular tabs, update ViewPager
                    binding.viewPager.currentItem = position

                    // Apply your existing selection styling
                    if (position < tabs.size && tabs[position].type == TabType.ICON) {
                        tab.icon?.setTint(Color.WHITE)
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                val position = tab.position

                // Apply your existing unselection styling
                if (position < tabs.size && tabs[position].type == TabType.ICON) {
                    tab.icon?.setTint(ContextCompat.getColor(requireContext(), R.color.gray))
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                val position = tab.position

                // If + tab is reselected and dialog is not showing
                if (position == binding.tabLayout.tabCount - 1 && !isAddTabDialogShowing) {
                    // Set flag to prevent multiple dialogs
                    isAddTabDialogShowing = true

                    // Show the dialog
                    showAddTabDialog { wasTabAdded ->
                        // Reset flag when dialog is dismissed
                        isAddTabDialogShowing = false

                        if (wasTabAdded) {
                            // The dialog will handle selecting the new tab
                        } else {
                            // If no tab was added, go back to previous tab
                            val currentTabPosition = binding.viewPager.currentItem
                            binding.tabLayout.getTabAt(currentTabPosition)?.select()
                        }
                    }
                }
            }
        })
    }


    private fun clearTabSelectionListeners() {
        // Get the field containing listeners
        try {
            val field = TabLayout::class.java.getDeclaredField("selectedListeners")
            field.isAccessible = true
            val listeners = field.get(binding.tabLayout) as? ArrayList<*>
            listeners?.clear()
        } catch (e: Exception) {
            Log.e("TabsFragment", "Error clearing tab listeners: ${e.message}")

            // Fallback: try to remove our known listener
            try {
                if (::tabSelectionListener.isInitialized) {
                    binding.tabLayout.removeOnTabSelectedListener(tabSelectionListener)
                }
            } catch (e: Exception) {
                Log.e("TabsFragment", "Error removing known listener: ${e.message}")
            }
        }
    }


    private fun styleTabs(tabs: List<TabInfo>) {
        binding.tabLayout.post {
            val tabLayout = binding.tabLayout
            val tabStrip = tabLayout.getChildAt(0) as ViewGroup

            // Calculate a smaller fixed width for each tab
            val tabWidth = resources.getDimensionPixelSize(R.dimen.smaller_tab_width)

            for (i in 0 until tabLayout.tabCount) {
                val tabView = tabStrip.getChildAt(i)

                // Apply fixed width to all tabs
                val layoutParams = tabView.layoutParams
                layoutParams.width = tabWidth
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                tabView.layoutParams = layoutParams

                // Add small margin between tabs
                val marginParams = tabView.layoutParams as ViewGroup.MarginLayoutParams
                marginParams.setMargins(2, 0, 2, 0) // 4dp margin on left and right
                tabView.layoutParams = marginParams

                // For regular tabs (not the + tab)
                if (i < tabs.size) {
                    val tabInfo = tabs[i]

                    // Style based on tab type with smaller text
                    when (tabInfo.type) {
                        TabType.TEXT, TabType.MIXED -> {
                            val tabTextView =
                                tabView.findViewById<TextView>(com.google.android.material.R.id.text)
                            tabTextView?.apply {
                                setTextSize(
                                    TypedValue.COMPLEX_UNIT_SP,
                                    12f
                                ) // Reduce from 14sp to 12sp
                                isAllCaps = false
                                gravity = Gravity.CENTER
                                // Reduce padding inside text view
                                setPadding(4, 0, 4, 0) // Minimal horizontal padding
                            }
                        }

                        else -> { /* No text styling needed */
                        }
                    }

                    // Style icon if present with smaller size
                    if (tabInfo.type == TabType.ICON || tabInfo.type == TabType.MIXED) {
                        val tabIconView =
                            tabView.findViewById<ImageView>(com.google.android.material.R.id.icon)
                        tabIconView?.apply {
                            val iconSize =
                                resources.getDimensionPixelSize(R.dimen.smaller_tab_icon_size) // Change to ~18dp
                            layoutParams.width = iconSize
                            layoutParams.height = iconSize
                            scaleType = ImageView.ScaleType.FIT_CENTER
                        }
                    }
                } else {
                    // Style the + tab
                    val tabTextView =
                        tabView.findViewById<TextView>(com.google.android.material.R.id.text)
                    tabTextView?.apply {
                        setTextSize(
                            TypedValue.COMPLEX_UNIT_SP,
                            14f
                        ) // Slightly larger than regular tabs
                        isAllCaps = false
                        gravity = Gravity.CENTER
                        // Use minimal padding
                        setPadding(4, 0, 4, 0)
                    }
                }
            }
        }
    }


    private fun showAddTabDialog(onComplete: (Boolean) -> Unit) {
        // Flag to track if a tab was added
        var tabWasAdded = false

        val dialogView = layoutInflater.inflate(R.layout.dialog_new_tab, null)
        val editText = dialogView.findViewById<EditText>(R.id.tabNameEditText)
        editText.setTextColor(Color.BLACK)

        val titleView = TextView(requireContext()).apply {
            text = "Add New Tab"
            setTextColor(Color.BLACK)
            textSize = 20f
            setPadding(24, 24, 24, 16)
            gravity = Gravity.START
            typeface = Typeface.DEFAULT_BOLD
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setCustomTitle(titleView)
            .setView(dialogView)
            .setPositiveButton("Add", null) // We'll set this later
            .setNegativeButton("Cancel", null)
            .create()

        // Prevent canceling by touching outside
        dialog.setCanceledOnTouchOutside(false)

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(android.R.color.white)

            // Override positive button to validate
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setTextColor(Color.BLACK)
            positiveButton.setOnClickListener {
                val tabName = editText.text.toString()
                if (tabName.isNotEmpty()) {
                    // Add the new tab
                    val newTabId = "custom_${System.currentTimeMillis()}"
                    viewModel.addTab(TabInfo(newTabId, tabName, type = TabType.TEXT))
                    saveTabsState()

                    // Mark that a tab was added
                    tabWasAdded = true

                    // Dismiss the dialog
                    dialog.dismiss()

                    // Select the newly added tab (it will be the second-to-last tab, before the + tab)
                    binding.tabLayout.post {
                        val newTabPosition = binding.tabLayout.tabCount - 2
                        binding.viewPager.currentItem = newTabPosition
                        binding.tabLayout.getTabAt(newTabPosition)?.select()
                    }
                } else {
                    // Show error for empty name
                    editText.error = "Tab name cannot be empty"
                }
            }

            // Style negative button
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            negativeButton.setTextColor(Color.BLACK)
            negativeButton.setOnClickListener {
                dialog.dismiss()
            }
        }

        // Notify when dialog is dismissed
        dialog.setOnDismissListener {
            onComplete(tabWasAdded)
        }

        dialog.show()
    }


    private fun setupTabLongPressMenu() {
        // We'll attach long press listeners whenever tabs change
        viewModel.tabs.observe(viewLifecycleOwner) { tabs ->
            // Use post to ensure TabLayout is ready
            binding.tabLayout.post {
                // Make sure TabLayout is properly initialized
                if (binding.tabLayout.tabCount > 0) {
                    attachLongPressListenersToTabs(tabs)
                } else {
                    // If tabs aren't ready yet, try again after a delay
                    Handler(Looper.getMainLooper()).postDelayed({
                        attachLongPressListenersToTabs(tabs)
                    }, 100)
                }
            }
        }
    }

    private fun attachLongPressListenersToTabs(tabs: List<TabInfo>) {
        try {
            val tabLayout = binding.tabLayout
            val tabStrip = tabLayout.getChildAt(0) as ViewGroup

            // Loop through each tab
            for (i in 0 until tabLayout.tabCount - 1) { // Exclude the + tab
                // Skip default tabs (assuming first 4 are default)
                if (i > 3 && i < tabs.size) { // Make sure we don't go out of bounds
                    val tabView = tabStrip.getChildAt(i)

                    // Make sure we have a valid tab info
                    val tabInfo = if (i < tabs.size) tabs[i] else continue

                    // Remove any existing listeners to avoid duplicates
                    tabView.setOnLongClickListener(null)

                    // Set new long press listener
                    tabView.setOnLongClickListener { view ->
                        // Remember which tab had the long press
                        val longPressedTabPosition = i

                        // Show popup menu with custom styling
                        val popup = PopupMenu(requireContext(), view)
                        popup.menuInflater.inflate(R.menu.menu_tab_options, popup.menu)

                        // Apply custom styling to popup menu
                        try {
                            val menuHelper = PopupMenu::class.java.getDeclaredField("mPopup")
                            menuHelper.isAccessible = true
                            val menuPopupHelper = menuHelper.get(popup)

                            // Force icons to show
                            val method = menuPopupHelper.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                            method.isAccessible = true
                            method.invoke(menuPopupHelper, true)
                        } catch (e: Exception) {
                            Log.e("TabsDebug", "Error styling popup: ${e.message}")
                        }

                        // Style each menu item to ensure text is black
                        for (j in 0 until popup.menu.size()) {
                            val item = popup.menu.getItem(j)
                            val spanString = SpannableString(item.title.toString())
                            spanString.setSpan(ForegroundColorSpan(Color.BLACK), 0, spanString.length, 0)
                            item.title = spanString
                        }

                        // Set a flag to prevent dialog from showing after menu actions
                        isAddTabDialogShowing = true

                        popup.setOnMenuItemClickListener { item ->
                            when (item.itemId) {
                                R.id.action_rename_tab -> {
                                    // Dismiss popup first
                                    popup.dismiss()

                                    // Then show rename dialog after a short delay
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        showRenameTabDialog(tabInfo, longPressedTabPosition)
                                    }, 100)
                                    true
                                }
                                R.id.action_delete_tab -> {
                                    // Dismiss popup first
                                    popup.dismiss()

                                    // Then show delete dialog after a short delay
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        showDeleteTabDialog(tabInfo, longPressedTabPosition)
                                    }, 100)
                                    true
                                }
                                else -> false
                            }
                        }

                        popup.show()
                        true
                    }

                    // Add a visual indication that this tab has a long press menu
                    tabView.isHapticFeedbackEnabled = true
                }
            }
        } catch (e: Exception) {
            Log.e("TabsDebug", "Error attaching long press listeners: ${e.message}", e)
        }
    }


    private fun isDialogVisible(): Boolean {
        // Check for any dialog fragments
        val fragmentManager = childFragmentManager
        for (fragment in fragmentManager.fragments) {
            if (fragment is DialogFragment && fragment.dialog?.isShowing == true) {
                return true
            }
        }

        // No dialogs visible
        return false
    }


    private fun showRenameTabDialog(tabInfo: TabInfo, tabPosition: Int) {
        // Set a class-level flag to prevent the add dialog from showing
        isAddTabDialogShowing = true

        val dialogView = layoutInflater.inflate(R.layout.dialog_new_tab, null)
        val editText = dialogView.findViewById<EditText>(R.id.tabNameEditText)
        editText.setText(tabInfo.title)
        editText.setTextColor(Color.BLACK)
        editText.setHintTextColor(Color.GRAY)

        val titleView = TextView(requireContext()).apply {
            text = "Rename Tab"
            setTextColor(Color.BLACK)
            textSize = 20f
            setPadding(24, 24, 24, 16)
            gravity = Gravity.START
            typeface = Typeface.DEFAULT_BOLD
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setCustomTitle(titleView)
            .setView(dialogView)
            .setPositiveButton("Save", null) // We'll set this later
            .setNegativeButton("Cancel", null) // We'll set this later
            .create()

        // Prevent canceling by touching outside
        dialog.setCanceledOnTouchOutside(false)

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(android.R.color.white)

            // Get and customize positive button
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setTextColor(Color.BLACK)
            positiveButton.setOnClickListener {
                val newName = editText.text.toString()
                if (newName.isNotEmpty()) {
                    // Update the tab
                    viewModel.updateTabTitle(tabInfo.id, newName)
                    saveTabsState()

                    // Dismiss the dialog
                    dialog.dismiss()

                    // Make sure we stay on the same tab
                    binding.tabLayout.post {
                        binding.tabLayout.getTabAt(tabPosition)?.select()
                    }
                } else {
                    // Show error for empty name
                    editText.error = "Tab name cannot be empty"
                }
            }

            // Get and customize negative button
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            negativeButton.setTextColor(Color.BLACK)
            negativeButton.setOnClickListener {
                dialog.dismiss()

                // Make sure we stay on the same tab
                binding.tabLayout.post {
                    binding.tabLayout.getTabAt(tabPosition)?.select()
                }
            }
        }

        // Reset flag when dialog is dismissed
        dialog.setOnDismissListener {
            // Reset flag with a delay to make sure selection is handled first
            Handler(Looper.getMainLooper()).postDelayed({
                isAddTabDialogShowing = false
            }, 500)
        }

        dialog.show()
    }


    private fun showDeleteTabDialog(tabInfo: TabInfo, tabPosition: Int) {
        // Set a class-level flag to prevent the add dialog from showing
        isAddTabDialogShowing = true

        val deleteLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 16)

            // Title TextView
            val titleView = TextView(requireContext()).apply {
                text = "Delete Tab"
                setTextColor(Color.BLACK)
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, 16)
            }

            // Message TextView
            val messageView = TextView(requireContext()).apply {
                text = "Are you sure you want to delete the '${tabInfo.title}' tab?"
                setTextColor(Color.BLACK)
                textSize = 16f
            }

            addView(titleView)
            addView(messageView)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(deleteLayout)
            .setPositiveButton("Delete", null) // We'll set this later
            .setNegativeButton("Cancel", null) // We'll set this later
            .create()

        // Prevent canceling by touching outside
        dialog.setCanceledOnTouchOutside(false)

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(android.R.color.white)

            // Get and customize positive button
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setTextColor(Color.RED) // Red for delete
            positiveButton.setOnClickListener {
                // Log that delete was clicked
                Log.d("TabsDebug", "Delete button clicked for tab: ${tabInfo.id}")

                // Remove the tab
                viewModel.removeTab(tabInfo.id)
                saveTabsState()

                // Dismiss dialog
                dialog.dismiss()

                // Select an appropriate tab after deletion
                binding.tabLayout.post {
                    Log.d("TabsDebug", "After deletion, tab count: ${binding.tabLayout.tabCount}")

                    // Select a nearby tab after deletion
                    val newPosition = min(tabPosition, binding.tabLayout.tabCount - 2)
                    if (newPosition >= 0) {
                        binding.viewPager.currentItem = newPosition
                        binding.tabLayout.getTabAt(newPosition)?.select()
                    }
                }
            }

            // Get and customize negative button
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            negativeButton.setTextColor(Color.BLACK)
            negativeButton.setOnClickListener {
                dialog.dismiss()

                // Make sure we stay on the same tab
                binding.tabLayout.post {
                    binding.tabLayout.getTabAt(tabPosition)?.select()
                }
            }
        }

        // Reset flag when dialog is dismissed
        dialog.setOnDismissListener {
            // Reset flag with a delay to make sure selection is handled first
            Handler(Looper.getMainLooper()).postDelayed({
                isAddTabDialogShowing = false
            }, 500)
        }

        dialog.show()
    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Avoid memory leaks
    }


}

