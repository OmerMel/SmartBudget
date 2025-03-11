package com.example.budgetsmart2.utils

import android.content.Context
import androidx.preference.PreferenceManager
import java.text.NumberFormat
import java.util.*
import androidx.core.content.edit

/**
 * Helper class for currency formatting and management
 */
object CurrencyHelper {

    // Constants for SharedPreferences
    private const val PREF_CURRENCY_CODE = "currency_code"
    private const val DEFAULT_CURRENCY_CODE = "USD"

    // Currency data
    data class CurrencyInfo(val code: String, val name: String, val symbol: String, val locale: Locale)

    // List of supported currencies
    val SUPPORTED_CURRENCIES = listOf(
        CurrencyInfo("USD", "US Dollar", "$", Locale.US),
        CurrencyInfo("EUR", "Euro", "€", Locale.GERMANY),
        CurrencyInfo("GBP", "British Pound", "£", Locale.UK),
        CurrencyInfo("ILS", "Israeli Shekel", "₪", Locale.getDefault()),
        CurrencyInfo("CAD", "Canadian Dollar", "C$", Locale.CANADA)
    )

    /**
     * Get the user's selected currency code
     */
    fun getUserCurrencyCode(context: Context): String {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return sharedPreferences.getString(PREF_CURRENCY_CODE, DEFAULT_CURRENCY_CODE) ?: DEFAULT_CURRENCY_CODE
    }

    /**
     * Save the user's selected currency code
     */
    fun saveUserCurrencyCode(context: Context, currencyCode: String) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPreferences.edit() { putString(PREF_CURRENCY_CODE, currencyCode) }
    }

    /**
     * Get the CurrencyInfo for the current currency code
     */
    fun getCurrentCurrencyInfo(context: Context): CurrencyInfo {
        val currencyCode = getUserCurrencyCode(context)
        return SUPPORTED_CURRENCIES.find { it.code == currencyCode } ?: SUPPORTED_CURRENCIES[0]
    }

    /**
     * Format a monetary value according to the user's selected currency
     */
    fun formatAmount(context: Context, amount: Double): String {
        val currencyInfo = getCurrentCurrencyInfo(context)

        val numberFormat = NumberFormat.getCurrencyInstance(currencyInfo.locale)
        numberFormat.currency = Currency.getInstance(currencyInfo.code)

        return numberFormat.format(amount)
    }

    /**
     * Get the display text for the currency selection
     */
    fun getCurrencyDisplayText(currencyCode: String): String {
        val currency = SUPPORTED_CURRENCIES.find { it.code == currencyCode }
            ?: return "$currencyCode - Unknown Currency"

        return "${currency.code} - ${currency.name}"
    }

    /**
     * Get the index of a currency code in the supported currencies list
     */
    fun getCurrencyIndex(currencyCode: String): Int {
        return SUPPORTED_CURRENCIES.indexOfFirst { it.code == currencyCode }.takeIf { it >= 0 } ?: 0
    }
}