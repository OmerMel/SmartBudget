package com.example.budgetsmart2.presentation.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.example.budgetsmart2.R
import com.example.budgetsmart2.domain.dataClasses.Category

/**
 * Custom adapter for displaying categories with icons in a spinner
 */
class CategoryAdapter(
    context: Context,
    private val categories: List<Category>
) : ArrayAdapter<Category>(context, 0, categories) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createItemView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createItemView(position, convertView, parent, true)
    }

    private fun createItemView(position: Int, convertView: View?, parent: ViewGroup, isDropDown: Boolean = false): View {
        val layoutId = if (isDropDown) {
            R.layout.item_category_dropdown
        } else {
            R.layout.item_category_selected
        }

        val view = convertView ?: LayoutInflater.from(context).inflate(layoutId, parent, false)

        val category = categories[position]

        // Set category name
        val categoryName = view.findViewById<TextView>(R.id.category_name)
        categoryName.text = category.name

        // Set category icon background color
        val iconBg = view.findViewById<CardView>(R.id.category_icon_bg)
        iconBg.setCardBackgroundColor(ContextCompat.getColor(context, category.color.toInt()))

        // Set category icon (emoji or text)
        val categoryIcon = view.findViewById<TextView>(R.id.category_icon)
        categoryIcon.text = category.icon

        return view
    }
}