package com.bloom.parental.data
data class ChildProfile(
    val id: String, val name: String,
    var dailyLimitMinutes: Int = 240,
    var todayUsedMinutes: Int = 0,
    var isPaused: Boolean = false,
    var batteryLevel: Int = 100,
    var latitude: Double = 0.0,
    var longitude: Double = 0.0
) {
    fun remainingMinutes(): Int = (dailyLimitMinutes - todayUsedMinutes).coerceAtLeast(0)
    fun usagePercent(): Float = if (dailyLimitMinutes > 0) todayUsedMinutes.toFloat() / dailyLimitMinutes.toFloat() else 0f
}
