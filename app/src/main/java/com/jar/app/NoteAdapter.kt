package com.jar.app

import android.content.res.ColorStateList
import android.graphics.Color
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.jar.app.databinding.ItemNoteBinding

class NoteAdapter(
    private val onNoteClick: (NoteWithTags) -> Unit,
    private val onNoteLongClick: (NoteWithTags, Int) -> Boolean = { _, _ -> false }
) : ListAdapter<NoteWithTags, NoteAdapter.NoteViewHolder>(NoteDiffCallback()) {

    private val selectedItems = mutableSetOf<Long>()
    var isSelectionMode = false
        private set

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

    fun toggleSelection(noteId: Long) {
        if (selectedItems.contains(noteId)) {
            selectedItems.remove(noteId)
        } else {
            selectedItems.add(noteId)
        }
        if (selectedItems.isEmpty()) {
            isSelectionMode = false
        }
        notifyDataSetChanged()
    }

    fun enterSelectionMode(noteId: Long) {
        isSelectionMode = true
        selectedItems.clear()
        selectedItems.add(noteId)
        notifyDataSetChanged()
    }

    fun exitSelectionMode() {
        isSelectionMode = false
        selectedItems.clear()
        notifyDataSetChanged()
    }

    fun selectAll() {
        currentList.forEach { selectedItems.add(it.note.id) }
        notifyDataSetChanged()
    }

    fun getSelectedItems(): List<NoteWithTags> {
        return currentList.filter { selectedItems.contains(it.note.id) }
    }

    fun getSelectedCount(): Int = selectedItems.size

    inner class NoteViewHolder(
        private val binding: ItemNoteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position)
                    if (isSelectionMode) {
                        toggleSelection(item.note.id)
                    } else {
                        onNoteClick(item)
                    }
                }
            }
            binding.root.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onNoteLongClick(getItem(position), position)
                } else {
                    false
                }
            }
        }

        fun bind(noteWithTags: NoteWithTags) {
            val note = noteWithTags.note
            val isSelected = selectedItems.contains(note.id)

            // Selection state
            binding.root.isChecked = isSelected

            if (note.title.isNotEmpty()) {
                binding.textTitle.visibility = View.VISIBLE
                binding.textTitle.text = note.title
            } else {
                binding.textTitle.visibility = View.GONE
            }

            if (note.content.isNotEmpty()) {
                binding.textContent.visibility = View.VISIBLE
                // Render HTML content or plain text
                val displayContent = if (note.content.contains("<") && note.content.contains(">")) {
                    Html.fromHtml(note.content, Html.FROM_HTML_MODE_COMPACT)
                } else {
                    note.content
                }
                binding.textContent.text = displayContent
                binding.textContent.maxLines = if (note.title.isEmpty()) 4 else 2
            } else {
                binding.textContent.visibility = View.GONE
            }

            binding.textDate.text = TimeUtils.formatRelativeTime(note.updatedAt)

            binding.chipGroupTags.removeAllViews()
            if (noteWithTags.tags.isNotEmpty()) {
                binding.chipGroupTags.visibility = View.VISIBLE
                val maxTags = 3
                val tagsToShow = noteWithTags.tags.take(maxTags)
                val remainingCount = noteWithTags.tags.size - maxTags

                tagsToShow.forEach { tag ->
                    val chip = Chip(binding.root.context).apply {
                        text = tag.name
                        isClickable = false
                        isCheckable = false
                        textSize = 10f
                        chipMinHeight = 24f
                        chipStartPadding = 8f
                        chipEndPadding = 8f

                        if (tag.color != 0) {
                            chipBackgroundColor = ColorStateList.valueOf(tag.color)
                            setTextColor(TagColors.getContrastColor(tag.color))
                        }
                    }
                    binding.chipGroupTags.addView(chip)
                }

                if (remainingCount > 0) {
                    val moreChip = Chip(binding.root.context).apply {
                        text = "+$remainingCount"
                        isClickable = false
                        isCheckable = false
                        textSize = 10f
                        chipMinHeight = 24f
                        chipStartPadding = 8f
                        chipEndPadding = 8f
                    }
                    binding.chipGroupTags.addView(moreChip)
                }
            } else {
                binding.chipGroupTags.visibility = View.GONE
            }
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
