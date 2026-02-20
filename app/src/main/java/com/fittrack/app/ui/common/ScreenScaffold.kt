package com.fittrack.app.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Standard screen wrapper used by every top-level screen.
 *
 * Provides consistent:
 * - Full-size background fill
 * - Vertical scroll
 * - Content padding: 16dp top / 16dp horizontal / 24dp bottom
 *
 * Usage:
 * ```kotlin
 * ScreenScaffold {
 *     Text("Hello")
 * }
 * ```
 */
@Composable
fun ScreenScaffold(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 24.dp),
        content = content
    )
}
