package com.example.easynotes.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.easynotes.R
import com.example.easynotes.adapters.NoteAdapter
import com.example.easynotes.databinding.FragmentCalenderBinding
import com.example.easynotes.databinding.FragmentGenericNotesBinding
import com.example.easynotes.models.Note
import com.example.easynotes.viewmodels.NoteViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CalenderFragment : Fragment() {
    private var _binding: FragmentCalenderBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: NoteViewModel
    private lateinit var adapter: NoteAdapter

    private val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val selectedDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var selectedDate = Date() // Default to today

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalenderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel first
        setupViewModel()

        // Then setup UI components
        setupToolbar()
        setupRecyclerView()
        setupCalendar()

        // Set up the FAB to add a note for the selected date
        binding.addNoteButton.setOnClickListener {
            val bundle = Bundle().apply {
                putString("selectedDate", selectedDateFormat.format(selectedDate))
            }
            findNavController().navigate(R.id.action_calenderFragment_to_writeFragment, bundle)
        }
    }

    private fun setupToolbar() {
        binding.calendarToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupCalendar() {
        // Set initial month/year text
        binding.monthYearText.text = dateFormat.format(Date())

        // Set up previous/next month buttons
        binding.previousMonthButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.time = selectedDate
            calendar.add(Calendar.MONTH, -1)
            updateCalendarForMonth(calendar)
        }

        binding.nextMonthButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.time = selectedDate
            calendar.add(Calendar.MONTH, 1)
            updateCalendarForMonth(calendar)
        }

        // Initialize calendar grid
        val today = Calendar.getInstance()
        updateCalendarForMonth(today)
    }

    private fun updateCalendarForMonth(calendar: Calendar) {
        calendar.set(Calendar.DAY_OF_MONTH, 1) // Move to first day of month
        selectedDate = calendar.time

        // Update header
        binding.monthYearText.text = dateFormat.format(selectedDate)

        // Clear existing calendar cells
        binding.calendarGrid.removeAllViews()

        // Get first day of month and number of days
        val firstDayOfMonth = calendar.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Create calendar cells
        val today = Calendar.getInstance()

        // Add empty cells for days before the 1st
        for (i in 0 until firstDayOfMonth) {
            val emptyCell = layoutInflater.inflate(R.layout.calendar_day_empty, binding.calendarGrid, false)
            binding.calendarGrid.addView(emptyCell)
        }

        // Add cells for each day of the month
        for (dayOfMonth in 1..daysInMonth) {
            val dayCell = layoutInflater.inflate(R.layout.calendar_day, binding.calendarGrid, false)
            val dayText = dayCell.findViewById<android.widget.TextView>(R.id.dayText)
            dayText.text = dayOfMonth.toString()

            // Check if this is today
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                calendar.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)) {
                dayCell.setBackgroundResource(R.drawable.calender_today_background)
            }

            // Set click listener for the day
            dayCell.setOnClickListener {
                // Update selected date
                val newCal = Calendar.getInstance()
                newCal.time = selectedDate
                newCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                selectedDate = newCal.time

                // Update UI to show selected date
                updateSelectedDateUI()

                // Load notes for this date
                loadNotesForDate(selectedDate)
            }

            binding.calendarGrid.addView(dayCell)
        }

        // Load notes for the first day of the month initially
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        selectedDate = calendar.time
        updateSelectedDateUI()
        loadNotesForDate(selectedDate)
    }

    private fun updateSelectedDateUI() {
        // Update the selected date indicator
        binding.selectedDateText.text = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(selectedDate)
    }

    private fun setupRecyclerView() {
        adapter = NoteAdapter(
            onNoteClick = { note ->
                val bundle = Bundle().apply {
                    putLong("noteId", note.id)
                }
                findNavController().navigate(R.id.action_calenderFragment_to_writeFragment, bundle)
            },
            onNoteDelete = { note ->
                // Show delete confirmation dialog
                showDeleteConfirmationDialog(note)
            },
            onNoteDuplicate = { note ->
                try {
                    // Create a duplicate note with the current timestamp
                    val duplicateNote = note.copy(
                        id = 0, // Ensure ID is 0 for new note
                        title = note.title + " (Copy)",
                        date = System.currentTimeMillis()
                    )

                    // Insert the duplicate note
                    viewModel.insert(duplicateNote)

                    // Show success message
                    Toast.makeText(requireContext(), "Note duplicated", Toast.LENGTH_SHORT).show()

                    // Log for debugging
                    Log.d("CalendarFragment", "Note duplicated: ${duplicateNote.title}")
                } catch (e: Exception) {
                    // Log the error and show error message
                    Log.e("CalendarFragment", "Error duplicating note", e)
                    Toast.makeText(requireContext(), "Failed to duplicate note: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )

        binding.notesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@CalenderFragment.adapter
        }
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[NoteViewModel::class.java]
    }

    private fun loadNotesForDate(date: Date) {
        val dateString = selectedDateFormat.format(date)
        binding.notesForDateTitle.text = "Notes for ${SimpleDateFormat("MMMM d", Locale.getDefault()).format(date)}"

        // Get all notes and filter by date
        viewModel.allNotes.observe(viewLifecycleOwner) { allNotes ->
            // Filter notes that match the selected date
            // This is a simple approach - for a real app, you'd want to add a proper date query to your DAO
            val startOfDay = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val endOfDay = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            val notesForDate = allNotes.filter { note ->
                note.date in startOfDay..endOfDay
            }

            adapter.submitList(notesForDate)

            // Show/hide empty state
            if (notesForDate.isEmpty()) {
                binding.emptyNotesView.visibility = View.VISIBLE
                binding.notesRecyclerView.visibility = View.GONE
            } else {
                binding.emptyNotesView.visibility = View.GONE
                binding.notesRecyclerView.visibility = View.VISIBLE
            }
        }
    }

    private fun showDeleteConfirmationDialog(note: Note) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Note")
            .setMessage("Are you sure you want to delete this note?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.delete(note)
                Toast.makeText(context, "Note deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Avoid memory leaks
    }
}