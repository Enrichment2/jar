package com.jar.app

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jar.app.databinding.ActivityTagManagementBinding
import com.jar.app.databinding.DialogEditTagBinding
import kotlinx.coroutines.launch

class TagManagementActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTagManagementBinding
    private lateinit var viewModel: NoteViewModel
    private lateinit var adapter: TagAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTagManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[NoteViewModel::class.java]

        setupToolbar()
        setupRecyclerView()
        observeTags()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = TagAdapter(
            onEditClick = { tag -> showEditDialog(tag) },
            onDeleteClick = { tag -> showDeleteConfirmation(tag) }
        )
        binding.recyclerTags.layoutManager = LinearLayoutManager(this)
        binding.recyclerTags.adapter = adapter
    }

    private fun observeTags() {
        viewModel.allTags.observe(this) { tags ->
            if (tags.isEmpty()) {
                binding.textEmpty.visibility = View.VISIBLE
                binding.recyclerTags.visibility = View.GONE
            } else {
                binding.textEmpty.visibility = View.GONE
                binding.recyclerTags.visibility = View.VISIBLE

                lifecycleScope.launch {
                    val tagsWithCount = tags.map { tag ->
                        TagWithCount(tag, viewModel.getTagNoteCount(tag.id))
                    }
                    adapter.submitList(tagsWithCount)
                }
            }
        }
    }

    private fun showEditDialog(tag: Tag) {
        val dialogBinding = DialogEditTagBinding.inflate(LayoutInflater.from(this))
        dialogBinding.editName.setText(tag.name)

        var selectedColor = tag.color

        setupColorPicker(dialogBinding.colorContainer, selectedColor) { color ->
            selectedColor = color
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Edit Tag")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val newName = dialogBinding.editName.text.toString().trim()
                if (newName.isNotEmpty()) {
                    viewModel.updateTag(tag.copy(name = newName, color = selectedColor))
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

    private fun showDeleteConfirmation(tag: Tag) {
        lifecycleScope.launch {
            val noteCount = viewModel.getTagNoteCount(tag.id)

            MaterialAlertDialogBuilder(this@TagManagementActivity)
                .setTitle("Delete Tag")
                .setMessage(
                    if (noteCount > 0) {
                        "This tag is used in $noteCount note${if (noteCount > 1) "s" else ""}. " +
                                "Deleting it will remove it from those notes."
                    } else {
                        "Are you sure you want to delete this tag?"
                    }
                )
                .setPositiveButton("Delete") { _, _ ->
                    viewModel.deleteTag(tag)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
