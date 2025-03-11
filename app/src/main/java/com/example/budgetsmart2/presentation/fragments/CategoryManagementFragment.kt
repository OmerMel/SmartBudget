package com.example.budgetsmart2.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgetsmart2.databinding.FragmentCategoryManagmentBinding
import com.example.budgetsmart2.domain.dataClasses.Category
import com.example.budgetsmart2.presentation.adapters.CategoryListAdapter
import com.example.budgetsmart2.presentation.dialogs.AddEditCategoryDialog
import com.example.budgetsmart2.presentation.viewModels.CategoryViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

/**
 * CategoryManagementFragment - Allows users to create, edit, and manage categories
 * This fragment enables users to:
 * - View all existing categories
 * - Add new categories
 * - Edit existing categories
 * - Delete categories (with validation)
 */
@AndroidEntryPoint
class CategoryManagementFragment : Fragment() {

    // View binding instance
    private var _binding: FragmentCategoryManagmentBinding? = null
    private val binding get() = _binding!!

    // ViewModel instance using Hilt dependency injection
    private val viewModel: CategoryViewModel by viewModels()

    // Category adapter
    private lateinit var categoryAdapter: CategoryListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using view binding
        _binding = FragmentCategoryManagmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI components
        setupRecyclerView()
        setupClickListeners()

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
     * Set up the RecyclerView for categories
     */
    private fun setupRecyclerView() {
        categoryAdapter = CategoryListAdapter(
            onEditClick = ::onCategoryEditClick,
            onDeleteClick = ::onCategoryDeleteClick
        )

        binding.categoriesRecyclerView.apply {
            adapter = categoryAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    /**
     * Set up click listeners for interactive UI elements
     */
    private fun setupClickListeners() {
        binding.apply {
            // Add Category FAB
            fabAddCategory.setOnClickListener {
                showAddCategoryDialog()
            }
        }
    }

    /**
     * Observe all LiveData objects from the ViewModel and update UI accordingly
     */
    private fun observeViewModel() {
        // Observe categories
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            updateCategoriesList(categories)
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

        // Observe category saved event
        viewModel.categorySaved.observe(viewLifecycleOwner) { saved ->
            if (saved) {
                Toast.makeText(
                    context,
                    "Category saved successfully",
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.resetCategorySaved()
            }
        }
    }

    /**
     * Update UI with categories list
     */
    private fun updateCategoriesList(categories: List<Category>) {
        if (categories.isEmpty()) {
            // Show empty state
            binding.apply {
                emptyStateView.visibility = View.VISIBLE
                categoriesRecyclerView.visibility = View.GONE
            }
            return
        }

        // Show categories list
        binding.apply {
            emptyStateView.visibility = View.GONE
            categoriesRecyclerView.visibility = View.VISIBLE
        }

        // Update the adapter
        categoryAdapter.submitList(categories)
    }

    /**
     * Show dialog to add a new category
     */
    private fun showAddCategoryDialog() {
        AddEditCategoryDialog.newInstance { name, icon, color ->
            viewModel.createCategory(name, icon, color)
        }.show(childFragmentManager, "AddCategoryDialog")
    }

    /**
     * Handle category edit click
     */
    private fun onCategoryEditClick(category: Category) {
        AddEditCategoryDialog.newInstance(
            category = category
        ) { name, icon, color ->
            viewModel.updateCategory(category.id, name, icon, color)
        }.show(childFragmentManager, "EditCategoryDialog")
    }

    /**
     * Handle category delete click
     */
    private fun onCategoryDeleteClick(category: Category) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete this category? This cannot be undone.")
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Delete") { dialog, _ ->
                viewModel.deleteCategory(category.id)
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up binding reference
        _binding = null
    }
}