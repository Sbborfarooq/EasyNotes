package com.example.easynotes.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.easynotes.database.AppDatabase
import com.example.easynotes.models.Note
import com.example.easynotes.models.TabInfo
import com.example.easynotes.repositories.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NoteRepository
    val allNotes: LiveData<List<Note>>

    // Tab management
    private val _tabs = MutableLiveData<List<TabInfo>>()
    val tabs: LiveData<List<TabInfo>> = _tabs

    // Status for operations
    private val _operationStatus = MutableLiveData<String>()
    val operationStatus: LiveData<String> = _operationStatus

    init {
        val noteDao = AppDatabase.getDatabase(application).noteDao()
        repository = NoteRepository(noteDao)
        allNotes = repository.allNotes
    }

    // Note-related functions
    fun getNotesByCategory(category: String): LiveData<List<Note>> =
        repository.getNotesByCategory(category)

    fun getNoteById(id: Long): LiveData<Note> {
        return repository.getNoteById(id).asLiveData()
    }

    fun insert(note: Note) = viewModelScope.launch {
        try {
            val newId = withContext(Dispatchers.IO) {
                repository.insert(note)
            }
            Log.d("NoteViewModel", "Note inserted with ID: $newId")
            _operationStatus.postValue("Note inserted successfully")
        } catch (e: Exception) {
            Log.e("NoteViewModel", "Error inserting note", e)
            _operationStatus.postValue("Failed to insert note: ${e.message}")
        }
    }

    fun update(note: Note) = viewModelScope.launch {
        try {
            withContext(Dispatchers.IO) {
                repository.update(note)
            }
            Log.d("NoteViewModel", "Note updated successfully: ${note.id}")
            _operationStatus.postValue("Note updated successfully")
        } catch (e: Exception) {
            Log.e("NoteViewModel", "Error updating note", e)
            _operationStatus.postValue("Failed to update note: ${e.message}")
        }
    }

    fun delete(note: Note) = viewModelScope.launch {
        try {
            withContext(Dispatchers.IO) {
                repository.delete(note)
            }
            _operationStatus.postValue("Note deleted successfully")
        } catch (e: Exception) {
            Log.e("NoteViewModel", "Error deleting note", e)
            _operationStatus.postValue("Failed to delete note: ${e.message}")
        }
    }

    fun getNotesByDate(dateString: String): LiveData<List<Note>> {
        return repository.getNotesByDate(dateString).asLiveData()
    }
}