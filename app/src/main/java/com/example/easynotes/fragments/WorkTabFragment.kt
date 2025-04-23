package com.example.easynotes.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.easynotes.R
import com.example.easynotes.adapters.NoteAdapter
import com.example.easynotes.databinding.FragmentWorkTabBinding
import com.example.easynotes.viewmodels.NoteViewModel


class WorkTabFragment : Fragment() {

    private var _binding: FragmentWorkTabBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: NoteViewModel
    private lateinit var adapter: NoteAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkTabBinding.inflate(inflater, container, false)
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
                // Handle note click, e.g., navigate to edit
            },
            onNoteDelete = { note ->
                viewModel.delete(note)
                Toast.makeText(context, "Note deleted", Toast.LENGTH_SHORT).show()
            }
        )

        binding.recyclerViewNotes.apply {
            adapter = this@WorkTabFragment.adapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[NoteViewModel::class.java]

        viewModel.getNotesByCategory("Work").observe(viewLifecycleOwner) { notes ->
            adapter.submitList(notes)
            toggleEmptyView(notes.isEmpty())
        }
    }

    private fun toggleEmptyView(isEmpty: Boolean) {
        if (isEmpty) {
            binding.imageView4.visibility = View.VISIBLE
            binding.recyclerViewNotes.visibility = View.GONE
        } else {
            binding.imageView4.visibility = View.GONE
            binding.recyclerViewNotes.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}