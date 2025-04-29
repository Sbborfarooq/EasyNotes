package com.example.easynotes.adapters

import android.content.Intent
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.easynotes.R
import com.example.easynotes.databinding.ItemNoteBinding
import com.example.easynotes.models.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteAdapter(private val onNoteClick: (Note) -> Unit, private val onNoteDelete: (Note) -> Unit ,
                  private val onNoteDuplicate: (Note) -> Unit = {})
    : ListAdapter<Note, NoteAdapter.NoteViewHolder>(NoteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NoteViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // Click listener to open note
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onNoteClick(getItem(position))
                }
            }

            // Add long-press listener to show popup menu
            binding.root.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    showPopupMenu(it, getItem(position))
                }
                true
            }
        }

        fun bind(note: Note) {
            binding.tvNoteTitle.text = note.title
            binding.tvNoteContent.text = note.content
            binding.tvNoteDate.text = formatDate(note.date)
            binding.cardNote.setCardBackgroundColor(note.color)
        }

        private fun formatDate(timestamp: Long): String {
            val dateFormat = SimpleDateFormat("dd/MM, HH:mm a", Locale.getDefault())
            return dateFormat.format(Date(timestamp))
        }

        private fun showPopupMenu(view: View, note: Note) {
            // Create popup with Material theme - use the fully qualified name
            val context = ContextThemeWrapper(view.context, com.google.android.material.R.style.ThemeOverlay_MaterialComponents_Light)
            val popup = PopupMenu(context, view)

            popup.menuInflater.inflate(R.menu.note_actions_menu, popup.menu)

            // Force show icons
            try {
                val method = popup.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                method.isAccessible = true
                method.invoke(popup, true)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_pin -> {
                        // Pin functionality
                        Toast.makeText(view.context, "Pin feature coming soon", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.action_add_lock -> {
                        // Lock functionality
                        Toast.makeText(view.context, "Lock feature coming soon", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.action_duplicate -> {
                        // Call the callback and let the fragment handle the duplication
                        onNoteDuplicate(note)
                        true
                    }
                    R.id.action_add_widget -> {
                        // Widget functionality
                        Toast.makeText(view.context, "Widget feature coming soon", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.action_move -> {
                        // Move functionality
                        Toast.makeText(view.context, "Move feature coming soon", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.action_share -> {
                        // Share functionality - implemented directly in the adapter
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TITLE, note.title)
                            putExtra(Intent.EXTRA_TEXT, "${note.title}\n\n${note.content}")
                            type = "text/plain"
                        }
                        view.context.startActivity(Intent.createChooser(shareIntent, "Share Note"))
                        true
                    }
                    R.id.action_delete -> {
                        onNoteDelete(note)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
        }

    class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }
    }
 }