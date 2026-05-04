package com.fittrack.app.services

import android.graphics.Bitmap
import com.fittrack.app.data.NutritionResult
import com.fittrack.app.data.ParsedFoodItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AiFoodParserService(
    private val geminiNanoService: GeminiNanoService = GeminiNanoService()
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun parseFood(input: String): List<ParsedFoodItem> = withContext(Dispatchers.IO) {
        if (!geminiNanoService.initIfNeeded()) return@withContext emptyList()
        val prompt = PARSE_PROMPT + input.trim() + "\"\nOutput:"
        
        var attempts = 0
        while (attempts < 2) {
            try {
                val response = geminiNanoService.generateContent(prompt)
                if (response.isNotBlank()) {
                    val result = extractJsonArray(response)
                    if (result.isNotEmpty()) return@withContext result
                }
            } catch (e: Exception) {
                // Ignore and retry
            }
            attempts++
        }
        emptyList()
    }

    suspend fun aiNutritionLookup(food: String): NutritionResult? = withContext(Dispatchers.IO) {
        if (!geminiNanoService.initIfNeeded()) return@withContext null

        val prompt = """<instruction>
Return the nutrition facts for 100g of $food as a JSON object with these fields: name (string), calories (number), protein (number, grams), carbs (number, grams), fat (number, grams), sugar (number, total sugars in grams). Use standard USDA values. Return ONLY the JSON object.
</instruction>
<example_output>
{"name":"Banana","calories":89,"protein":1.1,"carbs":22.8,"fat":0.3,"sugar":12.2}
</example_output>"""

        var attempts = 0
        while (attempts < 2) {
            try {
                val response = geminiNanoService.generateContent(prompt)
                if (response.isNotBlank()) {
                    val jsonObj = extractJsonObject(response)
                    if (jsonObj != null) {
                        val calories = jsonObj["calories"]?.jsonPrimitive?.content?.toIntOrNull()
                        if (calories != null && calories > 0) {
                            return@withContext NutritionResult(
                                name = jsonObj["name"]?.jsonPrimitive?.content ?: food,
                                calories = calories,
                                protein = round1(jsonObj["protein"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0),
                                carbs = round1(jsonObj["carbs"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0),
                                fat = round1(jsonObj["fat"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0),
                                sugar = round1(jsonObj["sugar"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0),
                                servingDescription = "100g"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore and retry
            }
            attempts++
        }
        null
    }

    private fun extractJsonArray(text: String): List<ParsedFoodItem> {
        return try {
            var searchText = text
            val codeBlockMatch = Regex("""```(?:json)?\s*([\s\S]*?)```""").find(text)
            if (codeBlockMatch != null) {
                searchText = codeBlockMatch.groupValues[1].trim()
            }
            val jsonMatch = Regex("""\[[\s\S]*?]""").find(searchText) ?: return emptyList()
            val parsed = json.parseToJsonElement(jsonMatch.value).jsonArray
            if (parsed.isEmpty()) return emptyList()

            parsed.mapNotNull { element ->
                val item = element.jsonObject
                val name = item["name"]?.jsonPrimitive?.content?.trim() ?: return@mapNotNull null
                val calories = item["calories"]?.jsonPrimitive?.content?.toIntOrNull() ?: return@mapNotNull null
                if (calories <= 0) return@mapNotNull null

                ParsedFoodItem(
                    name = name,
                    calories = safeDouble(item["calories"]?.jsonPrimitive?.content).toInt(),
                    protein = round1(safeDouble(item["protein"]?.jsonPrimitive?.content)),
                    carbs = round1(safeDouble(item["carbs"]?.jsonPrimitive?.content)),
                    fat = round1(safeDouble(item["fat"]?.jsonPrimitive?.content)),
                    sugar = round1(safeDouble(item["sugar"]?.jsonPrimitive?.content)),
                    quantity = item["quantity"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: "1 serving",
                    confidence = item["confidence"]?.jsonPrimitive?.content?.toFloatOrNull()?.coerceIn(0f, 1f)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun extractJsonObject(text: String): kotlinx.serialization.json.JsonObject? {
        return try {
            var searchText = text
            val codeBlockMatch = Regex("""```(?:json)?\s*([\s\S]*?)```""").find(text)
            if (codeBlockMatch != null) {
                searchText = codeBlockMatch.groupValues[1].trim()
            }
            val match = Regex("""\{[\s\S]*?\}""").find(searchText) ?: return null
            json.parseToJsonElement(match.value).jsonObject
        } catch (e: Exception) {
            null
        }
    }

    suspend fun parseNutritionLabel(bitmap: Bitmap): NutritionResult? = withContext(Dispatchers.IO) {
        try {
            if (!geminiNanoService.initIfNeeded()) return@withContext null

            val response = geminiNanoService.generateContent(bitmap, LABEL_PROMPT)
            if (response.isBlank()) return@withContext null

            val jsonObj = extractJsonObject(response) ?: return@withContext null
            val name = jsonObj["name"]?.jsonPrimitive?.content?.trim() ?: "Unknown"
            if (name == "NOT_FOUND") return@withContext null
            val calories = jsonObj["calories"]?.jsonPrimitive?.content?.toIntOrNull() ?: return@withContext null
            if (calories <= 0) return@withContext null

            val servingSize = jsonObj["serving_size"]?.jsonPrimitive?.content ?: "1 serving"
            val confidence = jsonObj["confidence"]?.jsonPrimitive?.content?.toFloatOrNull()?.coerceIn(0f, 1f)

            NutritionResult(
                name = name,
                calories = safeDouble(jsonObj["calories"]?.jsonPrimitive?.content).toInt(),
                protein = round1(safeDouble(jsonObj["protein"]?.jsonPrimitive?.content)),
                carbs = round1(safeDouble(jsonObj["carbs"]?.jsonPrimitive?.content)),
                fat = round1(safeDouble(jsonObj["fat"]?.jsonPrimitive?.content)),
                sugar = round1(safeDouble(jsonObj["sugar"]?.jsonPrimitive?.content)),
                servingDescription = servingSize,
                confidence = confidence
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun safeDouble(text: String?): Double {
        if (text == null) return 0.0
        // Use regex to find the first numeric part (e.g. "1.5g" -> "1.5")
        val match = Regex("""([\d.]+)""").find(text)
        return match?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    }

    private fun round1(n: Double): Float = (kotlin.math.round(n * 10) / 10.0).toFloat()

    companion object {
        private const val PARSE_PROMPT = """<instruction>
You are a nutrition data extractor. Given a food description, extract each individual food item with estimated nutrition per serving. Return ONLY a JSON array, no other text or markdown.

Each item in the array must have these fields:
- name (string): food name
- calories (number): kcal per serving
- protein (number): grams
- carbs (number): grams
- fat (number): grams
- sugar (number): total sugars in grams
- quantity (string): serving description like "2 large" or "1 slice"
- confidence (number): 0.0-1.0 accuracy estimate

Use standard USDA nutritional values. If unsure, provide reasonable estimates with lower confidence.
</instruction>

<examples>
Input: "2 scrambled eggs with toast"
Output: [{"name":"Scrambled eggs","calories":182,"protein":12.6,"carbs":1.6,"fat":13.4,"sugar":0.3,"quantity":"2 large","confidence":0.85},{"name":"White toast","calories":79,"protein":2.7,"carbs":14.8,"fat":1.0,"sugar":1.4,"quantity":"1 slice","confidence":0.9}]

Input: "handful of almonds"
Output: [{"name":"Almonds","calories":164,"protein":6,"carbs":6,"fat":14,"sugar":1.2,"quantity":"1 oz (23 almonds)","confidence":0.7}]
</examples>

<input>
"""

        private const val LABEL_PROMPT = """<instruction>
You are a nutrition label reader. Analyze the image and extract nutrition facts data.

If the image does NOT contain a nutrition label, return: {"name":"NOT_FOUND","calories":0}

If a nutrition label IS visible, return a JSON object with these fields:
- name (string): product name if visible, otherwise "Unknown"
- serving_size (string): serving size from label, use decimals not fractions
- calories (number): kcal as a pure number
- protein (number): grams as a pure number
- carbs (number): total carbohydrates in grams as a pure number
- fat (number): total fat in grams as a pure number
- sugar (number): total sugars in grams as a pure number
- confidence (number): 0.0-1.0 readability confidence

Return ONLY the JSON object, no other text or markdown.
</instruction>

<example_output>
{"name":"Cheerios","serving_size":"1.5 cups (58g)","calories":210,"protein":5,"carbs":44,"fat":3.5,"sugar":12,"confidence":0.95}
</example_output>"""
    }
}
