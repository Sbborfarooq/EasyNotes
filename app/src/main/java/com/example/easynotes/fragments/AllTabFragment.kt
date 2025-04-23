package com.example.easynotes.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.easynotes.R
import com.example.easynotes.adapters.NoteAdapter
import com.example.easynotes.databinding.FragmentAllTabBinding
import com.example.easynotes.models.Note
import com.example.easynotes.viewmodels.NoteViewModel

class AllTabFragment : Fragment() {
    private var _binding: FragmentAllTabBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: NoteViewModel
    private lateinit var adapter: NoteAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAllTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupViewModel()
    }

    private fun setupRecyclerView() {
        adapter = NoteAdapter(
            onNoteClick = { note ->
                val bundle = Bundle().apply {
                    putLong("noteId", note.id)
                }
                findNavController().navigate(R.id.action_mainFragment_to_writeFragment, bundle)
            },
            onNoteDelete = { note ->
                showDeleteConfirmationDialog(note)
            },
            onNoteDuplicate = { note ->
                val duplicateNote = note.copy(
                    id = 0,
                    title = note.title + " (Copy)",
                    date = System.currentTimeMillis()
                )
                viewModel.insert(duplicateNote)
                Toast.makeText(requireContext(), "Note duplicated", Toast.LENGTH_SHORT).show()
            }
        )

        binding.recyclerViewNotes.apply {
            adapter = this@AllTabFragment.adapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application))[NoteViewModel::class.java]

        viewModel.allNotes.observe(viewLifecycleOwner) { notes ->
            adapter.submitList(notes)
            toggleEmptyView(notes.isEmpty())
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

    private fun toggleEmptyView(isEmpty: Boolean) {
        if (isEmpty) {
            binding.imageViewIllustration.visibility = View.VISIBLE
            binding.recyclerViewNotes.visibility = View.GONE
        } else {
            binding.imageViewIllustration.visibility = View.GONE
            binding.recyclerViewNotes.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}