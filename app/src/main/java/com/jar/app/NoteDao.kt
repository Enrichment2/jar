package com.jar.app

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): LiveData<List<Note>>
    
    @Insert
    suspend fun insert(note: Note): Long
    
    @Update
    suspend fun update(note: Note)
    
    @Delete
    suspend fun delete(note: Note)
    
    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteById(noteId: Long)
}
