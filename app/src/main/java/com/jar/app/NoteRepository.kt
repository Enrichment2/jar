package com.jar.app

import androidx.lifecycle.LiveData

class NoteRepository(private val noteDao: NoteDao, private val tagDao: TagDao) {
    val allNotes: LiveData<List<Note>> = noteDao.getAllNotes()
    val allNotesWithTags: LiveData<List<NoteWithTags>> = tagDao.getNotesWithTags()
    val allTags: LiveData<List<Tag>> = tagDao.getAllTags()

    suspend fun insert(note: Note): Long {
        return noteDao.insert(note)
    }

    suspend fun update(note: Note) {
        noteDao.update(note)
    }

    suspend fun delete(note: Note) {
        noteDao.delete(note)
    }

    suspend fun deleteById(noteId: Long) {
        noteDao.deleteById(noteId)
    }

    suspend fun getAllTagsList(): List<Tag> {
        return tagDao.getAllTagsList()
    }

    suspend fun getTagsForNote(noteId: Long): List<Tag> {
        return tagDao.getTagsForNote(noteId)
    }

    suspend fun insertTag(tag: Tag): Long {
        return tagDao.insert(tag)
    }

    suspend fun getOrCreateTag(name: String): Tag {
        val existing = tagDao.getTagByName(name)
        if (existing != null) return existing
        val id = tagDao.insert(Tag(name = name))
        return Tag(id = id, name = name)
    }

    suspend fun addTagToNote(noteId: Long, tagId: Long) {
        tagDao.insertNoteTagCrossRef(NoteTagCrossRef(noteId, tagId))
    }

    suspend fun removeTagFromNote(noteId: Long, tagId: Long) {
        tagDao.deleteNoteTagCrossRef(NoteTagCrossRef(noteId, tagId))
    }

    suspend fun setTagsForNote(noteId: Long, tagIds: List<Long>) {
        tagDao.deleteAllTagsForNote(noteId)
        tagIds.forEach { tagId ->
            tagDao.insertNoteTagCrossRef(NoteTagCrossRef(noteId, tagId))
        }
    }

    fun getNotesByTag(tagId: Long): LiveData<List<NoteWithTags>> {
        return tagDao.getNotesByTag(tagId)
    }
}
