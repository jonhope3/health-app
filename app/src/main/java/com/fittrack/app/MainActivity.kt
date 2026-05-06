package com.fittrack.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.fittrack.app.data.DiaryItem
import com.fittrack.app.data.FoodItem
import com.fittrack.app.data.FoodRepository
import com.fittrack.app.data.MealType
import com.fittrack.app.theme.FitTrackTheme
import com.fittrack.app.util.todayKey
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var foodRepository: FoodRepository
    @Inject lateinit var goalsRepository: com.fittrack.app.data.GoalsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (intent.getBooleanExtra("POPULATE_TEST_DATA", false)) {
            lifecycleScope.launch(Dispatchers.IO) { populateTestData() }
        }

        setContent {
            FitTrackTheme(darkTheme = false) {
                FitTrackApp(goalsRepository)
            }
        }
    }

    /** Seeds the database with representative entries for integration testing via ADB intent. */
    private suspend fun populateTestData() {
        val today = todayKey()
        val now = System.currentTimeMillis()

        val entries = listOf(
            DiaryItem(
                id = UUID.randomUUID().toString(),
                name = "Protein Shake (Manual)",
                calories = 150,
                protein = 30f, carbs = 5f, fat = 2f, sugar = 0f,
                mealType = MealType.SNACK,
                timestamp = now,
            ),
            DiaryItem(
                id = UUID.randomUUID().toString(),
                name = "Scrambled Eggs (AI)",
                calories = 200,
                protein = 14f, carbs = 2f, fat = 15f, sugar = 1f,
                mealType = MealType.BREAKFAST,
                timestamp = now - 10_000,
            ),
            DiaryItem(
                id = UUID.randomUUID().toString(),
                name = "Apple (Search)",
                calories = 95,
                protein = 0.5f, carbs = 25f, fat = 0.3f, sugar = 19f,
                mealType = MealType.SNACK,
                timestamp = now - 20_000,
            ),
        )

        entries.forEach { foodRepository.addDiaryItem(it, today) }
        foodRepository.addOrUpdateFoodItem(
            FoodItem("Apple (Search)", 95, 0.5f, 25f, 0.3f, 19f, "1 large", 1, now)
        )
        foodRepository.addOrUpdateFoodItem(
            FoodItem("Scrambled Eggs (AI)", 200, 14f, 2f, 15f, 1f, "2 eggs", 1, now)
        )
    }
}
