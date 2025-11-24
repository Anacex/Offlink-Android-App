package com.offlinepayment.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
	private val pkrFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "PK"))

	fun formatPkr(amount: Double): String {
		val formatted = pkrFormat.format(amount)
		// Replace currency symbol with Rs for consistency
		return formatted.replace("₨", "Rs ")
	}
}
