package com.jar.app

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jar.app.databinding.ActivityNoteEditorBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NoteEditorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNoteEditorBinding
    private lateinit var viewModel: NoteViewModel

    private var noteId: Long = -1
    private var originalTitle: String = ""
    private var originalContent: String = ""
    private var isNewNote: Boolean = true
    private val selectedTags = mutableListOf<Tag>()
    private var originalTagIds = listOf<Long>()

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
        setupTagChips()
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

            lifecycleScope.launch {
                val tags = viewModel.getTagsForNote(noteId)
                selectedTags.addAll(tags)
                originalTagIds = tags.map { it.id }
                updateTagChips()
            }
        }
    }

    private fun setupTagChips() {
        updateTagChips()
    }

    private fun updateTagChips() {
        binding.chipGroupTags.removeAllViews()

        selectedTags.forEach { tag ->
            val chip = Chip(this).apply {
                text = tag.name
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    selectedTags.remove(tag)
                    updateTagChips()
                }
            }
            binding.chipGroupTags.addView(chip)
        }

        val addChip = Chip(this).apply {
            text = "Add tag"
            chipIcon = getDrawable(R.drawable.ic_add_tag)
            isChipIconVisible = true
            setOnClickListener {
                showAddTagDialog()
            }
        }
        binding.chipGroupTags.addView(addChip)
    }

    private fun showAddTagDialog() {
        lifecycleScope.launch {
            val allTags = viewModel.getAllTagsList()
            val availableTags = allTags.filter { tag -> selectedTags.none { it.id == tag.id } }

            val options = availableTags.map { it.name }.toMutableList()
            options.add("+ Create new tag")

            MaterialAlertDialogBuilder(this@NoteEditorActivity)
                .setTitle("Add Tag")
                .setItems(options.toTypedArray()) { _, which ->
                    if (which == options.size - 1) {
                        showCreateTagDialog()
                    } else {
                        selectedTags.add(availableTags[which])
                        updateTagChips()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showCreateTagDialog() {
        val editText = EditText(this).apply {
            hint = "Tag name"
            setPadding(48, 32, 48, 32)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Create Tag")
            .setView(editText)
            .setPositiveButton("Create") { _, _ ->
                val tagName = editText.text.toString().trim()
                if (tagName.isNotEmpty()) {
                    lifecycleScope.launch {
                        val tag = viewModel.getOrCreateTag(tagName)
                        if (selectedTags.none { it.id == tag.id }) {
                            selectedTags.add(tag)
                            updateTagChips()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()

        editText.requestFocus()
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
        val tagIds = selectedTags.map { it.id }

        if (title.isEmpty() && content.isEmpty()) {
            if (!isNewNote) {
                viewModel.deleteById(noteId)
            }
            finish()
            return
        }

        if (isNewNote) {
            viewModel.insert(title, content, tagIds)
        } else {
            val tagsChanged = tagIds.toSet() != originalTagIds.toSet()
            if (title != originalTitle || content != originalContent || tagsChanged) {
                viewModel.update(
                    Note(
                        id = noteId,
                        title = title,
                        content = content,
                        updatedAt = System.currentTimeMillis()
                    ),
                    tagIds
                )
            }
        }
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        saveAndFinish()
    }
}
