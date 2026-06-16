package com.runninghub.app.util

import kotlin.math.abs
import kotlin.math.roundToInt

fun formatOneDecimal(value: Double): String {
    val scaled = (value * 10).roundToInt()
    return "${scaled / 10}.${abs(scaled % 10)}"
}

fun formatMinutesSeconds(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
