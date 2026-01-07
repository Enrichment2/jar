package com.jar.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
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
            onNoteClick = { note -> openEditor(note) }
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
        viewModel.allNotes.observe(this) { notes ->
            adapter.submitList(notes)

            if (notes.isEmpty()) {
                binding.textEmpty.visibility = android.view.View.VISIBLE
                binding.recyclerNotes.visibility = android.view.View.GONE
            } else {
                binding.textEmpty.visibility = android.view.View.GONE
                binding.recyclerNotes.visibility = android.view.View.VISIBLE
            }
        }
    }

    private fun openEditor(note: Note?) {
        val intent = Intent(this, NoteEditorActivity::class.java)
        note?.let {
            intent.putExtra(NoteEditorActivity.EXTRA_NOTE_ID, it.id)
            intent.putExtra(NoteEditorActivity.EXTRA_NOTE_TITLE, it.title)
            intent.putExtra(NoteEditorActivity.EXTRA_NOTE_CONTENT, it.content)
            intent.putExtra(NoteEditorActivity.EXTRA_NOTE_UPDATED_AT, it.updatedAt)
        }
        startActivity(intent)
    }
}
