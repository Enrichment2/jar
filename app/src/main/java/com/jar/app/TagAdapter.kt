package com.jar.app

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jar.app.databinding.ItemTagBinding

data class TagWithCount(
    val tag: Tag,
    val noteCount: Int
)

class TagAdapter(
    private val onEditClick: (Tag) -> Unit,
    private val onDeleteClick: (Tag) -> Unit
) : ListAdapter<TagWithCount, TagAdapter.TagViewHolder>(TagDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val binding = ItemTagBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TagViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TagViewHolder(
        private val binding: ItemTagBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(tagWithCount: TagWithCount) {
            val tag = tagWithCount.tag

            binding.textName.text = tag.name
            binding.textCount.text = "${tagWithCount.noteCount} note${if (tagWithCount.noteCount != 1) "s" else ""}"

            if (tag.color != 0) {
                binding.viewColor.backgroundTintList = ColorStateList.valueOf(tag.color)
            } else {
                binding.viewColor.backgroundTintList = null
            }

            binding.btnEdit.setOnClickListener {
                onEditClick(tag)
            }

            binding.root.setOnLongClickListener {
                onDeleteClick(tag)
                true
            }
        }
    }

    class TagDiffCallback : DiffUtil.ItemCallback<TagWithCount>() {
        override fun areItemsTheSame(oldItem: TagWithCount, newItem: TagWithCount): Boolean {
            return oldItem.tag.id == newItem.tag.id
        }

        override fun areContentsTheSame(oldItem: TagWithCount, newItem: TagWithCount): Boolean {
            return oldItem == newItem
        }
    }
}
