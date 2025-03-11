package com.example.budgetsmart2.presentation.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.budgetsmart2.R
import com.example.budgetsmart2.databinding.DialogAddEditCategoryBinding
import com.example.budgetsmart2.domain.dataClasses.Category
import com.example.budgetsmart2.presentation.adapters.ColorPickerAdapter
import com.example.budgetsmart2.presentation.adapters.IconPickerAdapter

/**
 * Dialog for adding or editing a category
 */
class AddEditCategoryDialog : DialogFragment() {

    // View binding
    private var _binding: DialogAddEditCategoryBinding? = null
    private val binding get() = _binding!!

    // Data
    private var editingCategory: Category? = null
    private var selectedColor: Int = R.color.primary
    private var selectedIcon: String = "📊"

    // Callback for when a category is submitted
    private var onCategorySubmitted: ((name: String, icon: String, color: Int) -> Unit)? = null

    companion object {
        private const val ARG_CATEGORY = "category"

        /**
         * Create a new instance for adding a category
         */
        fun newInstance(
            onSubmit: (name: String, icon: String, color: Int) -> Unit
        ): AddEditCategoryDialog {
            return AddEditCategoryDialog().apply {
                onCategorySubmitted = onSubmit
            }
        }

        /**
         * Create a new instance for editing a category
         */
        fun newInstance(
            category: Category,
            onSubmit: (name: String, icon: String, color: Int) -> Unit
        ): AddEditCategoryDialog {
            val args = Bundle().apply {
                putParcelable(ARG_CATEGORY, category)
            }

            return AddEditCategoryDialog().apply {
                arguments = args
                onCategorySubmitted = onSubmit
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Dialog_FullWidth)

        // Get category if editing
        editingCategory = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(ARG_CATEGORY, Category::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable(ARG_CATEGORY)
        }

        // Set initial values if editing
        editingCategory?.let {
            selectedColor = it.color
            selectedIcon = it.icon
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddEditCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up dialog
        setupDialog()
        setupColorPicker()
        setupIconPicker()
        setupButtons()
    }

    /**
     * Set up dialog title and initial values
     */
    private fun setupDialog() {
        binding.apply {
            // Set dialog title
            dialogTitle.text = if (editingCategory != null) "Edit Category" else "Add Category"

            // Set initial values if editing
            editingCategory?.let {
                categoryNameInput.setText(it.name)
                updateSelectedColor(it.color)
                updateSelectedIcon(it.icon)
            }
        }
    }

    /**
     * Set up the color picker grid
     */
    private fun setupColorPicker() {
        // Available colors
        val colors = listOf(
            R.color.primary,
            R.color.expense,
            R.color.income,
            R.color.food,
            R.color.primary_dark,
            R.color.expense_light,
            R.color.income_light,
            R.color.food_light
        )

        // Create adapter
        val colorAdapter = ColorPickerAdapter(colors) { color ->
            updateSelectedColor(color)
        }

        // Set up RecyclerView
        binding.colorRecyclerView.apply {
            adapter = colorAdapter
            layoutManager = GridLayoutManager(requireContext(), 4)
        }
    }

    /**
     * Update the selected color UI
     */
    private fun updateSelectedColor(colorRes: Int) {
        selectedColor = colorRes
        binding.selectedColorView.backgroundTintList =
            ContextCompat.getColorStateList(requireContext(), colorRes)
    }

    /**
     * Set up the icon picker grid
     */
    private fun setupIconPicker() {
        // Common category icons
        val icons = listOf(
            "🍔", "🏠", "🚗", "👕", "💻", "📱", "🎮", "🎬",
            "✈️", "🛒", "💼", "💉", "📚", "🏋️", "🍷", "👶",
            "💰", "💳", "🏦", "📊", "🧾", "🎁", "⚡", "🚿"
        )

        // Create adapter
        val iconAdapter = IconPickerAdapter(icons) { icon ->
            updateSelectedIcon(icon)
        }

        // Set up RecyclerView
        binding.iconRecyclerView.apply {
            adapter = iconAdapter
            layoutManager = GridLayoutManager(requireContext(), 6)
        }
    }

    /**
     * Update the selected icon UI
     */
    private fun updateSelectedIcon(icon: String) {
        selectedIcon = icon
        binding.selectedIconView.text = icon
    }

    /**
     * Set up the save and cancel buttons
     */
    private fun setupButtons() {
        binding.apply {
            // Save button
            saveButton.setOnClickListener {
                saveCategory()
            }

            // Cancel button
            cancelButton.setOnClickListener {
                dismiss()
            }
        }
    }

    /**
     * Save the category if input is valid
     */
    private fun saveCategory() {
        // Get category name
        val name = binding.categoryNameInput.text.toString().trim()

        // Validate input
        if (name.isEmpty()) {
            binding.categoryNameInputLayout.error = "Please enter a category name"
            return
        } else {
            binding.categoryNameInputLayout.error = null
        }

        // Call the callback with the category data
        onCategorySubmitted?.invoke(name, selectedIcon, selectedColor)

        // Dismiss the dialog
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}