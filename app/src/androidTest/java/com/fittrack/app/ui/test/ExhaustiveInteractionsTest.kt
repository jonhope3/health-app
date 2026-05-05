package com.fittrack.app.ui.test

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fittrack.app.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exhaustive test suite that attempts to interact with every button and fill every form.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ExhaustiveInteractionsTest {

    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun exhaustiveNavigationAndInteraction() {
        // Wait for the UI to settle
        composeRule.waitForIdle()

        // ── HOME SCREEN ──
        // Click "Log Food" button on Home (using index 1 to avoid BottomNav clash if necessary, or just click the first)
        composeRule.onAllNodesWithText("Log Food")[0].performClick()
        composeRule.waitForIdle()

        // ── LOG FOOD SCREEN ──
        // Fill search field
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("Apple")
        composeRule.waitForIdle()
        
        // Switch to "AI" mode
        composeRule.onNodeWithText("AI").performClick()
        composeRule.waitForIdle()

        // Note: AI mode is unavailable on emulator so no text field is present.


        // Switch to "Manual" mode
        composeRule.onNodeWithText("Manual").performClick()
        composeRule.waitForIdle()

        // Fill all manual fields using index to guarantee we hit the text fields
        val textFields = composeRule.onAllNodes(hasSetTextAction())
        textFields[0].performTextInput("Banana") // Food name
        textFields[1].performTextInput("105")    // Cal
        textFields[2].performTextInput("1")      // Protein
        textFields[3].performTextInput("27")     // Carbs
        textFields[4].performTextInput("0")      // Fat
        textFields[5].performTextInput("14")     // Sugar
        composeRule.waitForIdle()

        // Save manual food
        composeRule.onNodeWithText("Add Entry").performClick()
        composeRule.waitForIdle()

        // Go back to Home using bottom nav (Log screen has Bottom Nav)
        composeRule.onAllNodesWithText("Home")[0].performClick()
        composeRule.waitForIdle()
        
        // ── SETTINGS SCREEN ──
        composeRule.onAllNodesWithText("Settings")[0].performClick()
        composeRule.waitForIdle()

        // Interact with Theme options
        composeRule.onNodeWithText("Dark").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Light").performClick()
        composeRule.waitForIdle()

        // Edit Profile
        composeRule.onAllNodesWithContentDescription("Edit")[0].performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodes(hasSetTextAction())[0].performTextClearance()
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("Tester")
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitForIdle()

        // Edit Daily Goals
        composeRule.onAllNodesWithContentDescription("Edit")[1].performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodes(hasSetTextAction())[0].performTextClearance()
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("2200")
        composeRule.onAllNodes(hasSetTextAction())[1].performTextClearance()
        composeRule.onAllNodes(hasSetTextAction())[1].performTextInput("12000")
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitForIdle()

        // Edit Body Measurements
        composeRule.onAllNodesWithContentDescription("Edit")[2].performClick()
        composeRule.waitForIdle()
        val bodyFields = composeRule.onAllNodes(hasSetTextAction())
        bodyFields[0].performTextClearance()
        bodyFields[0].performTextInput("175")
        bodyFields[1].performTextClearance()
        bodyFields[1].performTextInput("5")
        bodyFields[2].performTextClearance()
        bodyFields[2].performTextInput("10")
        bodyFields[3].performTextClearance()
        bodyFields[3].performTextInput("30")
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitForIdle()

        // Database Explorer
        composeRule.onNodeWithText("View DB").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Close").performClick()
        composeRule.waitForIdle()

        // Nuke Database
        composeRule.onNodeWithText("Reset & Start Fresh").performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Cancel")[0].performClick()
        composeRule.waitForIdle()

        // Back to Home
        composeRule.onNodeWithText("Home").performClick()
        composeRule.waitForIdle()
    }
}
