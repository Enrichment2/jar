package com.jar.app

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): LiveData<List<Tag>>

    @Query("SELECT * FROM tags ORDER BY name ASC")
    suspend fun getAllTagsList(): List<Tag>

    @Query("SELECT * FROM tags WHERE id = :tagId")
    suspend fun getTagById(tagId: Long): Tag?

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): Tag?

    @Insert
    suspend fun insert(tag: Tag): Long

    @Update
    suspend fun update(tag: Tag)

    @Delete
    suspend fun delete(tag: Tag)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteTagCrossRef(crossRef: NoteTagCrossRef)

    @Delete
    suspend fun deleteNoteTagCrossRef(crossRef: NoteTagCrossRef)

    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId")
    suspend fun deleteAllTagsForNote(noteId: Long)

    @Query("SELECT * FROM tags INNER JOIN note_tag_cross_ref ON tags.id = note_tag_cross_ref.tagId WHERE note_tag_cross_ref.noteId = :noteId")
    suspend fun getTagsForNote(noteId: Long): List<Tag>

    @Transaction
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getNotesWithTags(): LiveData<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id IN (SELECT noteId FROM note_tag_cross_ref WHERE tagId = :tagId) ORDER BY updatedAt DESC")
    fun getNotesByTag(tagId: Long): LiveData<List<NoteWithTags>>

    @Query("SELECT COUNT(*) FROM note_tag_cross_ref WHERE tagId = :tagId")
    suspend fun getTagNoteCount(tagId: Long): Int
}
