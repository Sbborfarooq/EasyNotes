package com.example.easynotes.fragments

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.example.easynotes.R
import com.example.easynotes.databinding.FragmentWriteBinding
import com.example.easynotes.models.Note
import com.example.easynotes.viewmodels.NoteViewModel
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class WriteFragment : Fragment() {
    private  var  _binding: FragmentWriteBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: NoteViewModel
    private lateinit var adView: AdView

    private var currentNoteColor = 0
    private var currentCategory = "Uncategorized"

    // Variables for edit mode
    private var isEditMode = false
    private var existingNoteId = 0L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWriteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentNoteColor = Color.parseColor(getString(R.string.salmon))

        viewModel = ViewModelProvider(
            this, ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)).get(NoteViewModel::class.java)

        // Check if we're editing an existing note
        checkForExistingNote()

        setupUI()
        setupListeners()
        setupAds()
    }

    // NEW METHOD: Check if we're editing an existing note
    private fun checkForExistingNote() {
        val noteId = arguments?.getLong("noteId", -1L) ?: -1L

        if (noteId != -1L) {
            isEditMode = true
            existingNoteId = noteId

            // Load the existing note data
            viewModel.getNoteById(noteId).observe(viewLifecycleOwner) { note ->
                note?.let {
                    // Fill UI with existing note data
                    binding.etTitle.setText(it.title)
                    binding.etContent.setText(it.content)
                    currentCategory = it.category
                    currentNoteColor = it.color

                    // Update UI to reflect loaded data
                    binding.tvCategory.text = currentCategory
                    binding.root.setBackgroundColor(currentNoteColor)

                    // Update the date display with the original note date
                    val dateFormat = SimpleDateFormat("dd/MM, HH:mm a", Locale.getDefault())
                    binding.tvDate.text = dateFormat.format(Date(it.date))
                }
            }
        }
    }

    private fun setupUI() {
        // Only set the current date for new notes
        if (!isEditMode) {
            val dateFormat = SimpleDateFormat("dd/MM, HH:mm a", Locale.getDefault())
            val date = Date()
            binding.tvDate.text = dateFormat.format(date)
        }

        binding.tvCategory.text = currentCategory
        binding.root.setBackgroundColor(currentNoteColor)
    }

    private fun setupListeners() {
        binding.btnCheckmark.setOnClickListener {
            saveNote()
        }

        binding.tvCategory.setOnClickListener {
            showCategoryDialog()
        }
    }

    private fun setupAds() {
        adView = AdView(requireContext()).apply {
            adUnitId = "ca-app-pub-3940256099942544/9214589741"
            setAdSize(AdSize.BANNER)
        }

        binding.adView.removeAllViews()
        binding.adView.addView(adView)

        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        binding.adView.adListener = object : AdListener() {
            override fun onAdClicked() {
                Log.d("AdMob", "Ad clicked")
            }
        }
    }

    private fun saveNote() {
        val title = binding.etTitle.text.toString()
        val content = binding.etContent.text.toString()

        if (title.isBlank() && content.isBlank()) {
            Toast.makeText(context, "Note cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val note = if (isEditMode) {
            // Update existing note
            Note(
                id = existingNoteId,  // Keep the same ID
                title = title,
                content = content,
                category = currentCategory,
                date = System.currentTimeMillis(),  // Update the date to current time
                color = currentNoteColor
            )
        } else {
            // Create new note
            Note(
                title = title,
                content = content,
                category = currentCategory,
                date = System.currentTimeMillis(),
                color = currentNoteColor
            )
        }

        // Insert or update based on edit mode
        if (isEditMode) {
            viewModel.update(note)
            Toast.makeText(context, "Note updated", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.insert(note)
            Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
        }

        Navigation.findNavController(binding.root).navigateUp()
    }

    private fun showCategoryDialog() {
        val categories = arrayOf("Uncategorized", "Home", "Work", "Personal")

        AlertDialog.Builder(requireContext()).setTitle("Select Category").setItems(categories) { _, which ->
            currentCategory = categories[which]
            binding.tvCategory.text = currentCategory
        }
            .show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}