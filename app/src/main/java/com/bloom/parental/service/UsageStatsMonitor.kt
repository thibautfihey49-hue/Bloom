package com.bloom.parental.service
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.util.Calendar
class UsageStatsMonitor(private val ctx: Context) {
    private val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    fun hasPermission(): Boolean = try {
        val ops = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName) == AppOpsManager.MODE_ALLOWED
    } catch(e: Exception) { false }
    fun getTodayMinutes(): Int {
        if (!hasPermission()) return 0
        return try {
            val cal = Calendar.getInstance()
            val endTime = cal.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
            val startTime = cal.timeInMillis
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
            stats?.sumOf { (it.totalTimeInForeground / 60000).toInt() } ?: 0
        } catch(e: Exception) { 0 }
    }
    fun getByApp(): Map<String, Int> {
        if (!hasPermission()) return emptyMap()
        return try {
            val cal = Calendar.getInstance()
            val endTime = cal.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
            val startTime = cal.timeInMillis
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
            stats?.associate { it.packageName to (it.totalTimeInForeground / 60000).toInt() } ?: emptyMap()
        } catch(e: Exception) { emptyMap() }
    }
}
