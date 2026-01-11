package com.jar.app

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NoteRepository
    val allNotesWithTags: LiveData<List<NoteWithTags>>
    val allTags: LiveData<List<Tag>>

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _selectedTagId = MutableLiveData<Long?>(null)
    val selectedTagId: LiveData<Long?> = _selectedTagId

    val filteredNotes: MediatorLiveData<List<NoteWithTags>>

    init {
        val db = NoteDatabase.getDatabase(application)
        repository = NoteRepository(db.noteDao(), db.tagDao())
        allNotesWithTags = repository.allNotesWithTags
        allTags = repository.allTags

        filteredNotes = MediatorLiveData<List<NoteWithTags>>().apply {
            fun update() {
                val notes = allNotesWithTags.value ?: emptyList()
                val query = _searchQuery.value?.lowercase() ?: ""
                val tagId = _selectedTagId.value

                value = notes.filter { noteWithTags ->
                    val matchesSearch = query.isEmpty() ||
                        noteWithTags.note.title.lowercase().contains(query) ||
                        noteWithTags.note.content.lowercase().contains(query)

                    val matchesTag = tagId == null ||
                        noteWithTags.tags.any { it.id == tagId }

                    matchesSearch && matchesTag
                }
            }

            addSource(allNotesWithTags) { update() }
            addSource(_searchQuery) { update() }
            addSource(_selectedTagId) { update() }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedTag(tagId: Long?) {
        _selectedTagId.value = tagId
    }

    fun insert(title: String, content: String, tagIds: List<Long> = emptyList(), onComplete: ((Long) -> Unit)? = null) = viewModelScope.launch {
        val note = Note(title = title, content = content)
        val noteId = repository.insert(note)
        if (tagIds.isNotEmpty()) {
            repository.setTagsForNote(noteId, tagIds)
        }
        onComplete?.invoke(noteId)
    }

    fun update(note: Note, tagIds: List<Long>? = null) = viewModelScope.launch {
        val updatedNote = note.copy(updatedAt = System.currentTimeMillis())
        repository.update(updatedNote)
        tagIds?.let {
            repository.setTagsForNote(note.id, it)
        }
    }

    fun delete(note: Note) = viewModelScope.launch {
        repository.delete(note)
    }

    fun deleteById(noteId: Long) = viewModelScope.launch {
        repository.deleteById(noteId)
    }

    suspend fun getTagsForNote(noteId: Long): List<Tag> {
        return repository.getTagsForNote(noteId)
    }

    suspend fun getAllTagsList(): List<Tag> {
        return repository.getAllTagsList()
    }

    suspend fun getOrCreateTag(name: String): Tag {
        return repository.getOrCreateTag(name)
    }

    suspend fun exportToJson(): String {
        return repository.exportToJson()
    }

    suspend fun importFromJson(json: String): ImportResult {
        return repository.importFromJson(json)
    }
}
