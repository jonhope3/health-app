package com.fittrack.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fittrack.app.data.NutritionResult
import com.fittrack.app.services.GeminiNanoService
import com.fittrack.app.services.NutritionSearchService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SearchTestRunner() {
    val searchService = remember { NutritionSearchService(GeminiNanoService()) }
    val scope = rememberCoroutineScope()
    var isRunning by remember { mutableStateOf(false) }
    var currentQuery by remember { mutableStateOf("") }
    var logEntries by remember { mutableStateOf(listOf<String>()) }

    val queries = listOf("1 Apple", "2 scrambled eggs", "a handful of almonds", "McDonalds Big Mac", "Croissants apple")

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF0F4F8)).padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)) {
        Text("Nutrition Search Evaluation", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

    LaunchedEffect(Unit) {
        if (isRunning) return@LaunchedEffect
        isRunning = true
        logEntries = listOf("Starting test...")
        for (q in queries) {
            currentQuery = q
            logEntries = logEntries + "Evaluating '$q'..."
            
            android.util.Log.e("SEARCH_EVAL", "=== Testing Query: $q ===")
            
            // New Pipeline (DuckDuckGo + AI)
            logEntries = logEntries + "  [Pipeline] Searching..."
            val result = searchService.searchNutrition(q)
            val resStr = "  [Result] ${result?.name ?: "NOT FOUND"} (${result?.calories ?: 0} cal)"
            logEntries = logEntries + resStr
            android.util.Log.e("SEARCH_EVAL", resStr)
            
            logEntries = logEntries + "---"
            delay(2000) // Delay for visualization/screenshot
        }
        isRunning = false
        logEntries = logEntries + "Test Complete!"
    }

        Spacer(Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(logEntries) { entry ->
                Text(entry, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 4.dp))
                Divider()
            }
        }
    }
}
