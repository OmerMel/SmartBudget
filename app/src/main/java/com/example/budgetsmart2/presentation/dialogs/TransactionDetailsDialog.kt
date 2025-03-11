package com.example.budgetsmart2.presentation.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.budgetsmart2.R
import com.example.budgetsmart2.databinding.DialogTansactionDetailsBinding
import com.example.budgetsmart2.domain.dataClasses.Transaction
import com.example.budgetsmart2.domain.dataClasses.TransactionWithCategory
import com.example.budgetsmart2.domain.enums.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog for displaying transaction details with options to edit or delete
 */
class TransactionDetailsDialog : DialogFragment() {

    // View binding
    private var _binding: DialogTansactionDetailsBinding? = null
    private val binding get() = _binding!!

    // Transaction data
    private lateinit var transaction: TransactionWithCategory

    // Callback for when an action is taken
    private var onActionCallback: ((Action, Transaction) -> Unit)? = null

    // Formatters
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
    private val dateFormatter = SimpleDateFormat("MMMM dd, yyyy - HH:mm", Locale.getDefault())

    /**
     * Actions that can be taken from the dialog
     */
    enum class Action {
        VIEW,   // Just view the details
        EDIT,   // Edit the transaction
        DELETE  // Delete the transaction
    }

    companion object {
        private const val ARG_TRANSACTION = "transaction"

        /**
         * Creates a new instance of the dialog
         * @param transaction The transaction to display
         * @param callback Callback function that receives the action and the transaction
         */
        fun newInstance(
            transaction: TransactionWithCategory,
            callback: (Action, Transaction) -> Unit
        ): TransactionDetailsDialog {
            val args = Bundle().apply {
                putParcelable(ARG_TRANSACTION, transaction)
            }

            return TransactionDetailsDialog().apply {
                arguments = args
                onActionCallback = callback
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Dialog_FullWidth)

        // Get transaction from arguments
        transaction = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(ARG_TRANSACTION, TransactionWithCategory::class.java)
                ?: throw IllegalArgumentException("Transaction must be provided")
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable(ARG_TRANSACTION)
                ?: throw IllegalArgumentException("Transaction must be provided")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTansactionDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Display transaction details
        setupDialog()
        setupButtons()
    }

    /**
     * Set up the dialog with transaction details
     */
    private fun setupDialog() {
        val isExpense = transaction.transaction.type == TransactionType.EXPENSE

        binding.apply {
            // Set title color based on transaction type
            val colorRes = if (isExpense) R.color.expense else R.color.income
            dialogTitle.setTextColor(ContextCompat.getColor(requireContext(), colorRes))

            // Set transaction details
            transactionDescription.text = transaction.transaction.description

            // Format amount with currency and sign
            val amount = transaction.transaction.amount
            val formattedAmount = if (isExpense) {
                "-${currencyFormatter.format(amount)}"
            } else {
                "+${currencyFormatter.format(amount)}"
            }
            transactionAmount.text = formattedAmount
            transactionAmount.setTextColor(ContextCompat.getColor(requireContext(), colorRes))

            // Set category details
            categoryName.text = transaction.category.name
            categoryIcon.text = transaction.category.icon
            categoryIconBg.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), transaction.category.color)
            )

            // Set date
            transactionDate.text = dateFormatter.format(transaction.transaction.date)

            // Set transaction type text
            transactionType.text = if (isExpense) "Expense" else "Income"
            transactionType.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
        }
    }

    /**
     * Set up button click listeners
     */
    private fun setupButtons() {
        binding.apply {
            // Edit button
            editButton.setOnClickListener {
                onActionCallback?.invoke(Action.EDIT, transaction.transaction)
                dismiss()
            }

            // Delete button
            deleteButton.setOnClickListener {
                onActionCallback?.invoke(Action.DELETE, transaction.transaction)
                dismiss()
            }

            // Close button
            closeButton.setOnClickListener {
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}