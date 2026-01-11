package com.jar.app

import androidx.lifecycle.LiveData
import com.google.gson.Gson
import com.google.gson.GsonBuilder

class NoteRepository(private val noteDao: NoteDao, private val tagDao: TagDao) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
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

    suspend fun exportToJson(): String {
        val notes = noteDao.getAllNotesList()
        val tags = tagDao.getAllTagsList()

        val exportNotes = notes.map { note ->
            val noteTags = tagDao.getTagsForNote(note.id)
            ExportNote(
                title = note.title,
                content = note.content,
                createdAt = note.createdAt,
                updatedAt = note.updatedAt,
                tagNames = noteTags.map { it.name }
            )
        }

        val exportTags = tags.map { tag ->
            ExportTag(name = tag.name, color = tag.color)
        }

        val exportData = ExportData(
            notes = exportNotes,
            tags = exportTags
        )

        return gson.toJson(exportData)
    }

    suspend fun importFromJson(json: String): ImportResult {
        return try {
            val exportData = gson.fromJson(json, ExportData::class.java)
            var notesImported = 0
            var tagsImported = 0

            // Import tags first
            val tagNameToId = mutableMapOf<String, Long>()
            exportData.tags.forEach { exportTag ->
                val existingTag = tagDao.getTagByName(exportTag.name)
                if (existingTag != null) {
                    tagNameToId[exportTag.name] = existingTag.id
                } else {
                    val newTag = Tag(name = exportTag.name, color = exportTag.color)
                    val tagId = tagDao.insert(newTag)
                    tagNameToId[exportTag.name] = tagId
                    tagsImported++
                }
            }

            // Import notes
            exportData.notes.forEach { exportNote ->
                val note = Note(
                    title = exportNote.title,
                    content = exportNote.content,
                    createdAt = exportNote.createdAt,
                    updatedAt = exportNote.updatedAt
                )
                val noteId = noteDao.insert(note)

                // Link tags to note
                exportNote.tagNames.forEach { tagName ->
                    val tagId = tagNameToId[tagName]
                    if (tagId != null) {
                        tagDao.insertNoteTagCrossRef(NoteTagCrossRef(noteId, tagId))
                    }
                }
                notesImported++
            }

            ImportResult.Success(notesImported, tagsImported)
        } catch (e: Exception) {
            ImportResult.Error(e.message ?: "Unknown error")
        }
    }
}

sealed class ImportResult {
    data class Success(val notesImported: Int, val tagsImported: Int) : ImportResult()
    data class Error(val message: String) : ImportResult()
}
