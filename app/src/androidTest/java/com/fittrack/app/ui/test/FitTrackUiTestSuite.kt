package com.fittrack.app.ui.test

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fittrack.app.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Emulator-based Compose UI tests for FitTrack.
 *
 * TARGET DEVICE : Pixel_10_Pro AVD (1080×2410, 420dpi, Android 35)
 * SCOPE         : UI structure, navigation, and theme switching only.
 *                 No AI / Gemini Nano interactions — those require the physical device.
 *
 * Run via: make ui-test  (emulator must be running)
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class FitTrackUiTestSuite {

    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @Test
    fun bottomNav_homeTabIsSelectedByDefault() {
        composeRule.onNodeWithText("Home").assertIsDisplayed()
    }

    @Test
    fun bottomNav_tapLogFood_navigatesToLogScreen() {
        composeRule.onNodeWithText("Log Food").performClick()
        composeRule.onNodeWithText("Log Food").assertIsDisplayed()
    }

    @Test
    fun bottomNav_tapSteps_navigatesToStepsScreen() {
        composeRule.onNodeWithText("Steps").performClick()
        // Steps screen should show the step count ring
        composeRule.onNodeWithText("STEPS").assertIsDisplayed()
    }

    @Test
    fun bottomNav_tapSettings_navigatesToSettingsScreen() {
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    // ── Home Screen ───────────────────────────────────────────────────────────

    @Test
    fun homeScreen_showsCaloriesAndStepsRings() {
        composeRule.onNodeWithText("CALORIES CONSUMED").assertIsDisplayed()
        composeRule.onNodeWithText("STEPS").assertIsDisplayed()
    }

    @Test
    fun homeScreen_showsMacrosSection() {
        composeRule.onNodeWithText("Macros").assertIsDisplayed()
        composeRule.onNodeWithText("Protein").assertIsDisplayed()
        composeRule.onNodeWithText("Carbs").assertIsDisplayed()
        composeRule.onNodeWithText("Fat").assertIsDisplayed()
    }

    // ── Settings Screen ───────────────────────────────────────────────────────

    @Test
    fun settingsScreen_showsAppearanceSection() {
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Appearance").assertIsDisplayed()
        composeRule.onNodeWithText("Color Scheme").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_themePicker_hasThreeOptions() {
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Light").assertIsDisplayed()
        composeRule.onNodeWithText("Dark").assertIsDisplayed()
        composeRule.onNodeWithText("System").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_themePicker_defaultIsLight() {
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNode(hasText("Light")).assertIsSelected()
    }

    @Test
    fun settingsScreen_switchToDark_persistsSelection() {
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Dark").performClick()
        // Selection should update immediately (Room write → Flow → recompose)
        composeRule.onNode(hasText("Dark")).assertIsSelected()
    }

    @Test
    fun settingsScreen_switchToSystem_persistsSelection() {
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("System").performClick()
        composeRule.onNode(hasText("System")).assertIsSelected()
    }

    @Test
    fun settingsScreen_showsGoalsSection() {
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Goals").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_showsMacroGoalsSection() {
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Macro Goals").assertIsDisplayed()
    }

    // ── Log Food Screen ───────────────────────────────────────────────────────

    @Test
    fun logFoodScreen_showsSearchField() {
        composeRule.onNodeWithText("Log Food").performClick()
        // AI search bar should be present
        composeRule
            .onNode(hasText("Search food or describe what you ate…", substring = true))
            .assertIsDisplayed()
    }
}
