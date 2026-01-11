package com.jar.app

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.jar.app.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: NoteViewModel
    private lateinit var adapter: NoteAdapter

    private var isSearchVisible = false
    private var isFilterVisible = false
    private var pendingExportJson: String? = null
    private var actionMode: ActionMode? = null
    private var recentlyDeletedNote: NoteWithTags? = null

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { saveExportToFile(it) }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { importFromFile(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[NoteViewModel::class.java]

        setupToolbar()
        setupRecyclerView()
        setupSwipeToDelete()
        setupSearch()
        setupFab()
        observeNotes()
        observeTags()
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_search -> {
                    toggleSearch()
                    true
                }
                R.id.action_filter -> {
                    toggleFilter()
                    true
                }
                R.id.action_sort -> {
                    showSortDialog()
                    true
                }
                R.id.action_export -> {
                    exportNotes()
                    true
                }
                R.id.action_import -> {
                    importNotes()
                    true
                }
                R.id.action_manage_tags -> {
                    startActivity(Intent(this, TagManagementActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun showSortDialog() {
        val options = arrayOf("Date modified", "Date created", "Alphabetical")
        val currentSort = viewModel.sortOrder.value ?: SortOrder.DATE_MODIFIED

        MaterialAlertDialogBuilder(this)
            .setTitle("Sort by")
            .setSingleChoiceItems(options, currentSort.ordinal) { dialog, which ->
                viewModel.setSortOrder(SortOrder.entries[which])
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleSearch() {
        isSearchVisible = !isSearchVisible
        binding.searchLayout.visibility = if (isSearchVisible) View.VISIBLE else View.GONE
        if (!isSearchVisible) {
            binding.editSearch.setText("")
            viewModel.setSearchQuery("")
        } else {
            binding.editSearch.requestFocus()
        }
    }

    private fun toggleFilter() {
        isFilterVisible = !isFilterVisible
        binding.tagFilterScroll.visibility = if (isFilterVisible) View.VISIBLE else View.GONE
        if (!isFilterVisible) {
            viewModel.setSelectedTag(null)
            binding.chipGroupTags.clearCheck()
        }
    }

    private fun setupSearch() {
        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
        })
    }

    private fun setupRecyclerView() {
        adapter = NoteAdapter(
            onNoteClick = { noteWithTags ->
                if (adapter.isSelectionMode) {
                    updateActionModeTitle()
                    if (!adapter.isSelectionMode) {
                        actionMode?.finish()
                    }
                } else {
                    openEditor(noteWithTags)
                }
            },
            onNoteLongClick = { noteWithTags, _ ->
                if (!adapter.isSelectionMode) {
                    adapter.enterSelectionMode(noteWithTags.note.id)
                    startActionMode()
                }
                true
            }
        )
        binding.recyclerNotes.layoutManager = LinearLayoutManager(this)
        binding.recyclerNotes.adapter = adapter
    }

    private fun setupSwipeToDelete() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            private val deleteIcon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_delete)
            private val paint = Paint().apply {
                color = ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_light)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val noteWithTags = adapter.currentList[position]
                deleteNoteWithUndo(noteWithTags)
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val iconMargin = (itemView.height - (deleteIcon?.intrinsicHeight ?: 0)) / 2

                if (dX > 0) {
                    // Swiping to the right
                    c.drawRoundRect(
                        RectF(itemView.left.toFloat(), itemView.top.toFloat(), dX, itemView.bottom.toFloat()),
                        16f, 16f, paint
                    )
                    deleteIcon?.let {
                        it.setBounds(
                            itemView.left + iconMargin,
                            itemView.top + iconMargin,
                            itemView.left + iconMargin + it.intrinsicWidth,
                            itemView.bottom - iconMargin
                        )
                        it.setTint(ContextCompat.getColor(this@MainActivity, android.R.color.white))
                        it.draw(c)
                    }
                } else if (dX < 0) {
                    // Swiping to the left
                    c.drawRoundRect(
                        RectF(itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat()),
                        16f, 16f, paint
                    )
                    deleteIcon?.let {
                        it.setBounds(
                            itemView.right - iconMargin - it.intrinsicWidth,
                            itemView.top + iconMargin,
                            itemView.right - iconMargin,
                            itemView.bottom - iconMargin
                        )
                        it.setTint(ContextCompat.getColor(this@MainActivity, android.R.color.white))
                        it.draw(c)
                    }
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }

            override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                return if (adapter.isSelectionMode) 0 else super.getSwipeDirs(recyclerView, viewHolder)
            }
        }

        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerNotes)
    }

    private fun deleteNoteWithUndo(noteWithTags: NoteWithTags) {
        recentlyDeletedNote = noteWithTags
        viewModel.deleteById(noteWithTags.note.id)

        Snackbar.make(binding.root, "Note deleted", Snackbar.LENGTH_LONG)
            .setAction("Undo") {
                recentlyDeletedNote?.let { deleted ->
                    viewModel.insert(
                        deleted.note.title,
                        deleted.note.content,
                        deleted.tags.map { it.id }
                    )
                }
                recentlyDeletedNote = null
            }
            .addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    if (event != DISMISS_EVENT_ACTION) {
                        recentlyDeletedNote = null
                    }
                }
            })
            .show()
    }

    private fun startActionMode() {
        actionMode = startActionMode(object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                menuInflater.inflate(R.menu.menu_selection, menu)
                binding.fabAdd.hide()
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                return when (item.itemId) {
                    R.id.action_delete -> {
                        deleteSelectedNotes()
                        true
                    }
                    R.id.action_select_all -> {
                        adapter.selectAll()
                        updateActionModeTitle()
                        true
                    }
                    else -> false
                }
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                adapter.exitSelectionMode()
                actionMode = null
                binding.fabAdd.show()
            }
        })
        updateActionModeTitle()
    }

    private fun updateActionModeTitle() {
        actionMode?.title = "${adapter.getSelectedCount()} selected"
    }

    private fun deleteSelectedNotes() {
        val selectedNotes = adapter.getSelectedItems()
        if (selectedNotes.isEmpty()) return

        MaterialAlertDialogBuilder(this)
            .setTitle("Delete ${selectedNotes.size} note${if (selectedNotes.size > 1) "s" else ""}?")
            .setMessage("This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteMultiple(selectedNotes.map { it.note.id })
                actionMode?.finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            openEditor(null)
        }
    }

    private fun observeNotes() {
        viewModel.filteredNotes.observe(this) { notesWithTags ->
            adapter.submitList(notesWithTags)

            val isEmpty = notesWithTags.isEmpty()
            val hasSearchOrFilter = viewModel.searchQuery.value?.isNotEmpty() == true ||
                viewModel.selectedTagId.value != null

            if (isEmpty) {
                binding.textEmpty.visibility = View.VISIBLE
                binding.recyclerNotes.visibility = View.GONE
                binding.textEmpty.text = if (hasSearchOrFilter) {
                    "No results found."
                } else {
                    "Your jar is empty.\nTap + to add a note."
                }
            } else {
                binding.textEmpty.visibility = View.GONE
                binding.recyclerNotes.visibility = View.VISIBLE
            }
        }
    }

    private fun observeTags() {
        viewModel.allTags.observe(this) { tags ->
            binding.chipGroupTags.removeAllViews()

            tags.forEach { tag ->
                val chip = Chip(this).apply {
                    text = tag.name
                    isCheckable = true
                    setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            viewModel.setSelectedTag(tag.id)
                        } else if (viewModel.selectedTagId.value == tag.id) {
                            viewModel.setSelectedTag(null)
                        }
                    }
                }
                binding.chipGroupTags.addView(chip)
            }
        }
    }

    private fun openEditor(noteWithTags: NoteWithTags?) {
        val intent = Intent(this, NoteEditorActivity::class.java)
        noteWithTags?.let {
            intent.putExtra(NoteEditorActivity.EXTRA_NOTE_ID, it.note.id)
            intent.putExtra(NoteEditorActivity.EXTRA_NOTE_TITLE, it.note.title)
            intent.putExtra(NoteEditorActivity.EXTRA_NOTE_CONTENT, it.note.content)
            intent.putExtra(NoteEditorActivity.EXTRA_NOTE_UPDATED_AT, it.note.updatedAt)
            intent.putExtra(NoteEditorActivity.EXTRA_NOTE_CREATED_AT, it.note.createdAt)
        }
        startActivity(intent)
    }

    private fun exportNotes() {
        lifecycleScope.launch {
            try {
                val json = viewModel.exportToJson()
                pendingExportJson = json
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val fileName = "jar-notes-${dateFormat.format(Date())}.json"
                exportLauncher.launch(fileName)
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveExportToFile(uri: Uri) {
        val json = pendingExportJson ?: return
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toByteArray())
            }
            Toast.makeText(this, "Notes exported successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to save file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        pendingExportJson = null
    }

    private fun importNotes() {
        importLauncher.launch(arrayOf("application/json"))
    }

    private fun importFromFile(uri: Uri) {
        lifecycleScope.launch {
            try {
                val json = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (json.isNullOrEmpty()) {
                    Toast.makeText(this@MainActivity, "File is empty", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                when (val result = viewModel.importFromJson(json)) {
                    is ImportResult.Success -> {
                        Toast.makeText(
                            this@MainActivity,
                            "Imported ${result.notesImported} notes and ${result.tagsImported} new tags",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    is ImportResult.Error -> {
                        Toast.makeText(
                            this@MainActivity,
                            "Import failed: ${result.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBackPressed() {
        if (adapter.isSelectionMode) {
            actionMode?.finish()
        } else {
            super.onBackPressed()
        }
    }
}
