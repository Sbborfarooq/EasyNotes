package com.example.easynotes.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.easynotes.adapters.NoteAdapter
import com.example.easynotes.databinding.FragmentHomeTabBinding
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
                // Handle note click, e.g., navigate to edit
            },
            onNoteDelete = { note ->
                viewModel.delete(note)
                Toast.makeText(context, "Note deleted", Toast.LENGTH_SHORT).show()
            },
            onNoteDuplicate = { note ->
                val duplicateNote = note.copy(
                    id = 0,
                    title = note.title + " (Copy)",
                    date = System.currentTimeMillis(),
                    category = note.category
                )
                viewModel.insert(duplicateNote)
                Toast.makeText(requireContext(), "Note duplicated", Toast.LENGTH_SHORT).show()
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
            adapter.submitList(notes)
            toggleEmptyView(notes.isEmpty())
        }
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