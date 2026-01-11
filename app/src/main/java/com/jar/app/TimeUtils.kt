package com.jar.app

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object TimeUtils {
    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            diff < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                "$minutes min ago"
            }
            diff < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                "$hours hr ago"
            }
            diff < TimeUnit.DAYS.toMillis(2) -> "Yesterday"
            diff < TimeUnit.DAYS.toMillis(7) -> {
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                "$days days ago"
            }
            else -> {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = timestamp
                val thisYear = Calendar.getInstance().get(Calendar.YEAR)
                val noteYear = calendar.get(Calendar.YEAR)

                if (noteYear == thisYear) {
                    SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
                } else {
                    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
                }
            }
        }
    }

    fun formatFullDate(timestamp: Long): String {
        return SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
            .format(Date(timestamp))
    }
}
