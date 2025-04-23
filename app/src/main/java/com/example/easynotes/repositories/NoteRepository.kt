package com.example.easynotes.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.example.easynotes.database.dao.NoteDao
import com.example.easynotes.models.Note
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    val allNotes: LiveData<List<Note>> = noteDao.getAllNotes().asLiveData()

    fun getNotesByCategory(category: String): LiveData<List<Note>> =
            noteDao.getNotesByCategory(category).asLiveData()


    fun getNoteById(noteId: Long): Flow<Note> {
        return noteDao.getNoteById(noteId)
    }

    suspend fun insert(note: Note): Long {
        return noteDao.insertNote(note)
    }

    suspend fun update(note: Note) {
        noteDao.updateNote(note)
    }

    suspend fun delete(note: Note) {
        noteDao.deleteNote(note)
    }
}