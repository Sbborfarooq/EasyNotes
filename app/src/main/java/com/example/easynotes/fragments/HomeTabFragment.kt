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
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.easynotes.R
import com.example.easynotes.adapters.NoteAdapter
import com.example.easynotes.databinding.FragmentHomeTabBinding
import com.example.easynotes.models.Note
import com.example.easynotes.viewmodels.NoteViewModel

class HomeTabFragment : Fragment() {
    private var _binding: FragmentHomeTabBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: NoteViewModel
    private lateinit var adapter: NoteAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupViewModel("Home")
    }

    private fun setupRecyclerView() {
        adapter = NoteAdapter(
            onNoteClick = { note ->
                // Navigate to edit the note
                val bundle = Bundle().apply {
                    putLong("noteId", note.id)
                }
                findNavController().navigate(R.id.action_mainFragment_to_writeFragment, bundle)
            },
            onNoteDelete = { note ->
                // Show delete confirmation dialog instead of deleting directly
                showDeleteConfirmationDialog(note)
            },
            onNoteDuplicate = { note ->
                try {
                    // Create a duplicate note with the current timestamp
                    val duplicateNote = note.copy(
                        id = 0, // Ensure ID is 0 for new note
                        title = note.title + " (Copy)",
                        date = System.currentTimeMillis(),
                        category = note.category
                    )

                    // Insert the duplicate note
                    viewModel.insert(duplicateNote)

                    // Show success message
                    Toast.makeText(requireContext(), "Note duplicated", Toast.LENGTH_SHORT).show()

                    // Log for debugging
                    Log.d("HomeTabFragment", "Note duplicated: ${duplicateNote.title}")
                } catch (e: Exception) {
                    // Log the error and show error message
                    Log.e("HomeTabFragment", "Error duplicating note", e)
                    Toast.makeText(requireContext(), "Failed to duplicate note: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )

        binding.recyclerViewNotes.apply {
            adapter = this@HomeTabFragment.adapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupViewModel(category: String) {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[NoteViewModel::class.java]

        viewModel.getNotesByCategory("Home").observe(viewLifecycleOwner) { notes ->
            try {
                // Sort notes by date in descending order (newest first)
                val sortedNotes = notes.sortedByDescending { it.date }
                adapter.submitList(sortedNotes)
                toggleEmptyView(notes.isEmpty())

                // Log for debugging
                Log.d("HomeTabFragment", "Notes updated: ${notes.size} notes")
            } catch (e: Exception) {
                Log.e("HomeTabFragment", "Error updating notes list", e)
            }
        }
    }

    // Add this method to show delete confirmation dialog
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

    private fun toggleEmptyView(isEmpty: Boolean) {
        if (isEmpty) {
            // Show the empty message TextView and illustration
            binding.tvEmptyMessage.visibility = View.VISIBLE
            binding.imageView3.visibility = View.VISIBLE
            // Hide the RecyclerView
            binding.recyclerViewNotes.visibility = View.GONE
        } else {
            // Hide the empty message TextView and illustration
            binding.tvEmptyMessage.visibility = View.GONE
            binding.imageView3.visibility = View.GONE
            // Show the RecyclerView
            binding.recyclerViewNotes.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}