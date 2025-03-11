package com.example.budgetsmart2.presentation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetsmart2.databinding.ItemColorPickerBinding

/**
 * Adapter for displaying color options in a grid
 */
class ColorPickerAdapter(
    private val colors: List<Int>,
    private val onColorSelected: (Int) -> Unit
) : RecyclerView.Adapter<ColorPickerAdapter.ColorViewHolder>() {

    // Currently selected color
    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val binding = ItemColorPickerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ColorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        holder.bind(colors[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = colors.size

    /**
     * ViewHolder for color item
     */
    inner class ColorViewHolder(
        private val binding: ItemColorPickerBinding
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
                    onColorSelected(colors[position])
                }
            }
        }

        /**
         * Bind color to view
         */
        fun bind(colorRes: Int, isSelected: Boolean) {
            val context = binding.root.context

            // Set color
            binding.colorView.backgroundTintList =
                ContextCompat.getColorStateList(context, colorRes)

            // Show/hide selection indicator
            binding.selectedIndicator.visibility = if (isSelected) View.VISIBLE else View.GONE
        }
    }
}