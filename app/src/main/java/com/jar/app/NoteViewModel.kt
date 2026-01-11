package com.jar.app

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import kotlinx.coroutines.launch

enum class SortOrder {
    DATE_MODIFIED,
    DATE_CREATED,
    ALPHABETICAL
}

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NoteRepository
    val allNotesWithTags: LiveData<List<NoteWithTags>>
    val allTags: LiveData<List<Tag>>

    private val prefs = application.getSharedPreferences("jar_prefs", Context.MODE_PRIVATE)

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _selectedTagId = MutableLiveData<Long?>(null)
    val selectedTagId: LiveData<Long?> = _selectedTagId

    private val _sortOrder = MutableLiveData(loadSortOrder())
    val sortOrder: LiveData<SortOrder> = _sortOrder

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
                val sort = _sortOrder.value ?: SortOrder.DATE_MODIFIED

                var filtered = notes.filter { noteWithTags ->
                    val matchesSearch = query.isEmpty() ||
                        noteWithTags.note.title.lowercase().contains(query) ||
                        noteWithTags.note.content.lowercase().contains(query) ||
                        noteWithTags.tags.any { it.name.lowercase().contains(query) }

                    val matchesTag = tagId == null ||
                        noteWithTags.tags.any { it.id == tagId }

                    matchesSearch && matchesTag
                }

                filtered = when (sort) {
                    SortOrder.DATE_MODIFIED -> filtered.sortedByDescending { it.note.updatedAt }
                    SortOrder.DATE_CREATED -> filtered.sortedByDescending { it.note.createdAt }
                    SortOrder.ALPHABETICAL -> filtered.sortedBy {
                        it.note.title.ifEmpty { it.note.content }.lowercase()
                    }
                }

                value = filtered
            }

            addSource(allNotesWithTags) { update() }
            addSource(_searchQuery) { update() }
            addSource(_selectedTagId) { update() }
            addSource(_sortOrder) { update() }
        }
    }

    private fun loadSortOrder(): SortOrder {
        val ordinal = prefs.getInt("sort_order", SortOrder.DATE_MODIFIED.ordinal)
        return SortOrder.entries.getOrNull(ordinal) ?: SortOrder.DATE_MODIFIED
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedTag(tagId: Long?) {
        _selectedTagId.value = tagId
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        prefs.edit().putInt("sort_order", order.ordinal).apply()
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

    fun deleteMultiple(noteIds: List<Long>) = viewModelScope.launch {
        noteIds.forEach { repository.deleteById(it) }
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

    fun updateTag(tag: Tag) = viewModelScope.launch {
        repository.updateTag(tag)
    }

    fun deleteTag(tag: Tag) = viewModelScope.launch {
        repository.deleteTag(tag)
    }

    suspend fun getTagNoteCount(tagId: Long): Int {
        return repository.getTagNoteCount(tagId)
    }

    suspend fun exportToJson(): String {
        return repository.exportToJson()
    }

    suspend fun importFromJson(json: String): ImportResult {
        return repository.importFromJson(json)
    }
}
