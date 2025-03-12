package com.example.budgetsmart2.presentation.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetsmart2.R
import com.example.budgetsmart2.databinding.ItemTransactionBinding
import com.example.budgetsmart2.domain.dataClasses.TransactionWithCategory
import com.example.budgetsmart2.domain.enums.TransactionType
import com.example.budgetsmart2.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Unified adapter for displaying transactions in a RecyclerView
 * Can be used for both recent transactions and full transaction list
 * Uses ListAdapter with DiffUtil for efficient updates
 */
class TransactionAdapter(
    private val onItemClick: (TransactionWithCategory) -> Unit
) : ListAdapter<TransactionWithCategory, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    // Date formatters for displaying transaction dates
    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    private val todayDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    // Today's date for comparison
    private val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time

    // Yesterday's date for comparison
    private val yesterday = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder for transaction items
     */
    inner class TransactionViewHolder(
        private val binding: ItemTransactionBinding
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
         * Bind transaction data to the view
         */
        fun bind(item: TransactionWithCategory) {
            binding.apply {
                // Set transaction title/description
                transactionTitle1.text = item.transaction.description

                // Format amount based on transaction type
                val amount = item.transaction.amount
                val isExpense = item.transaction.type == TransactionType.EXPENSE

                // Format the amount with currency and sign using CurrencyFormatter utility
                transactionAmount1.text = if (isExpense) {
                    CurrencyFormatter.formatWithSign(itemView.context, -amount)
                } else {
                    CurrencyFormatter.formatWithSign(itemView.context, amount, true)
                }

                // Set text color based on transaction type
                val colorRes = if (isExpense) {
                    R.color.expense
                } else {
                    R.color.income
                }
                transactionAmount1.setTextColor(itemView.context.getColor(colorRes))

                // Set transaction date with relative formatting
                val transactionDate = item.transaction.date
                val transactionDateStart = Calendar.getInstance().apply {
                    time = transactionDate
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time

                // Display date as "Today", "Yesterday", or date format
                val dateText = when {
                    transactionDateStart == today -> {
                        "Today at ${todayDateFormat.format(transactionDate)}"
                    }
                    transactionDateStart == yesterday -> {
                        "Yesterday at ${todayDateFormat.format(transactionDate)}"
                    }
                    else -> {
                        dateFormatter.format(transactionDate)
                    }
                }
                transactionDate1.text = dateText

                // Use the category's color for the background
                val backgroundColorRes = item.category.color
                transactionIconBg1.setCardBackgroundColor(itemView.context.getColor(backgroundColorRes))

                // Set the category icon (emoji)
                transactionIcon.text = item.category.icon
            }
        }
    }

    /**
     * DiffUtil callback for efficient updates
     */
    private class TransactionDiffCallback : DiffUtil.ItemCallback<TransactionWithCategory>() {
        override fun areItemsTheSame(
            oldItem: TransactionWithCategory,
            newItem: TransactionWithCategory
        ): Boolean {
            return oldItem.transaction.id == newItem.transaction.id
        }

        override fun areContentsTheSame(
            oldItem: TransactionWithCategory,
            newItem: TransactionWithCategory
        ): Boolean {
            return oldItem == newItem
        }
    }
}