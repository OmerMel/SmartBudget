package com.example.budgetsmart2.utils

import android.content.Context
import java.text.NumberFormat

/**
 * Utility class to provide consistent currency formatting throughout the app
 */
object CurrencyFormatter {

    /**
     * Format a monetary value according to the user's selected currency
     * This is the main method that should be used throughout the app
     */
    fun format(context: Context, amount: Double): String {
        return CurrencyHelper.formatAmount(context, amount)
    }

    /**
     * Format a monetary value with a sign (+ for positive, - for negative)
     * Useful for displaying transactions
     */
    fun formatWithSign(context: Context, amount: Double, forceSign: Boolean = false): String {
        val formatted = format(context, Math.abs(amount))

        return when {
            amount < 0 -> "-$formatted"
            amount > 0 && forceSign -> "+$formatted"
            else -> formatted
        }
    }

    /**
     * Format a monetary value without currency symbol
     * Useful for some UI elements or inputs
     */
    fun formatNumberOnly(amount: Double): String {
        val numberFormat = NumberFormat.getNumberInstance()
        numberFormat.minimumFractionDigits = 2
        numberFormat.maximumFractionDigits = 2
        return numberFormat.format(amount)
    }

    /**
     * Get just the currency symbol for the current currency
     */
    fun getCurrencySymbol(context: Context): String {
        val currencyInfo = CurrencyHelper.getCurrentCurrencyInfo(context)
        return currencyInfo.symbol
    }
}