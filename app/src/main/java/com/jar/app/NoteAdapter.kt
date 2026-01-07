package com.jar.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.jar.app.databinding.ItemNoteBinding
import java.text.SimpleDateFormat
import java.util.*

class NoteAdapter(
    private val onNoteClick: (NoteWithTags) -> Unit
) : ListAdapter<NoteWithTags, NoteAdapter.NoteViewHolder>(NoteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NoteViewHolder(
        private val binding: ItemNoteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onNoteClick(getItem(position))
                }
            }
        }

        fun bind(noteWithTags: NoteWithTags) {
            val note = noteWithTags.note

            if (note.title.isNotEmpty()) {
                binding.textTitle.visibility = View.VISIBLE
                binding.textTitle.text = note.title
            } else {
                binding.textTitle.visibility = View.GONE
            }

            if (note.content.isNotEmpty()) {
                binding.textContent.visibility = View.VISIBLE
                binding.textContent.text = note.content
            } else {
                binding.textContent.visibility = View.GONE
            }

            binding.textDate.text = formatDate(note.updatedAt)

            binding.chipGroupTags.removeAllViews()
            if (noteWithTags.tags.isNotEmpty()) {
                binding.chipGroupTags.visibility = View.VISIBLE
                noteWithTags.tags.forEach { tag ->
                    val chip = Chip(binding.root.context).apply {
                        text = tag.name
                        isClickable = false
                        isCheckable = false
                        textSize = 10f
                        chipMinHeight = 24f
                        chipStartPadding = 8f
                        chipEndPadding = 8f
                    }
                    binding.chipGroupTags.addView(chip)
                }
            } else {
                binding.chipGroupTags.visibility = View.GONE
            }
        }

        private fun formatDate(timestamp: Long): String {
            val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    class NoteDiffCallback : DiffUtil.ItemCallback<NoteWithTags>() {
        override fun areItemsTheSame(oldItem: NoteWithTags, newItem: NoteWithTags): Boolean {
            return oldItem.note.id == newItem.note.id
        }

        override fun areContentsTheSame(oldItem: NoteWithTags, newItem: NoteWithTags): Boolean {
            return oldItem.note == newItem.note && oldItem.tags == newItem.tags
        }
    }
}
