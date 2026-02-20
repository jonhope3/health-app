package com.fittrack.app.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.time.LocalDate

fun todayKey(): String = LocalDate.now().toString()

fun formatTime(timestamp: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))

fun getGreeting(nickname: String = ""): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val timeOfDay = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
    return if (nickname.isNotBlank()) "$timeOfDay, $nickname" else timeOfDay
}

private val commaFormat = NumberFormat.getIntegerInstance(Locale.US)

fun fmtNum(n: Int): String = commaFormat.format(n)
