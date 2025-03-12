package com.example.budgetsmart2.presentation.fragments

import android.Manifest
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.budgetsmart2.R
import com.example.budgetsmart2.databinding.FragmentReportsBinding
import com.example.budgetsmart2.presentation.viewModels.ReportsViewModel
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.*

/**
 * ReportsFragment - Provides visual analytics about spending and income
 * This fragment displays:
 * - Bar charts for monthly spending/income
 * - Pie charts for category breakdown
 * - Top spending categories list
 * - Time period selection
 * - Export options
 */
@AndroidEntryPoint
class ReportsFragment : Fragment() {

    // View binding instance
    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    // ViewModel instance using Hilt dependency injection
    private val viewModel: ReportsViewModel by viewModels()

    // Time periods for dropdown
    private val timePeriods = arrayOf(
        "Last Month",
        "Last 3 Months",
        "Last 6 Months",
        "Last Year"
    )


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using view binding
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI components
        setupPeriodSelector()
        setupToggleButtons()
        setupCharts()

        // Initialize ViewModel with current user ID
        FirebaseAuth.getInstance().currentUser?.let { user ->
            viewModel.initialize(user.uid)
        } ?: run {
            // Handle not logged in state
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
        }

        // Observe LiveData from ViewModel
        observeViewModel()
    }

    /**
     * Set up the time period selector dropdown
     */
    private fun setupPeriodSelector() {
        binding.apply {
            // Create and set adapter for time period dropdown
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                timePeriods
            )

            // Set initial text to "Last 3 Months"
            periodDisplay.text = timePeriods[1]

            // Show dropdown when clicked
            periodDisplay.setOnClickListener {
                showTimePeriodDropdown()
            }
            periodDropdown.setOnClickListener {
                showTimePeriodDropdown()
            }
        }
    }

    /**
     * Show time period dropdown menu
     */
    private fun showTimePeriodDropdown() {
        val popup = androidx.appcompat.widget.PopupMenu(requireContext(), binding.periodDisplay)

        // Add time period options
        timePeriods.forEachIndexed { index, period ->
            popup.menu.add(0, index, index, period)
        }

        // Handle selection
        popup.setOnMenuItemClickListener { item ->
            binding.periodDisplay.text = timePeriods[item.itemId]

            // Update time period in ViewModel
            val timePeriod = when (item.itemId) {
                0 -> ReportsViewModel.TimePeriod.LAST_MONTH
                1 -> ReportsViewModel.TimePeriod.LAST_3_MONTHS
                2 -> ReportsViewModel.TimePeriod.LAST_6_MONTHS
                3 -> ReportsViewModel.TimePeriod.LAST_YEAR
                else -> ReportsViewModel.TimePeriod.LAST_3_MONTHS
            }
            viewModel.setTimePeriod(timePeriod)
            true
        }

        popup.show()
    }

    /**
     * Set up toggle buttons for filtering reports
     */
    private fun setupToggleButtons() {
        binding.apply {
            // Expense toggle (selected by default)
            toggleExpenses.setOnClickListener {
                updateToggleState(ReportsViewModel.ReportType.EXPENSES)
            }

            // Income toggle
            toggleIncome.setOnClickListener {
                updateToggleState(ReportsViewModel.ReportType.INCOME)
            }
        }
    }

    /**
     * Update toggle buttons UI state and filter
     */
    private fun updateToggleState(reportType: ReportsViewModel.ReportType) {
        binding.apply {
            // Reset both toggles to default state
            toggleExpenses.setCardBackgroundColor(resources.getColor(R.color.background_variant, null))
            toggleIncome.setCardBackgroundColor(resources.getColor(R.color.background_variant, null))

            toggleExpensesText.setTextColor(resources.getColor(R.color.text_secondary, null))
            toggleIncomeText.setTextColor(resources.getColor(R.color.text_secondary, null))

            // Update selected toggle
            when (reportType) {
                ReportsViewModel.ReportType.EXPENSES -> {
                    toggleExpenses.setCardBackgroundColor(resources.getColor(R.color.primary, null))
                    toggleExpensesText.setTextColor(resources.getColor(R.color.white, null))
                    chartTitle.text = "Monthly Expenses"
                }
                ReportsViewModel.ReportType.INCOME -> {
                    toggleIncome.setCardBackgroundColor(resources.getColor(R.color.primary, null))
                    toggleIncomeText.setTextColor(resources.getColor(R.color.white, null))
                    chartTitle.text = "Monthly Income"
                }
                else -> {
                    // Default to expenses if somehow an invalid type is passed
                    toggleExpenses.setCardBackgroundColor(resources.getColor(R.color.primary, null))
                    toggleExpensesText.setTextColor(resources.getColor(R.color.white, null))
                    chartTitle.text = "Monthly Expenses"
                }
            }

            // Update report type in ViewModel
            viewModel.setReportType(reportType)
        }

    }

    /**
     * Set up chart configurations
     */
    private fun setupCharts() {
        setupPieChart()
    }


    /**
     * Configure the pie chart for category data
     */
    private fun setupPieChart() {
        binding.pieChart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            holeRadius = 45f
            transparentCircleRadius = 50f

            // Legend setup
            legend.isEnabled = true
            legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
            legend.orientation = Legend.LegendOrientation.VERTICAL
            legend.setDrawInside(false)
            legend.textSize = 12f
            legend.form = Legend.LegendForm.CIRCLE


            // Extra styling
            setDrawEntryLabels(false)
            setUsePercentValues(true)
            setExtraOffsets(5f, 5f, 5f, 5f)
        }
    }

    /**
     * Observe all LiveData objects from the ViewModel and update UI accordingly
     */
    private fun observeViewModel() {

        // Observe category expense data for pie chart
        viewModel.categoryExpenseData.observe(viewLifecycleOwner) { data ->
            updatePieChart(data)
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe error state
        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    /**
     * Update pie chart with category data
     */
    private fun updatePieChart(data: List<ReportsViewModel.CategoryExpenseData>) {
        if (data.isEmpty()) {
            binding.pieChart.setNoDataText("No data available")
            binding.pieChart.invalidate()
            return
        }

        // Calculate total for percentages
        val total = data.sumOf { it.amount }

        // Create pie entries
        val entries = data.map { category ->
            val percentage = (category.amount / total) * 100
            PieEntry(percentage.toFloat(), category.categoryName)
        }

        // Create dataset
        val dataSet = PieDataSet(entries, "Categories")
        dataSet.apply {
            // Get colors from category colors or use default colors
            val colors = if (data.size <= 4) {
                data.map { resources.getColor(it.categoryColor, null) }
            } else {
                ColorTemplate.MATERIAL_COLORS.toList()
            }
            this.colors = colors
            valueTextColor = Color.WHITE
            valueTextSize = 12f
            valueFormatter = PercentFormatter()
        }

        // Set data to chart
        val pieData = PieData(dataSet)
        binding.pieChart.data = pieData
        binding.pieChart.invalidate()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up binding reference
        _binding = null
    }
}