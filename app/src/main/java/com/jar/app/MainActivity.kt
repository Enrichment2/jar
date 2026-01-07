package com.jar.app

import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jar.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: NoteViewModel
    private lateinit var adapter: NoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[NoteViewModel::class.java]

        setupRecyclerView()
        setupFab()
        observeNotes()
    }

    private fun setupRecyclerView() {
        adapter = NoteAdapter(
            onNoteClick = { note -> showEditDialog(note) },
            onNoteLongClick = { note -> showDeleteDialog(note) }
        )
        binding.recyclerNotes.layoutManager = LinearLayoutManager(this)
        binding.recyclerNotes.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            showAddDialog()
        }
    }

    private fun observeNotes() {
        viewModel.allNotes.observe(this) { notes ->
            adapter.submitList(notes)
            
            // Show empty state if no notes
            if (notes.isEmpty()) {
                binding.textEmpty.visibility = android.view.View.VISIBLE
                binding.recyclerNotes.visibility = android.view.View.GONE
            } else {
                binding.textEmpty.visibility = android.view.View.GONE
                binding.recyclerNotes.visibility = android.view.View.VISIBLE
            }
        }
    }

    private fun showAddDialog() {
        val editText = EditText(this).apply {
            hint = "What's on your mind?"
            setPadding(48, 32, 48, 32)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("New Note")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val content = editText.text.toString().trim()
                if (content.isNotEmpty()) {
                    viewModel.insert(content)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()

        // Auto-focus and show keyboard
        editText.requestFocus()
    }

    private fun showEditDialog(note: Note) {
        val editText = EditText(this).apply {
            setText(note.content)
            setPadding(48, 32, 48, 32)
            setSelection(note.content.length)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Edit Note")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val content = editText.text.toString().trim()
                if (content.isNotEmpty()) {
                    viewModel.update(note.copy(content = content))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()

        editText.requestFocus()
    }

    private fun showDeleteDialog(note: Note) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Note")
            .setMessage("Are you sure you want to delete this note?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.delete(note)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
