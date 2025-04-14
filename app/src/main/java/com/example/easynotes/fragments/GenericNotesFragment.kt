package com.example.easynotes.fragments

import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.easynotes.databinding.FragmentGenericNotesBinding


class GenericNotesFragment : Fragment() {

    private var _binding: FragmentGenericNotesBinding? = null
    private val binding get() = _binding!!

    private var tabId: String? = null
    private var tabTitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tabId = it.getString("TAB_ID")
            tabTitle = it.getString("TAB_TITLE")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGenericNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up your fragment with the tab information
        binding.titleTextView.text = "Notes for $tabTitle"

        // Load content based on tabId
        // This would typically involve loading notes from your database
        // that are tagged with this tab's ID
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}