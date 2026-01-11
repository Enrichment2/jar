package com.jar.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jar.app.databinding.ActivityNoteEditorBinding
import com.jar.app.databinding.DialogEditTagBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NoteEditorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNoteEditorBinding
    private lateinit var viewModel: NoteViewModel
    private lateinit var richTextEditor: RichTextEditor

    private var noteId: Long = -1
    private var originalTitle: String = ""
    private var originalContent: String = ""
    private var createdAt: Long = 0
    private var isNewNote: Boolean = true
    private val selectedTags = mutableListOf<Tag>()
    private var originalTagIds = listOf<Long>()

    companion object {
        const val EXTRA_NOTE_ID = "note_id"
        const val EXTRA_NOTE_TITLE = "note_title"
        const val EXTRA_NOTE_CONTENT = "note_content"
        const val EXTRA_NOTE_UPDATED_AT = "note_updated_at"
        const val EXTRA_NOTE_CREATED_AT = "note_created_at"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[NoteViewModel::class.java]
        richTextEditor = RichTextEditor(binding.editContent)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.toolbar.setNavigationOnClickListener {
            saveAndFinish()
        }

        loadNoteFromIntent()
        setupTagChips()
        setupFormatToolbar()
    }

    private fun setupFormatToolbar() {
        binding.btnBold.setOnClickListener {
            richTextEditor.toggleBold()
            updateFormatButtonStates()
        }
        binding.btnItalic.setOnClickListener {
            richTextEditor.toggleItalic()
            updateFormatButtonStates()
        }
        binding.btnUnderline.setOnClickListener {
            richTextEditor.toggleUnderline()
            updateFormatButtonStates()
        }
        binding.btnStrikethrough.setOnClickListener {
            richTextEditor.toggleStrikethrough()
            updateFormatButtonStates()
        }
        binding.btnBulletList.setOnClickListener { richTextEditor.insertBulletPoint() }
        binding.btnNumberedList.setOnClickListener { richTextEditor.insertNumberedItem() }
        binding.btnCheckbox.setOnClickListener { richTextEditor.insertCheckbox() }
        binding.btnIndentDecrease.setOnClickListener { richTextEditor.decreaseIndent() }
        binding.btnIndentIncrease.setOnClickListener { richTextEditor.increaseIndent() }
    }

    private fun updateFormatButtonStates() {
        val activeColor = getColor(com.google.android.material.R.color.design_default_color_primary)
        val defaultColor = getColor(com.google.android.material.R.color.material_on_surface_emphasis_medium)

        binding.btnBold.imageTintList = ColorStateList.valueOf(
            if (richTextEditor.isBoldActive) activeColor else defaultColor
        )
        binding.btnItalic.imageTintList = ColorStateList.valueOf(
            if (richTextEditor.isItalicActive) activeColor else defaultColor
        )
        binding.btnUnderline.imageTintList = ColorStateList.valueOf(
            if (richTextEditor.isUnderlineActive) activeColor else defaultColor
        )
        binding.btnStrikethrough.imageTintList = ColorStateList.valueOf(
            if (richTextEditor.isStrikethroughActive) activeColor else defaultColor
        )
    }

    private fun loadNoteFromIntent() {
        noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1)

        if (noteId != -1L) {
            isNewNote = false
            originalTitle = intent.getStringExtra(EXTRA_NOTE_TITLE) ?: ""
            originalContent = intent.getStringExtra(EXTRA_NOTE_CONTENT) ?: ""
            val updatedAt = intent.getLongExtra(EXTRA_NOTE_UPDATED_AT, 0)
            createdAt = intent.getLongExtra(EXTRA_NOTE_CREATED_AT, 0)

            binding.editTitle.setText(originalTitle)

            // Load content - try as HTML first, fallback to plain text
            if (originalContent.contains("<") && originalContent.contains(">")) {
                richTextEditor.fromHtml(originalContent)
            } else {
                binding.editContent.setText(originalContent)
            }

            if (updatedAt > 0) {
                binding.textLastEdited.visibility = View.VISIBLE
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

                if (tag.color != 0) {
                    chipBackgroundColor = ColorStateList.valueOf(tag.color)
                    setTextColor(TagColors.getContrastColor(tag.color))
                    closeIconTint = ColorStateList.valueOf(TagColors.getContrastColor(tag.color))
                }

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
        val dialogBinding = DialogEditTagBinding.inflate(LayoutInflater.from(this))
        var selectedColor = TagColors.PRESET_COLORS[0]

        setupColorPicker(dialogBinding.colorContainer, selectedColor) { color ->
            selectedColor = color
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Create Tag")
            .setView(dialogBinding.root)
            .setPositiveButton("Create") { _, _ ->
                val tagName = dialogBinding.editName.text.toString().trim()
                if (tagName.isNotEmpty()) {
                    lifecycleScope.launch {
                        val tag = viewModel.getOrCreateTag(tagName)
                        val coloredTag = tag.copy(color = selectedColor)
                        viewModel.updateTag(coloredTag)
                        if (selectedTags.none { it.id == coloredTag.id }) {
                            selectedTags.add(coloredTag)
                            updateTagChips()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupColorPicker(
        container: LinearLayout,
        currentColor: Int,
        onColorSelected: (Int) -> Unit
    ) {
        container.removeAllViews()

        var selectedView: View? = null

        TagColors.PRESET_COLORS.forEach { color ->
            val colorView = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(48.dpToPx(), 48.dpToPx()).apply {
                    marginEnd = 8.dpToPx()
                }
                background = getDrawable(R.drawable.shape_circle)
                backgroundTintList = ColorStateList.valueOf(color)

                if (color == currentColor) {
                    selectedView = this
                    alpha = 1f
                    scaleX = 1.2f
                    scaleY = 1.2f
                } else {
                    alpha = 0.7f
                }

                setOnClickListener { view ->
                    selectedView?.let {
                        it.alpha = 0.7f
                        it.scaleX = 1f
                        it.scaleY = 1f
                    }
                    view.alpha = 1f
                    view.scaleX = 1.2f
                    view.scaleY = 1.2f
                    selectedView = view
                    onColorSelected(color)
                }
            }
            container.addView(colorView)
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
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
            R.id.action_share -> {
                shareNote()
                true
            }
            R.id.action_delete -> {
                showDeleteConfirmation()
                true
            }
            R.id.action_info -> {
                showNoteInfo()
                true
            }
            R.id.action_copy -> {
                copyNoteToClipboard()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun shareNote() {
        val title = binding.editTitle.text.toString().trim()
        val content = richTextEditor.getPlainText().trim()

        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(this, "Nothing to share", Toast.LENGTH_SHORT).show()
            return
        }

        val shareText = buildString {
            if (title.isNotEmpty()) {
                append(title)
                if (content.isNotEmpty()) {
                    append("\n\n")
                }
            }
            if (content.isNotEmpty()) {
                append(content)
            }
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            if (title.isNotEmpty()) {
                putExtra(Intent.EXTRA_SUBJECT, title)
            }
        }
        startActivity(Intent.createChooser(intent, "Share note"))
    }

    private fun showNoteInfo() {
        val title = binding.editTitle.text.toString()
        val content = richTextEditor.getPlainText()
        val fullText = "$title $content".trim()

        val wordCount = if (fullText.isEmpty()) 0 else fullText.split("\\s+".toRegex()).size
        val charCount = fullText.length
        val charCountNoSpaces = fullText.replace("\\s".toRegex(), "").length
        val lineCount = if (content.isEmpty()) 0 else content.lines().size

        val createdStr = if (createdAt > 0) formatDate(createdAt) else "Unknown"
        val modifiedStr = if (!isNewNote) {
            val updatedAt = intent.getLongExtra(EXTRA_NOTE_UPDATED_AT, 0)
            if (updatedAt > 0) formatDate(updatedAt) else "Unknown"
        } else "Not saved yet"

        val message = buildString {
            append("Words: $wordCount\n")
            append("Characters: $charCount\n")
            append("Characters (no spaces): $charCountNoSpaces\n")
            append("Lines: $lineCount\n")
            append("\n")
            append("Created: $createdStr\n")
            append("Modified: $modifiedStr")
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Note Info")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun copyNoteToClipboard() {
        val title = binding.editTitle.text.toString().trim()
        val content = richTextEditor.getPlainText().trim()

        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(this, "Nothing to copy", Toast.LENGTH_SHORT).show()
            return
        }

        val copyText = buildString {
            if (title.isNotEmpty()) {
                append(title)
                if (content.isNotEmpty()) {
                    append("\n\n")
                }
            }
            if (content.isNotEmpty()) {
                append(content)
            }
        }

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Note", copyText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
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
        val plainContent = richTextEditor.getPlainText().trim()
        val htmlContent = richTextEditor.toHtml()
        val tagIds = selectedTags.map { it.id }

        if (title.isEmpty() && plainContent.isEmpty()) {
            if (!isNewNote) {
                viewModel.deleteById(noteId)
            }
            finish()
            return
        }

        // Save as HTML to preserve formatting
        val contentToSave = if (hasFormatting()) htmlContent else plainContent

        if (isNewNote) {
            viewModel.insert(title, contentToSave, tagIds)
        } else {
            val tagsChanged = tagIds.toSet() != originalTagIds.toSet()
            if (title != originalTitle || contentToSave != originalContent || tagsChanged) {
                viewModel.update(
                    Note(
                        id = noteId,
                        title = title,
                        content = contentToSave,
                        updatedAt = System.currentTimeMillis()
                    ),
                    tagIds
                )
            }
        }
        finish()
    }

    private fun hasFormatting(): Boolean {
        val text = binding.editContent.text ?: return false
        return text.getSpans(0, text.length, android.text.style.CharacterStyle::class.java).isNotEmpty()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        saveAndFinish()
    }
}
