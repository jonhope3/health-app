package com.fittrack.app.util

import com.fittrack.app.ui.log.MeasureUnit
import java.util.Locale

data class ServingSizeInfo(
    val amount: Float,
    val unit: MeasureUnit,
    val baseGrams: Float
)

fun parseServingSize(description: String?): ServingSizeInfo {
    val default = ServingSizeInfo(100f, MeasureUnit.GRAMS, 100f)
    if (description == null) return default
    
    val desc = description.lowercase(Locale.US)
    
    // Try to find the weight in parentheses first e.g. "1 cup (24g)"
    val parenMatch = Regex("""\(([\d./\s]+)\s*(g|ml|oz|oz\.)\)""").find(desc)
    val baseGrams = if (parenMatch != null) {
        val valStr = parenMatch.groupValues[1]
        val unitStr = parenMatch.groupValues[2]
        val value = parseAmount(valStr) ?: 100f
        when {
            unitStr.contains("g") -> value
            unitStr.contains("ml") -> value
            unitStr.contains("oz") -> value * 28.35f
            else -> value
        }
    } else {
        null
    }

    // Capture broad numeric range including fractions: "1 1/2", "0.5", "1/4"
    val numPattern = """[\d.\s/]+"""

    // Try to find the leading amount and unit
    val unitEntries = MeasureUnit.entries.sortedByDescending { it.label.length }
    for (unit in unitEntries) {
        val unitLabel = unit.label.lowercase(Locale.US)
        // Look for number immediately followed by unit, ensuring we don't start inside a number
        val regex = Regex("""(?<![\d./])($numPattern)\s*$unitLabel\b""")
        val match = regex.find(desc)
        if (match != null) {
            val amountStr = match.groupValues[1].trim()
            val amount = parseAmount(amountStr) ?: 1f
            return ServingSizeInfo(
                amount = amount,
                unit = unit,
                baseGrams = baseGrams ?: (amount * unit.gramsPerUnit)
            )
        }
    }
    
    // Fallback for "1 serving" or similar
    val numMatch = Regex("""($numPattern)""").find(desc)
    if (numMatch != null) {
        val amount = parseAmount(numMatch.groupValues[1].trim()) ?: 1f
        return ServingSizeInfo(
            amount = amount,
            unit = MeasureUnit.GRAMS,
            baseGrams = baseGrams ?: 100f
        )
    }

    return default
}

fun parseAmount(text: String): Float? {
    val clean = text.trim()
    if (clean.isEmpty()) return null
    
    // Try plain decimal
    clean.toFloatOrNull()?.let { return it }
    
    // Try simple fraction "1/2"
    if (clean.contains("/")) {
        val parts = clean.split(Regex("""\s+""")).filter { it.isNotBlank() }
        var total = 0f
        for (part in parts) {
            if (part.contains("/")) {
                val fParts = part.split("/")
                if (fParts.size == 2) {
                    val num = fParts[0].toFloatOrNull() ?: 0f
                    val den = fParts[1].toFloatOrNull() ?: 1f
                    total += num / den
                }
            } else {
                total += part.toFloatOrNull() ?: 0f
            }
        }
        if (total > 0) return total
    }
    
    return null
}
