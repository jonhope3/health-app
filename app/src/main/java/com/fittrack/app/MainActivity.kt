package com.fittrack.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fittrack.app.theme.FitTrackTheme
import com.fittrack.app.data.FoodRepository
import com.fittrack.app.data.DiaryItem
import com.fittrack.app.data.FoodItem
import com.fittrack.app.data.MealType
import com.fittrack.app.util.todayKey
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (intent.getBooleanExtra("POPULATE_TEST_DATA", false)) {
            populateTestData()
        }

        enableEdgeToEdge()
        setContent {
            FitTrackTheme {
                FitTrackApp()
            }
        }
    }

    private fun populateTestData() {
        val repo = FoodRepository(this)
        val today = todayKey()
        
        val manualItem = DiaryItem(
            id = UUID.randomUUID().toString(),
            name = "Protein Shake (Manual)",
            calories = 150,
            protein = 30f, carbs = 5f, fat = 2f, sugar = 0f,
            mealType = MealType.SNACK, timestamp = System.currentTimeMillis()
        )
        val aiItem = DiaryItem(
            id = UUID.randomUUID().toString(),
            name = "Scrambled Eggs (AI)",
            calories = 200,
            protein = 14f, carbs = 2f, fat = 15f, sugar = 1f,
            mealType = MealType.BREAKFAST, timestamp = System.currentTimeMillis() - 10000
        )
        val searchItem = DiaryItem(
            id = UUID.randomUUID().toString(),
            name = "Apple (Search)",
            calories = 95,
            protein = 0.5f, carbs = 25f, fat = 0.3f, sugar = 19f,
            mealType = MealType.SNACK, timestamp = System.currentTimeMillis() - 20000
        )

        repo.addDiaryItem(manualItem, today)
        repo.addDiaryItem(aiItem, today)
        repo.addDiaryItem(searchItem, today)
        
        repo.addOrUpdateFoodItem(FoodItem(searchItem.name, searchItem.calories, searchItem.protein, searchItem.carbs, searchItem.fat, searchItem.sugar, "1 large", 1, System.currentTimeMillis()))
        repo.addOrUpdateFoodItem(FoodItem(aiItem.name, aiItem.calories, aiItem.protein, aiItem.carbs, aiItem.fat, aiItem.sugar, "2 eggs", 1, System.currentTimeMillis()))
    }
}
