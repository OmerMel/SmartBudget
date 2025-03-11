package com.example.budgetsmart2.presentation.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetsmart2.databinding.ItemCategoryBinding
import com.example.budgetsmart2.domain.dataClasses.Category

/**
 * Adapter for displaying categories in a RecyclerView
 * Uses ListAdapter with DiffUtil for efficient updates
 */
class CategoryListAdapter(
    private val onEditClick: (Category) -> Unit,
    private val onDeleteClick: (Category) -> Unit
) : ListAdapter<Category, CategoryListAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder for category items
     */
    inner class CategoryViewHolder(
        private val binding: ItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Bind category data to the view
         */
        fun bind(item: Category) {
            binding.apply {
                // Set category name
                categoryName.text = item.name

                // Set category icon
                categoryIcon.text = item.icon

                // Set category color
                categoryIconBg.setCardBackgroundColor(
                    ContextCompat.getColor(itemView.context, item.color.toInt())
                )

                // Set click listeners for action buttons
                editButton.setOnClickListener {
                    onEditClick(item)
                }

                deleteButton.setOnClickListener {
                    onDeleteClick(item)
                }
            }
        }
    }

    /**
     * DiffUtil callback for efficient updates
     */
    private class CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem
        }
    }
}