package com.jar.app

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jar.app.databinding.ActivityNoteEditorBinding
import java.text.SimpleDateFormat
import java.util.*

class NoteEditorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNoteEditorBinding
    private lateinit var viewModel: NoteViewModel

    private var noteId: Long = -1
    private var originalTitle: String = ""
    private var originalContent: String = ""
    private var isNewNote: Boolean = true

    companion object {
        const val EXTRA_NOTE_ID = "note_id"
        const val EXTRA_NOTE_TITLE = "note_title"
        const val EXTRA_NOTE_CONTENT = "note_content"
        const val EXTRA_NOTE_UPDATED_AT = "note_updated_at"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[NoteViewModel::class.java]

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.toolbar.setNavigationOnClickListener {
            saveAndFinish()
        }

        loadNoteFromIntent()
    }

    private fun loadNoteFromIntent() {
        noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1)

        if (noteId != -1L) {
            isNewNote = false
            originalTitle = intent.getStringExtra(EXTRA_NOTE_TITLE) ?: ""
            originalContent = intent.getStringExtra(EXTRA_NOTE_CONTENT) ?: ""
            val updatedAt = intent.getLongExtra(EXTRA_NOTE_UPDATED_AT, 0)

            binding.editTitle.setText(originalTitle)
            binding.editContent.setText(originalContent)

            if (updatedAt > 0) {
                binding.textLastEdited.visibility = android.view.View.VISIBLE
                binding.textLastEdited.text = "Last edited ${formatDate(updatedAt)}"
            }
        }
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (!isNewNote) {
            menuInflater.inflate(R.menu.menu_editor, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete -> {
                showDeleteConfirmation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Note")
            .setMessage("Are you sure you want to delete this note?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteById(noteId)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveAndFinish() {
        val title = binding.editTitle.text.toString().trim()
        val content = binding.editContent.text.toString().trim()

        if (title.isEmpty() && content.isEmpty()) {
            if (!isNewNote) {
                viewModel.deleteById(noteId)
            }
            finish()
            return
        }

        if (isNewNote) {
            viewModel.insert(title, content)
        } else {
            if (title != originalTitle || content != originalContent) {
                viewModel.update(Note(
                    id = noteId,
                    title = title,
                    content = content,
                    updatedAt = System.currentTimeMillis()
                ))
            }
        }
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        saveAndFinish()
    }
}
