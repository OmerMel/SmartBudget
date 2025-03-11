package com.example.budgetsmart2.presentation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetsmart2.databinding.ItemIconPickerBinding

/**
 * Adapter for displaying icon options in a grid
 */
class IconPickerAdapter(
    private val icons: List<String>,
    private val onIconSelected: (String) -> Unit
) : RecyclerView.Adapter<IconPickerAdapter.IconViewHolder>() {

    // Currently selected icon
    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val binding = ItemIconPickerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return IconViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        holder.bind(icons[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = icons.size

    /**
     * ViewHolder for icon item
     */
    inner class IconViewHolder(
        private val binding: ItemIconPickerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val oldPosition = selectedPosition
                    selectedPosition = position

                    // Update UI
                    notifyItemChanged(oldPosition)
                    notifyItemChanged(selectedPosition)

                    // Notify listener
                    onIconSelected(icons[position])
                }
            }
        }

        /**
         * Bind icon to view
         */
        fun bind(icon: String, isSelected: Boolean) {
            // Set icon
            binding.iconView.text = icon

            // Show/hide selection indicator
            binding.selectedIndicator.visibility = if (isSelected) View.VISIBLE else View.GONE
        }
    }
}