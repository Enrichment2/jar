package com.jar.app

import android.graphics.Color

object TagColors {
    val PRESET_COLORS = listOf(
        0xFFE57373.toInt(), // Red
        0xFFFF8A65.toInt(), // Deep Orange
        0xFFFFB74D.toInt(), // Orange
        0xFFFFD54F.toInt(), // Amber
        0xFFDCE775.toInt(), // Lime
        0xFF81C784.toInt(), // Green
        0xFF4DD0E1.toInt(), // Cyan
        0xFF64B5F6.toInt(), // Blue
        0xFF7986CB.toInt(), // Indigo
        0xFFBA68C8.toInt(), // Purple
        0xFFF06292.toInt(), // Pink
        0xFFA1887F.toInt()  // Brown
    )

    val COLOR_NAMES = listOf(
        "Red", "Deep Orange", "Orange", "Amber",
        "Lime", "Green", "Cyan", "Blue",
        "Indigo", "Purple", "Pink", "Brown"
    )

    fun getContrastColor(backgroundColor: Int): Int {
        val luminance = (0.299 * Color.red(backgroundColor) +
                0.587 * Color.green(backgroundColor) +
                0.114 * Color.blue(backgroundColor)) / 255
        return if (luminance > 0.5) Color.BLACK else Color.WHITE
    }

    fun getColorName(color: Int): String {
        val index = PRESET_COLORS.indexOf(color)
        return if (index >= 0) COLOR_NAMES[index] else "Custom"
    }
}
