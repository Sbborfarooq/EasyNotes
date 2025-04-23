package com.example.easynotes.fragments

import android.app.AlertDialog
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.easynotes.R
import com.example.easynotes.adapters.NoteAdapter
import com.example.easynotes.databinding.FragmentGenericNotesBinding
import com.example.easynotes.models.Note
import com.example.easynotes.viewmodels.NoteViewModel
import java.util.Locale


class GenericNotesFragment : Fragment() {
    private var _binding: FragmentGenericNotesBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: NoteViewModel
    private lateinit var adapter: NoteAdapter
    private var category: String = "Uncategorized"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            category = it.getString(ARG_TAB_ID) ?: "Uncategorized"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGenericNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupRecyclerView()
        setupViewModel()
    }

    private fun setupViews() {
        // Set title based on category
        binding.titleTextView.text = when (category.lowercase(Locale.getDefault())) {
            "all" -> "All Notes"
            else -> "Notes for $category"
        }

        // Configure empty state view
        binding.emptyStateTextView.text = when (category.lowercase()) {
            "all" -> "No notes yet"
            else -> "No notes in $category"
        }
    }

    private fun setupRecyclerView() {
        adapter = NoteAdapter(
            onNoteClick = { note ->
                val bundle = Bundle().apply {
                    putLong("noteId", note.id)
                }
                findNavController().navigate(
                    R.id.action_mainFragment_to_writeFragment,
                    bundle
                )
            },
            onNoteDelete = { note ->
                showDeleteConfirmationDialog(note)
            },
            onNoteDuplicate = { note ->
                val duplicateNote = note.copy(
                    id = 0,
                    title = note.title + " (Copy)",
                    date = System.currentTimeMillis(),
                    category = note.category // Maintain original category
                )
                viewModel.insert(duplicateNote)
                Toast.makeText(requireContext(), "Note duplicated", Toast.LENGTH_SHORT).show()
            }
        )

        binding.notesRecyclerView.apply {
            adapter = this@GenericNotesFragment.adapter
            layoutManager = LinearLayoutManager(context)
            // Add item decoration if needed
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[NoteViewModel::class.java]

        when (category.lowercase(Locale.getDefault())) {
            "all" -> {
                viewModel.allNotes.observe(viewLifecycleOwner) { notes ->
                    handleNotes(notes)
                }
            }
            "home" -> {
                viewModel.getNotesByCategory("Home").observe(viewLifecycleOwner) { notes ->
                    handleNotes(notes)
                }
            }
            else -> {
                viewModel.getNotesByCategory(category).observe(viewLifecycleOwner) { notes ->
                    handleNotes(notes)
                }
            }
        }
    }

    private fun handleNotes(notes: List<Note>) {
        adapter.submitList(notes)
        toggleEmptyView(notes.isEmpty())
        Log.d("GenericNotesFragment", "Notes updated for $category: ${notes.size} notes")
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

    private fun toggleEmptyView(isEmpty: Boolean) {
        if (isEmpty) {
            binding.apply {
                emptyStateTextView.visibility = View.VISIBLE
                emptyStateTextView.visibility = View.VISIBLE // Make sure you have this in your layout
                notesRecyclerView.visibility = View.GONE
            }
        } else {
            binding.apply {
                emptyStateTextView.visibility = View.GONE
                emptyStateTextView.visibility = View.GONE
                notesRecyclerView.visibility = View.VISIBLE
            }
        }
    }

    companion object {
        const val ARG_TAB_ID = "TAB_ID"
        const val ARG_TAB_TITLE = "TAB_TITLE"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}