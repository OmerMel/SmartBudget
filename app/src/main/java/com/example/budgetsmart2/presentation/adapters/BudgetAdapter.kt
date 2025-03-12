package com.example.budgetsmart2.presentation.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetsmart2.R
import com.example.budgetsmart2.databinding.ItemBudgetCategoryBinding
import com.example.budgetsmart2.domain.dataClasses.BudgetStatus
import com.example.budgetsmart2.utils.CurrencyFormatter
import java.text.NumberFormat
import java.util.*

/**
 * Adapter for displaying budget categories and their status in a RecyclerView
 * Uses ListAdapter with DiffUtil for efficient updates
 */
class BudgetAdapter(
    private val onItemClick: (BudgetStatus) -> Unit
) : ListAdapter<BudgetStatus, BudgetAdapter.BudgetViewHolder>(BudgetDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        val binding = ItemBudgetCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BudgetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder for budget category items
     */
    inner class BudgetViewHolder(
        private val binding: ItemBudgetCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        /**
         * Bind budget data to the view
         */
        fun bind(item: BudgetStatus) {
            binding.apply {
                // Set category name
                foodTitle.text = item.category.name

                // Set budget progress
                val percentage = item.percentage.toInt()
                foodProgress.progress = percentage
                foodPercentage.text = "$percentage%"

                // Set progress color based on percentage
                val colorRes = when {
                    percentage >= 100 -> R.color.expense
                    percentage >= 80 -> R.color.food
                    else -> R.color.primary
                }
                foodPercentage.setTextColor(itemView.context.getColor(colorRes))

                // Update progress drawable
                foodProgress.progressDrawable = ContextCompat.getDrawable(
                    itemView.context,
                    R.drawable.budget_progress_drawable
                )

                // Set budget amount
                val spent = CurrencyFormatter.format(itemView.context, item.spent)
                val budget = CurrencyFormatter.format(itemView.context, item.budget.amount)
                foodBudget.text = "$budget/$spent"

                // Set category icon
                categoryIcon.text = item.category.icon
                foodIconBg.setCardBackgroundColor(
                    ContextCompat.getColor(itemView.context, item.category.color.toInt())
                )
            }
        }
    }

    /**
     * DiffUtil callback for efficient updates
     */
    private class BudgetDiffCallback : DiffUtil.ItemCallback<BudgetStatus>() {
        override fun areItemsTheSame(oldItem: BudgetStatus, newItem: BudgetStatus): Boolean {
            return oldItem.budget.id == newItem.budget.id
        }

        override fun areContentsTheSame(oldItem: BudgetStatus, newItem: BudgetStatus): Boolean {
            return oldItem == newItem
        }
    }
}