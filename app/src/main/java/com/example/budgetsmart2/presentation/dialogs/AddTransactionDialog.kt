package com.example.budgetsmart2.presentation.dialogs

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.budgetsmart2.R
import com.example.budgetsmart2.databinding.DialogAddTransactionBinding
import com.example.budgetsmart2.domain.dataClasses.Category
import com.example.budgetsmart2.domain.enums.TransactionType
import com.example.budgetsmart2.presentation.adapters.CategoryAdapter
import com.example.budgetsmart2.presentation.viewModels.CategoryViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog for adding or editing transactions
 * Allows user to enter amount, description, category, and date
 */
@AndroidEntryPoint
class AddTransactionDialog : DialogFragment() {

    // View binding
    private var _binding: DialogAddTransactionBinding? = null
    private val binding get() = _binding!!

    // Category ViewModel for loading categories
    private val categoryViewModel: CategoryViewModel by viewModels()

    // Selected values
    private var selectedCategoryId: String = ""
    private var selectedDate: Date = Date()
    private lateinit var transactionType: TransactionType

    // Callback for when a transaction is submitted
    private var onTransactionSubmitted: ((amount: Double, description: String, categoryId: String, date: Date) -> Unit)? = null

    // Date formatter
    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    companion object {
        private const val ARG_TRANSACTION_TYPE = "transaction_type"

        /**
         * Creates a new instance of the dialog
         * @param type The transaction type (INCOME or EXPENSE)
         * @param onSubmit Callback function that receives the transaction data
         */
        fun newInstance(
            type: TransactionType,
            onSubmit: (amount: Double, description: String, categoryId: String, date: Date) -> Unit
        ): AddTransactionDialog {
            val dialog = AddTransactionDialog().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_TRANSACTION_TYPE, type)
                }
                onTransactionSubmitted = onSubmit
            }
            return dialog
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Dialog_FullWidth)

        // Get transaction type from arguments
        // Get transaction type from arguments using the non-deprecated method for API 33+
        transactionType = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable(ARG_TRANSACTION_TYPE, TransactionType::class.java) ?: TransactionType.EXPENSE
        } else {
            @Suppress("DEPRECATION")
            arguments?.getSerializable(ARG_TRANSACTION_TYPE) as? TransactionType ?: TransactionType.EXPENSE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the dialog with the current user ID
        setupDialog()
        setupCategoriesSpinner()
        setupDatePicker()
        setupButtons()

        // Initialize ViewModel with current user ID
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            categoryViewModel.initialize(currentUser.uid)
        }

        // Observe ViewModel
        observeViewModel()
    }

    /**
     * Set up the dialog title and styling based on transaction type
     */
    private fun setupDialog() {
        // Set dialog title and color based on transaction type
        val colorRes = if (transactionType == TransactionType.EXPENSE) {
            R.color.expense
        } else {
            R.color.income
        }

        val titleText = if (transactionType == TransactionType.EXPENSE) {
            "Add Expense"
        } else {
            "Add Income"
        }

        binding.apply {
            dialogTitle.text = titleText
            dialogTitle.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
            amountInputLayout.setStartIconTintList(ContextCompat.getColorStateList(requireContext(), colorRes))
        }
    }

    /**
     * Set up the categories spinner
     */
    private fun setupCategoriesSpinner() {
        // Initially disable until categories are loaded
        binding.categorySpinner.isEnabled = false
    }

    /**
     * Set up the date picker
     */
    private fun setupDatePicker() {
        // Set initial date
        updateDateDisplay()

        // Show date picker when field is clicked
        binding.datePickerField.setOnClickListener {
            showDatePicker()
        }
    }

    /**
     * Update the date display field
     */
    private fun updateDateDisplay() {
        binding.datePickerField.setText(dateFormatter.format(selectedDate))
    }

    /**
     * Show the date picker dialog
     */
    private fun showDatePicker() {
        val calendar = Calendar.getInstance().apply {
            time = selectedDate
        }

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedDate = calendar.time
                updateDateDisplay()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    /**
     * Set up the save and cancel buttons
     */
    private fun setupButtons() {
        binding.saveButton.setOnClickListener {
            saveTransaction()
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }

    /**
     * Save the transaction if input is valid
     */
    private fun saveTransaction() {
        // Validate input
        val amount = binding.amountInput.text.toString().toDoubleOrNull()
        val description = binding.descriptionInput.text.toString().trim()

        if (amount == null || amount <= 0) {
            binding.amountInputLayout.error = "Please enter a valid amount"
            return
        } else {
            binding.amountInputLayout.error = null
        }

        if (description.isEmpty()) {
            binding.descriptionInputLayout.error = "Please enter a description"
            return
        } else {
            binding.descriptionInputLayout.error = null
        }

        if (selectedCategoryId.isEmpty()) {
            Toast.makeText(context, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }

        // Call the callback with the transaction data
        onTransactionSubmitted?.invoke(amount, description, selectedCategoryId, selectedDate)

        // Dismiss the dialog
        dismiss()
    }

    /**
     * Observe changes from the ViewModel
     */
    private fun observeViewModel() {
        // Observe categories
        categoryViewModel.categories.observe(viewLifecycleOwner) { categories ->
            setupCategoryAdapter(categories)
        }

        // Observe errors
        categoryViewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                categoryViewModel.clearError()
            }
        }
    }

    /**
     * Set up the category spinner adapter with the loaded categories
     */
    private fun setupCategoryAdapter(categories: List<Category>) {
        // Filter categories based on transaction type if needed
        // For this example, we're using all categories

        if (categories.isEmpty()) {
            binding.categorySpinner.isEnabled = false
            return
        }

        // Create custom adapter for spinner
        val adapter = CategoryAdapter(requireContext(), categories)

        binding.categorySpinner.apply {
            this.adapter = adapter
            isEnabled = true

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}