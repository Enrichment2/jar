package com.jar.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NoteRepository
    val allNotes: LiveData<List<Note>>

    init {
        val noteDao = NoteDatabase.getDatabase(application).noteDao()
        repository = NoteRepository(noteDao)
        allNotes = repository.allNotes
    }

    fun insert(content: String) = viewModelScope.launch {
        val note = Note(content = content)
        repository.insert(note)
    }

    fun update(note: Note) = viewModelScope.launch {
        val updatedNote = note.copy(updatedAt = System.currentTimeMillis())
        repository.update(updatedNote)
    }

    fun delete(note: Note) = viewModelScope.launch {
        repository.delete(note)
    }
}
