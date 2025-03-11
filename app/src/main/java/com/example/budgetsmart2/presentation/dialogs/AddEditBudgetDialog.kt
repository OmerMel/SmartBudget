package com.example.budgetsmart2.presentation.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.budgetsmart2.R
import com.example.budgetsmart2.databinding.DialogAddEditBudgetBinding
import com.example.budgetsmart2.domain.dataClasses.Category
import com.example.budgetsmart2.presentation.adapters.CategoryAdapter
import java.text.NumberFormat
import java.util.*

/**
 * Dialog for adding or editing a budget
 */
class AddEditBudgetDialog : DialogFragment() {

    // View binding
    private var _binding: DialogAddEditBudgetBinding? = null
    private val binding get() = _binding!!

    // Data
    private lateinit var categories: List<Category>
    private var selectedCategoryId: String = ""
    private var initialAmount: Double = 0.0
    private var isEdit: Boolean = false

    // Callback
    private var onBudgetSubmitted: ((categoryId: String, amount: Double) -> Unit)? = null

    // Currency formatter
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

    companion object {
        private const val ARG_CATEGORIES = "categories"
        private const val ARG_CATEGORY_ID = "category_id"
        private const val ARG_AMOUNT = "amount"

        /**
         * Create a new instance for adding a budget
         */
        fun newInstance(
            categories: List<Category>,
            onSubmit: (categoryId: String, amount: Double) -> Unit
        ): AddEditBudgetDialog {
            val args = Bundle().apply {
                putParcelableArrayList(ARG_CATEGORIES, ArrayList(categories))
            }

            return AddEditBudgetDialog().apply {
                arguments = args
                onBudgetSubmitted = onSubmit
            }
        }

        /**
         * Create a new instance for editing a budget
         */
        fun newInstance(
            categories: List<Category>,
            categoryId: String,
            amount: Double,
            onSubmit: (categoryId: String, amount: Double) -> Unit
        ): AddEditBudgetDialog {
            val args = Bundle().apply {
                putParcelableArrayList(ARG_CATEGORIES, ArrayList(categories))
                putString(ARG_CATEGORY_ID, categoryId)
                putDouble(ARG_AMOUNT, amount)
            }

            return AddEditBudgetDialog().apply {
                arguments = args
                onBudgetSubmitted = onSubmit
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Dialog_FullWidth)

        // Get arguments
        categories = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelableArrayList(ARG_CATEGORIES, Category::class.java) ?: emptyList()
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelableArrayList(ARG_CATEGORIES) ?: emptyList()
        }

        selectedCategoryId = arguments?.getString(ARG_CATEGORY_ID, "") ?: ""
        initialAmount = arguments?.getDouble(ARG_AMOUNT, 0.0) ?: 0.0
        isEdit = selectedCategoryId.isNotEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddEditBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up dialog
        setupDialog()
        setupCategorySpinner()
        setupButtons()
    }

    /**
     * Set up dialog title and initial values
     */
    private fun setupDialog() {
        binding.apply {
            // Set dialog title
            dialogTitle.text = if (isEdit) "Edit Budget" else "Add Budget"

            // Set initial amount if editing
            if (isEdit) {
                amountInput.setText(initialAmount.toString())
            }
        }
    }

    /**
     * Set up the category spinner
     */
    private fun setupCategorySpinner() {
        if (categories.isEmpty()) {
            binding.categorySpinner.isEnabled = false
            return
        }

        // Create adapter for spinner
        val adapter = CategoryAdapter(requireContext(), categories)

        binding.categorySpinner.apply {
            this.adapter = adapter
            isEnabled = true

            // Set initial selection if editing
            if (isEdit) {
                val position = categories.indexOfFirst { it.id == selectedCategoryId }
                if (position >= 0) {
                    setSelection(position)
                }
            }

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    selectedCategoryId = categories[position].id
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    selectedCategoryId = ""
                }
            }
        }
    }

    /**
     * Set up the save and cancel buttons
     */
    private fun setupButtons() {
        binding.apply {
            // Save button
            saveButton.setOnClickListener {
                saveBudget()
            }

            // Cancel button
            cancelButton.setOnClickListener {
                dismiss()
            }
        }
    }

    /**
     * Save the budget if input is valid
     */
    private fun saveBudget() {
        // Validate input
        val amount = binding.amountInput.text.toString().toDoubleOrNull()

        if (amount == null || amount <= 0) {
            binding.amountInputLayout.error = "Please enter a valid amount"
            return
        } else {
            binding.amountInputLayout.error = null
        }

        if (selectedCategoryId.isEmpty()) {
            Toast.makeText(context, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }

        // Call the callback with the budget data
        onBudgetSubmitted?.invoke(selectedCategoryId, amount)

        // Dismiss the dialog
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}