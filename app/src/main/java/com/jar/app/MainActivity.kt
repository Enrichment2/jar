package com.jar.app

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.jar.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: NoteViewModel
    private lateinit var adapter: NoteAdapter

    private var isSearchVisible = false
    private var isFilterVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[NoteViewModel::class.java]

        setupToolbar()
        setupRecyclerView()
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
                else -> false
            }
        }
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
            onNoteClick = { noteWithTags -> openEditor(noteWithTags) }
        )
        binding.recyclerNotes.layoutManager = LinearLayoutManager(this)
        binding.recyclerNotes.adapter = adapter
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
        }
        startActivity(intent)
    }
}
