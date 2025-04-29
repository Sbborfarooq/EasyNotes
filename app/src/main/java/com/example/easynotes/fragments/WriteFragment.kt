package com.example.easynotes.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import java.util.Stack
import androidx.navigation.fragment.findNavController
import com.example.easynotes.R
import com.example.easynotes.databinding.FragmentWriteBinding
import com.example.easynotes.models.Note
import com.example.easynotes.viewmodels.NoteViewModel
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class WriteFragment : Fragment() {
    private var _binding: FragmentWriteBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: NoteViewModel
    private lateinit var adView: AdView

    private var currentNoteColor = 0
    private var currentCategory = "Uncategorized"

    private var isEditMode = false
    private var existingNoteId = 0L

    private data class TextState(
        val title: String,
        val content: String,
        val titleCursorPosition: Int,
        val contentCursorPosition: Int
    )

    private val undoStack = Stack<TextState>()
    private val redoStack = Stack<TextState>()
    private var isUndoRedoAction = false
    private val handler = Handler(Looper.getMainLooper())
    private var textChangeRunnable: Runnable? = null

    private var selectedNoteDate: Long? = null


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

        // Initialize the undo stack with the initial state
        undoStack.push(TextState(
            title = binding.etTitle.text.toString(),
            content = binding.etContent.text.toString(),
            titleCursorPosition = binding.etTitle.selectionStart,
            contentCursorPosition = binding.etContent.selectionStart
        ))

        addTextWatchers()

        // Initialize button states
        updateUndoRedoButtonStates()
    }

    private fun addTextWatchers() {
        val titleWatcher = object : TextWatcher {
            private var beforeText = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (!isUndoRedoAction) {
                    beforeText = s.toString()
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Cancel any pending text change runnables
                textChangeRunnable?.let { handler.removeCallbacks(it) }
            }

            override fun afterTextChanged(s: Editable?) {
                if (!isUndoRedoAction && isAdded && !isRemoving) {
                    textChangeRunnable = Runnable {
                        if (isAdded && _binding != null) {  // Check if fragment is still attached and binding is valid
                            val currentState = TextState(
                                title = binding.etTitle.text.toString(),
                                content = binding.etContent.text.toString(),
                                titleCursorPosition = binding.etTitle.selectionStart,
                                contentCursorPosition = binding.etContent.selectionStart
                            )

                            // Only add to stack if there's an actual change
                            if (undoStack.isEmpty() || undoStack.peek().title != currentState.title ||
                                undoStack.peek().content != currentState.content) {
                                undoStack.push(currentState)
                                redoStack.clear() // Clear redo stack when new change is made
                                updateUndoRedoButtonStates()
                            }
                        }
                    }
                    handler.postDelayed(textChangeRunnable!!, 100) // Shorter delay for more granular changes
                }

                // Update button states based on content
                if (isAdded && _binding != null) {
                    updateUndoRedoButtonStates()
                }
            }
        }

        val contentWatcher = object : TextWatcher {
            private var beforeText = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (!isUndoRedoAction) {
                    beforeText = s.toString()
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Cancel any pending text change runnables
                textChangeRunnable?.let { handler.removeCallbacks(it) }
            }

            override fun afterTextChanged(s: Editable?) {
                if (!isUndoRedoAction && isAdded && !isRemoving) {
                    textChangeRunnable = Runnable {
                        if (isAdded && _binding != null) {  // Check if fragment is still attached and binding is valid
                            val currentState = TextState(
                                title = binding.etTitle.text.toString(),
                                content = binding.etContent.text.toString(),
                                titleCursorPosition = binding.etTitle.selectionStart,
                                contentCursorPosition = binding.etContent.selectionStart
                            )

                            // Only add to stack if there's an actual change
                            if (undoStack.isEmpty() || undoStack.peek().title != currentState.title ||
                                undoStack.peek().content != currentState.content) {
                                undoStack.push(currentState)
                                redoStack.clear() // Clear redo stack when new change is made
                                updateUndoRedoButtonStates()
                            }
                        }
                    }
                    handler.postDelayed(textChangeRunnable!!, 100) // Shorter delay for more granular changes
                }

                // Update button states based on content
                if (isAdded && _binding != null) {
                    updateUndoRedoButtonStates()
                }
            }
        }

        binding.etTitle.addTextChangedListener(titleWatcher)
        binding.etContent.addTextChangedListener(contentWatcher)
    }

    private fun updateUndoRedoButtonStates() {
        if (_binding == null) return  // Safety check

        // Check if there's content to determine button states
        val hasContent = binding.etTitle.text.isNotEmpty() || binding.etContent.text.isNotEmpty()

        // For undo button: enabled if there's content and more than one state in the undo stack
        val canUndo = hasContent && undoStack.size > 1
        binding.btnUndo.alpha = if (canUndo) 1.0f else 0.5f
        binding.btnUndo.isEnabled = canUndo

        // For redo button: enabled if there are states in the redo stack
        val canRedo = redoStack.isNotEmpty()
        binding.btnRedo.alpha = if (canRedo) 1.0f else 0.5f
        binding.btnRedo.isEnabled = canRedo
    }

    private fun undo() {
        if (_binding == null) return  // Safety check

        if (undoStack.size > 1) { // Keep at least one state to prevent empty stack
            val currentState = TextState(
                title = binding.etTitle.text.toString(),
                content = binding.etContent.text.toString(),
                titleCursorPosition = binding.etTitle.selectionStart,
                contentCursorPosition = binding.etContent.selectionStart
            )

            redoStack.push(currentState) // Save current state to redo stack
            undoStack.pop() // Remove current state
            val previousState = undoStack.peek() // Get the previous state

            isUndoRedoAction = true

            binding.etTitle.setText(previousState.title)
            binding.etContent.setText(previousState.content)

            // Restore cursor positions
            if (previousState.titleCursorPosition <= previousState.title.length) {
                binding.etTitle.setSelection(previousState.titleCursorPosition)
            }

            if (previousState.contentCursorPosition <= previousState.content.length) {
                binding.etContent.setSelection(previousState.contentCursorPosition)
            }

            isUndoRedoAction = false
            updateUndoRedoButtonStates()
        }
    }

    private fun redo() {
        if (_binding == null) return  // Safety check

        if (redoStack.isNotEmpty()) {
            val currentState = TextState(
                title = binding.etTitle.text.toString(),
                content = binding.etContent.text.toString(),
                titleCursorPosition = binding.etTitle.selectionStart,
                contentCursorPosition = binding.etContent.selectionStart
            )

            undoStack.push(currentState) // Save current state to undo stack
            val nextState = redoStack.pop() // Get the next state

            isUndoRedoAction = true

            binding.etTitle.setText(nextState.title)
            binding.etContent.setText(nextState.content)

            // Restore cursor positions
            if (nextState.titleCursorPosition <= nextState.title.length) {
                binding.etTitle.setSelection(nextState.titleCursorPosition)
            }

            if (nextState.contentCursorPosition <= nextState.content.length) {
                binding.etContent.setSelection(nextState.contentCursorPosition)
            }

            isUndoRedoAction = false
            updateUndoRedoButtonStates()
        }
    }

    private fun checkForExistingNote() {
        val noteId = arguments?.getLong("noteId", -1L) ?: -1L
        val selectedDateString = arguments?.getString("selectedDate")

        if (noteId != -1L) {
            // Editing an existing note
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

                    // Reset undo/redo stacks with the loaded note
                    undoStack.clear()
                    redoStack.clear()
                    undoStack.push(TextState(
                        title = it.title,
                        content = it.content,
                        titleCursorPosition = 0,
                        contentCursorPosition = 0
                    ))

                    // Update button states
                    updateUndoRedoButtonStates()
                }
            }
        } else if (selectedDateString != null) {
            // Creating a new note from calendar with a specific date
            isEditMode = false

            try {
                // Parse the selected date string
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val selectedDate = dateFormat.parse(selectedDateString)

                if (selectedDate != null) {
                    // Set the time to current time (keeping the selected date)
                    val calendar = Calendar.getInstance()
                    val timeCalendar = Calendar.getInstance()

                    calendar.time = selectedDate
                    // Set hours, minutes, seconds from current time
                    calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                    calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                    calendar.set(Calendar.SECOND, timeCalendar.get(Calendar.SECOND))

                    // Store this date to use when saving the note
                    selectedNoteDate = calendar.timeInMillis

                    // Update the date display
                    val displayFormat = SimpleDateFormat("dd/MM, HH:mm a", Locale.getDefault())
                    binding.tvDate.text = displayFormat.format(calendar.time)

                    // Log for debugging
                    Log.d("WriteFragment", "Creating note for selected date: $selectedDateString")
                }
            } catch (e: Exception) {
                Log.e("WriteFragment", "Error parsing date: $selectedDateString", e)

                // Fallback to current date/time if there's an error
                val currentDate = Date()
                val dateFormat = SimpleDateFormat("dd/MM, HH:mm a", Locale.getDefault())
                binding.tvDate.text = dateFormat.format(currentDate)
            }
        } else {
            // Regular new note (not from calendar)
            isEditMode = false

            // Set current date/time
            val currentDate = Date()
            val dateFormat = SimpleDateFormat("dd/MM, HH:mm a", Locale.getDefault())
            binding.tvDate.text = dateFormat.format(currentDate)
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
        binding.btnUndo.setOnClickListener {
            undo()
        }

        binding.btnRedo.setOnClickListener {
            redo()
        }
        binding.btnShare.setOnClickListener {
            showShareOptionsDialog()
        }
    }

    private fun showShareOptionsDialog() {
        if (_binding == null) return  // Safety check

        val shareDialog = Dialog(requireContext())
        shareDialog.setContentView(R.layout.share_options_dialog)

        // Set dialog width to be narrower and centered
        val window = shareDialog.window
        if (window != null) {
            // Get the screen width
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels

            // Set dialog width to 80% of screen width
            val width = (screenWidth * 0.8).toInt()

            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setGravity(Gravity.CENTER)
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        val pictureOption = shareDialog.findViewById<View>(R.id.pictureOption)
        val textOnlyOption = shareDialog.findViewById<View>(R.id.textOnlyOption)

        pictureOption.setOnClickListener {
            shareDialog.dismiss()
            shareAsImage()
        }

        textOnlyOption.setOnClickListener {
            shareDialog.dismiss()
            shareAsText()
        }

        shareDialog.show()
    }

    private fun shareAsImage() {
        if (_binding == null) return  // Safety check

        try {
            // Get the ScrollView that contains the note content
            val scrollView = binding.scrollView

            // Create a bitmap of the visible area first
            val visibleBitmap = Bitmap.createBitmap(
                scrollView.width,
                scrollView.height,
                Bitmap.Config.ARGB_8888
            )

            // Draw the current view content
            val canvas = Canvas(visibleBitmap)
            canvas.drawColor(currentNoteColor) // Use the current note color as background
            scrollView.draw(canvas)

            // Save bitmap to cache directory
            val cachePath = File(requireContext().cacheDir, "images")
            cachePath.mkdirs() // Make sure directory exists

            val fileName = "note_image_${System.currentTimeMillis()}.png"
            val filePath = File(cachePath, fileName)

            FileOutputStream(filePath).use { out ->
                visibleBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }

            // Create content URI using FileProvider
            val contentUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                filePath
            )

            // Share the image
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share note as image"))

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("WriteFragment", "Error sharing image: ${e.message}")
            Toast.makeText(
                requireContext(),
                "Error sharing image: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun shareAsText() {
        if (_binding == null) return  // Safety check

        val title = binding.etTitle.text.toString()
        val content = binding.etContent.text.toString()
        val textToShare = if (content.isNotEmpty()) "$title\n\n$content" else title

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, textToShare)
        startActivity(Intent.createChooser(shareIntent, "Share note as text"))
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
        if (_binding == null) return  // Safety check

        try {
            val title = binding.etTitle.text.toString().trim()
            val content = binding.etContent.text.toString().trim()

            if (title.isBlank() && content.isBlank()) {
                Toast.makeText(context, "Note cannot be empty", Toast.LENGTH_SHORT).show()
                return
            }

            // Create a safe title if empty
            val safeTitle = if (title.isBlank()) {
                val contentPreview = content.take(20) + if (content.length > 20) "..." else ""
                contentPreview
            } else {
                title
            }

            // Use selectedNoteDate if available, otherwise use current time
            val noteDate = selectedNoteDate ?: System.currentTimeMillis()

            // Generate date string for filtering
            val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(noteDate))

            val note = if (isEditMode) {
                Note(
                    id = existingNoteId,
                    title = safeTitle,
                    content = content,
                    category = currentCategory,
                    date = noteDate,
                    color = currentNoteColor,
                    dateString = dateString
                )
            } else {
                Note(
                    title = safeTitle,
                    content = content,
                    category = currentCategory,
                    date = noteDate,
                    color = currentNoteColor,
                    dateString = dateString
                )
            }

            Log.d("WriteFragment", "Saving note: ID=${note.id}, Title=${note.title}, Date=${note.dateString}")

            if (isEditMode) {
                viewModel.update(note)
            } else {
                viewModel.insert(note)
            }

            Toast.makeText(context, if (isEditMode) "Note updated" else "Note saved", Toast.LENGTH_SHORT).show()

            // Remove all callbacks to prevent accessing binding after fragment is destroyed
            handler.removeCallbacksAndMessages(null)

            // Navigate back
            findNavController().navigateUp()

        } catch (e: Exception) {
            Log.e("WriteFragment", "Error saving note", e)
            Toast.makeText(context, "Error saving note: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showCategoryDialog() {
        if (_binding == null) return  // Safety check

        val categories = arrayOf("Uncategorized", "Home", "Work")

        AlertDialog.Builder(requireContext())
            .setTitle("Select Category")
            .setItems(categories) { _, which ->
                currentCategory = categories[which]
                binding.tvCategory.text = currentCategory
                Toast.makeText(context, "Category set to: $currentCategory", Toast.LENGTH_SHORT).show()
                Log.d("WriteFragment", "Selected category: $currentCategory")
            }
            .show()
    }

    override fun onPause() {
        super.onPause()
        // Remove all callbacks when the fragment is paused
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove all callbacks when the view is destroyed
        handler.removeCallbacksAndMessages(null)
        // Clear the binding
        _binding = null
    }
}