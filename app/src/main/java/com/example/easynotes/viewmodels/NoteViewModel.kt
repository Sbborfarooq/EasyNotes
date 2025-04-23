package com.example.easynotes.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.easynotes.database.AppDatabase
import com.example.easynotes.models.Note
import com.example.easynotes.models.TabInfo
import com.example.easynotes.repositories.NoteRepository
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NoteRepository
    val allNotes: LiveData<List<Note>>

    // Tab management
    private val _tabs = MutableLiveData<List<TabInfo>>()
    val tabs: LiveData<List<TabInfo>> = _tabs

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
        repository.insert(note)
    }

    fun update(note: Note) = viewModelScope.launch {
        repository.update(note)
    }

    fun delete(note: Note) = viewModelScope.launch {
        repository.delete(note)
    }


}