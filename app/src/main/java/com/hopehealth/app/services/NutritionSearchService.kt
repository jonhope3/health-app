package com.hopehealth.app.services

import com.hopehealth.app.data.NutritionResult
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
        private val geminiNanoService: com.hopehealth.app.services.GeminiNanoService,
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

        internal suspend fun searchDuckDuckGo(
                food: String,
                onStatus: (String) -> Unit = {}
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
                                sugar =
                                        round1(
                                                pickNum(
                                                                all,
                                                                listOf(
                                                                        Regex(
                                                                                """(?:total\s+)?sugars?[:\s]+(\d+(?:\.\d+)?)\s*g""",
                                                                                RegexOption
                                                                                        .IGNORE_CASE
                                                                        ),
                                                                        Regex(
                                                                                """(\d+(?:\.\d+)?)\s*g\s+(?:of\s+)?sugar""",
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
        private fun round1(n: Double): Float = (kotlin.math.round(n * 10) / 10.0).toFloat()
}
