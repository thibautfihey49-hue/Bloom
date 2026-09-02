package com.bloom.parental.data

data class ChildProfile(
    val id: String, val name: String, var dailyLimitMinutes: Int,
    var todayUsedMinutes: Int = 0, var batteryLevel: Int = 100,
    var latitude: Double = 0.0, var longitude: Double = 0.0,
    var isPaused: Boolean = false
) {
    fun usagePercent(): Float = if(dailyLimitMinutes>0) todayUsedMinutes.toFloat()/dailyLimitMinutes else 0f
    fun remainingMinutes(): Int = (dailyLimitMinutes - todayUsedMinutes).coerceAtLeast(0)
}
