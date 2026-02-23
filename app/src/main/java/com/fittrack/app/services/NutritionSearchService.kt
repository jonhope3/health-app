package com.fittrack.app.services

import com.fittrack.app.data.NutritionResult
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

class NutritionSearchService(
        private val geminiNanoService: com.fittrack.app.services.GeminiNanoService,
        private val client: OkHttpClient =
                OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS)
                        .build()
) {
        private val json = Json { ignoreUnknownKeys = true }

        suspend fun searchNutrition(
                food: String,
                onStatus: (String) -> Unit = {}
        ): NutritionResult? =
                withContext(Dispatchers.IO) {
                        withTimeoutOrNull(15000L) { searchDuckDuckGo(food, onStatus) }
                }

        private suspend fun searchUSDA(food: String): NutritionResult? {
                return try {
                        val query = java.net.URLEncoder.encode(food, "UTF-8")
                        val url =
                                "https://api.nal.usda.gov/fdc/v1/foods/search?api_key=DEMO_KEY&query=$query&dataType=Foundation,SR%20Legacy,Branded&pageSize=20"
                        val request = Request.Builder().url(url).build()
                        val response = client.newCall(request).execute()
                        if (!response.isSuccessful) return null

                        val body = response.body?.string() ?: return null
                        val root = json.parseToJsonElement(body).jsonObject
                        val foods = root["foods"]?.jsonArray ?: return null
                        if (foods.isEmpty()) return null

                        val foodLower = food.lowercase().trim()
                        val searchWords = foodLower.split(Regex("\\s+"))
                        // Significant words: skip short words and pure quantity tokens like "1",
                        // "2"
                        val sigWords =
                                searchWords.filter { it.length > 2 && it.toIntOrNull() == null }

                        fun fuzzyMatch(word: String, text: String): Boolean {
                                if (text.contains(word)) return true
                                val ws = stem(word)
                                return text.split(Regex("[\\s,]+")).any { stem(it) == ws }
                        }

                        data class Scored(val food: JsonObject, val score: Double)

                        val scored =
                                foods.mapNotNull { element ->
                                        val f = element.jsonObject
                                        val desc =
                                                (f["description"]?.jsonPrimitive?.content ?: "")
                                                        .lowercase()
                                        if (desc.isEmpty()) return@mapNotNull null

                                        val nutrients =
                                                f["foodNutrients"]?.jsonArray
                                                        ?: return@mapNotNull null
                                        val cal = getKcal(nutrients)
                                        if (cal <= 0) return@mapNotNull null

                                        // Hard requirement: every significant search word must
                                        // appear somewhere in desc
                                        val allSigPresent = sigWords.all { fuzzyMatch(it, desc) }
                                        if (!allSigPresent) return@mapNotNull null

                                        var score = 0.0
                                        val mainPart =
                                                desc.replace(Regex("\\([^)]*\\)"), " ")
                                                        .replace(Regex("\\s+"), " ")
                                                        .trim()
                                        val mainWords =
                                                mainPart.split(Regex("[\\s,]+")).filter {
                                                        it.isNotEmpty()
                                                }
                                        val mainMatches =
                                                searchWords.count { fuzzyMatch(it, mainPart) }
                                        val allMatch = mainMatches == searchWords.size

                                        if (allMatch) {
                                                score = 80.0
                                                if (searchWords.any { mainPart.startsWith(it) })
                                                        score += 10
                                                if (mainPart.contains("raw")) score += 8
                                                if (mainWords.size <= searchWords.size + 2)
                                                        score += 5
                                        } else {
                                                val fullMatches =
                                                        searchWords.count { fuzzyMatch(it, desc) }
                                                score =
                                                        (fullMatches.toDouble() /
                                                                searchWords.size) * 40
                                        }

                                        if (Regex(
                                                                "dehydrat|dried|powder|juice|canned|frozen|breaded|fried"
                                                        )
                                                        .containsMatchIn(desc)
                                        ) {
                                                score -= 30
                                        }
                                        if (Regex(
                                                                "cooked|boiled|baked|braised|roasted|rotisserie|grilled|stewed"
                                                        )
                                                        .containsMatchIn(desc)
                                        ) {
                                                score -= 10
                                        }
                                        val extraWords =
                                                (mainWords.size - searchWords.size).coerceAtLeast(0)
                                        score -= extraWords * 3
                                        score -= ((desc.length - 40).coerceAtLeast(0) * 0.3)

                                        Scored(f, score)
                                }

                        val best = scored.maxByOrNull { it.score } ?: return null
                        // Reject if the best match is not relevant enough — prevents returning
                        // wrong foods
                        if (best.score < 15) return null
                        val bestNutrients = best.food["foodNutrients"]?.jsonArray ?: return null
                        val nutrientsMap = mutableMapOf<String, Double>()
                        bestNutrients.forEach { n ->
                                val name =
                                        n.jsonObject["nutrientName"]?.jsonPrimitive?.content
                                                ?: return@forEach
                                val value =
                                        n.jsonObject["value"]?.jsonPrimitive?.content
                                                ?.toDoubleOrNull()
                                                ?: return@forEach
                                nutrientsMap[name] = value
                        }

                        val desc = best.food["description"]?.jsonPrimitive?.content ?: food
                        val nameParts = desc.split(",").map { it.trim() }
                        var displayName = nameParts.getOrElse(0) { food }
                        if (nameParts.size > 1) {
                                val second = nameParts[1].lowercase()
                                if (searchWords.any { second.contains(it) }) {
                                        displayName += " ${nameParts[1]}"
                                        if (nameParts.size > 2) {
                                                displayName +=
                                                        " (${nameParts.drop(2).joinToString(", ")})"
                                        }
                                } else {
                                        displayName += " (${nameParts.drop(1).joinToString(", ")})"
                                }
                        }
                        if (displayName.isNotEmpty()) {
                                displayName =
                                        displayName.first().uppercase() +
                                                displayName.drop(1).lowercase()
                        }

                        NutritionResult(
                                name = displayName,
                                calories = getKcal(bestNutrients).toInt(),
                                protein = round1(nutrientsMap["Protein"] ?: 0.0),
                                carbs = round1(nutrientsMap["Carbohydrate, by difference"] ?: 0.0),
                                fat = round1(nutrientsMap["Total lipid (fat)"] ?: 0.0),
                                servingDescription = "100g"
                        )
                } catch (e: Exception) {
                        null
                }
        }

        private fun getKcal(foodNutrients: JsonArray): Double {
                for (n in foodNutrients) {
                        val obj = n.jsonObject
                        val name = obj["nutrientName"]?.jsonPrimitive?.content ?: ""
                        val unit = (obj["unitName"]?.jsonPrimitive?.content ?: "").uppercase()
                        if (unit == "KJ") continue
                        if (name == "Energy" || name.startsWith("Energy (Atwater")) {
                                val value =
                                        obj["value"]?.jsonPrimitive?.content?.toDoubleOrNull()
                                                ?: 0.0
                                if (value > 0) return value
                        }
                }
                return 0.0
        }

        private fun stem(w: String): String =
                w.replace(Regex("(ing|ed|es|s)$", RegexOption.IGNORE_CASE), "")

        private fun round1(n: Double): Float = (kotlin.math.round(n * 10) / 10.0).toFloat()

        private suspend fun searchOpenFoodFacts(food: String): NutritionResult? {
                return try {
                        val query = java.net.URLEncoder.encode(food, "UTF-8")
                        val url =
                                "https://world.openfoodfacts.org/cgi/search.pl?search_terms=$query&json=1&page_size=10&fields=product_name,nutriments"
                        val request =
                                Request.Builder()
                                        .url(url)
                                        .addHeader("User-Agent", "FitTrack/1.0 (fitness-app)")
                                        .build()
                        val response = client.newCall(request).execute()
                        if (!response.isSuccessful) return null

                        val body = response.body?.string() ?: return null
                        val root = json.parseToJsonElement(body).jsonObject
                        val products = root["products"]?.jsonArray ?: return null
                        if (products.isEmpty()) return null

                        for (p in products) {
                                val product = p.jsonObject
                                val nutriments = product["nutriments"]?.jsonObject ?: continue
                                val calories =
                                        nutriments["energy-kcal_100g"]?.jsonPrimitive?.content
                                                ?.toDoubleOrNull()
                                                ?: nutriments["energy-kcal_serving"]?.jsonPrimitive
                                                        ?.content?.toDoubleOrNull()
                                                        ?: 0.0
                                if (calories <= 0) continue

                                val hasPer100g = nutriments["energy-kcal_100g"] != null
                                val productName =
                                        product["product_name"]?.jsonPrimitive?.content?.takeIf {
                                                it.isNotBlank()
                                        }
                                                ?: continue
                                // Skip products whose name doesn't match any search word
                                val nameLower = productName.lowercase()
                                val searchWords =
                                        food.lowercase().split(Regex("\\s+")).filter {
                                                it.length > 2
                                        }
                                val anyMatch =
                                        searchWords.isEmpty() ||
                                                searchWords.any { nameLower.contains(it) }
                                if (!anyMatch) continue

                                return NutritionResult(
                                        name = productName,
                                        calories = calories.toInt(),
                                        protein =
                                                round1(
                                                        nutriments["proteins_100g"]?.jsonPrimitive
                                                                ?.content?.toDoubleOrNull()
                                                                ?: 0.0
                                                ),
                                        carbs =
                                                round1(
                                                        nutriments["carbohydrates_100g"]
                                                                ?.jsonPrimitive?.content
                                                                ?.toDoubleOrNull()
                                                                ?: 0.0
                                                ),
                                        fat =
                                                round1(
                                                        nutriments["fat_100g"]?.jsonPrimitive
                                                                ?.content?.toDoubleOrNull()
                                                                ?: 0.0
                                                ),
                                        servingDescription = if (hasPer100g) "100g" else "serving"
                                )
                        }
                        null
                } catch (e: Exception) {
                        null
                }
        }

        private suspend fun searchDuckDuckGo(
                food: String,
                onStatus: (String) -> Unit
        ): NutritionResult? {
                return try {
                        onStatus("Searching web...")
                        val query =
                                java.net.URLEncoder.encode(
                                        "$food nutrition facts calories protein carbs fat",
                                        "UTF-8"
                                )
                        val url = "https://lite.duckduckgo.com/lite/?q=$query"
                        val request =
                                Request.Builder()
                                        .url(url)
                                        .addHeader(
                                                "User-Agent",
                                                "Mozilla/5.0 (Linux; Android 14; Pixel 10 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
                                        )
                                        .build()
                        val response = client.newCall(request).execute()
                        if (!response.isSuccessful) return null

                        val html = response.body?.string() ?: return null
                        val texts = mutableListOf<String>()

                        val snippetRe =
                                Regex(
                                        "<td[^>]*class=\"result-snippet\"[^>]*>([\\s\\S]*?)</td>",
                                        RegexOption.IGNORE_CASE
                                )
                        snippetRe.findAll(html).forEach { m ->
                                val t = stripHtml(m.groupValues[1])
                                if (t.length > 10) texts.add(t)
                        }

                        val tdRe = Regex("<td[^>]*>([\\s\\S]*?)</td>", RegexOption.IGNORE_CASE)
                        tdRe.findAll(html).forEach { m ->
                                val t = stripHtml(m.groupValues[1])
                                if (t.length > 30 &&
                                                Regex(
                                                                "(calori|protein|carb|fat|sugar)",
                                                                RegexOption.IGNORE_CASE
                                                        )
                                                        .containsMatchIn(t) &&
                                                t !in texts
                                ) {
                                        texts.add(t)
                                }
                        }

                        if (texts.isEmpty()) return null
                        val all = texts.joinToString(" ")

                        // Try AI parsing first
                        if (geminiNanoService.initIfNeeded()) {
                                onStatus("Parsing with AI...")
                                try {
                                        val prompt =
                                                "" +
                                                        "Extract nutrition facts from the following text as a JSON object with exactly these keys: " +
                                                        "\"name\" (string), \"calories\" (integer), \"protein_g\" (float), \"carbs_g\" (float), \"fat_g\" (float), \"sugar_g\" (float), \"serving_size\" (string). " +
                                                        "If a value is missing, use 0 for numbers and \"Unknown\" for strings. " +
                                                        "Output ONLY valid JSON.\n\nText: $all"
                                        val aiResponse = geminiNanoService.generateContent(prompt)
                                        val cleaned =
                                                aiResponse
                                                        .replace(Regex("```json\\s*"), "")
                                                        .replace("```", "")
                                                        .trim()
                                        val root = json.parseToJsonElement(cleaned).jsonObject

                                        val cals =
                                                root["calories"]
                                                        ?.jsonPrimitive
                                                        ?.content
                                                        ?.toDoubleOrNull()
                                                        ?.toInt()
                                                        ?: 0
                                        if (cals > 0) {
                                                return NutritionResult(
                                                        name =
                                                                root["name"]?.jsonPrimitive?.content
                                                                        ?.takeIf { it != "Unknown" }
                                                                        ?: food,
                                                        calories = cals,
                                                        protein =
                                                                root["protein_g"]?.jsonPrimitive
                                                                        ?.content?.toFloatOrNull()
                                                                        ?: 0f,
                                                        carbs =
                                                                root["carbs_g"]?.jsonPrimitive
                                                                        ?.content?.toFloatOrNull()
                                                                        ?: 0f,
                                                        fat =
                                                                root["fat_g"]?.jsonPrimitive
                                                                        ?.content?.toFloatOrNull()
                                                                        ?: 0f,
                                                        sugar =
                                                                root["sugar_g"]?.jsonPrimitive
                                                                        ?.content?.toFloatOrNull()
                                                                        ?: 0f,
                                                        servingDescription =
                                                                root["serving_size"]?.jsonPrimitive
                                                                        ?.content?.takeIf {
                                                                        it != "Unknown"
                                                                }
                                                                        ?: "serving"
                                                )
                                        }
                                } catch (e: Exception) {
                                        // Fallback to regex if AI fails
                                }
                        }

                        onStatus("Parsing result...")
                        val calories =
                                pickNum(
                                        all,
                                        listOf(
                                                Regex(
                                                        """(\d+(?:\.\d+)?)\s*(?:kcal|calories|cals)\b""",
                                                        RegexOption.IGNORE_CASE
                                                ),
                                                Regex(
                                                        """calories[:\s]+(\d+(?:\.\d+)?)""",
                                                        RegexOption.IGNORE_CASE
                                                ),
                                                Regex(
                                                        """(\d+(?:\.\d+)?)\s*cal(?:orie)?s?\b""",
                                                        RegexOption.IGNORE_CASE
                                                )
                                        )
                                )
                        if (calories <= 0) return null

                        NutritionResult(
                                name = food,
                                calories = calories,
                                protein =
                                        round1(
                                                pickNum(
                                                                all,
                                                                listOf(
                                                                        Regex(
                                                                                """protein[:\s]+(\d+(?:\.\d+)?)\s*g""",
                                                                                RegexOption
                                                                                        .IGNORE_CASE
                                                                        ),
                                                                        Regex(
                                                                                """(\d+(?:\.\d+)?)\s*g\s+(?:of\s+)?protein""",
                                                                                RegexOption
                                                                                        .IGNORE_CASE
                                                                        )
                                                                )
                                                        )
                                                        .toDouble()
                                        ),
                                carbs =
                                        round1(
                                                pickNum(
                                                                all,
                                                                listOf(
                                                                        Regex(
                                                                                """carb(?:ohydrate)?s?[:\s]+(\d+(?:\.\d+)?)\s*g""",
                                                                                RegexOption
                                                                                        .IGNORE_CASE
                                                                        ),
                                                                        Regex(
                                                                                """(\d+(?:\.\d+)?)\s*g\s+(?:of\s+)?carb""",
                                                                                RegexOption
                                                                                        .IGNORE_CASE
                                                                        )
                                                                )
                                                        )
                                                        .toDouble()
                                        ),
                                fat =
                                        round1(
                                                pickNum(
                                                                all,
                                                                listOf(
                                                                        Regex(
                                                                                """(?:total\s+)?fat[:\s]+(\d+(?:\.\d+)?)\s*g""",
                                                                                RegexOption
                                                                                        .IGNORE_CASE
                                                                        ),
                                                                        Regex(
                                                                                """(\d+(?:\.\d+)?)\s*g\s+(?:of\s+)?(?:total\s+)?fat""",
                                                                                RegexOption
                                                                                        .IGNORE_CASE
                                                                        )
                                                                )
                                                        )
                                                        .toDouble()
                                        ),
                                servingDescription = pickServing(all) ?: "serving"
                        )
                } catch (e: Exception) {
                        null
                }
        }

        private fun stripHtml(html: String): String =
                html.replace(Regex("<[^>]*>"), " ")
                        .replace("&amp;", "&")
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                        .replace("&quot;", "\"")
                        .replace("&#39;", "'")
                        .replace(Regex("&[a-z]+;"), " ")
                        .replace(Regex("\\s+"), " ")
                        .trim()

        private fun pickNum(text: String, patterns: List<Regex>): Int {
                for (p in patterns) {
                        val m = p.find(text)
                        m?.groupValues?.get(1)?.let { n ->
                                val v = n.toDoubleOrNull()
                                if (v != null && v > 0 && v < 10000) return v.toInt()
                        }
                }
                return 0
        }

        private fun pickServing(text: String): String? {
                val patterns =
                        listOf(
                                Regex(
                                        """per\s+([\d.]+\s*(?:g|oz|cup|tbsp|tsp|ml|piece|slice|medium|large|small)\w*)""",
                                        RegexOption.IGNORE_CASE
                                ),
                                Regex(
                                        """serving\s+size[:\s]+([\d.]+\s*(?:g|oz|cup|tbsp|tsp|ml)\w*)""",
                                        RegexOption.IGNORE_CASE
                                ),
                                Regex("""(100\s*g(?:ram)?s?)""", RegexOption.IGNORE_CASE),
                                Regex(
                                        """(1\s+(?:medium|large|small|cup|piece|slice))""",
                                        RegexOption.IGNORE_CASE
                                )
                        )
                for (p in patterns) {
                        p.find(text)?.groupValues?.get(1)?.let {
                                return it.trim()
                        }
                }
                return null
        }
}
