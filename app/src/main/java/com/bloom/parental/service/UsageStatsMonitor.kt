package com.bloom.parental.service

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.concurrent.TimeUnit

class UsageStatsMonitor(private val ctx: Context) {
    private fun hasPerm(): Boolean = try {
        val ops=ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(),ctx.packageName)==AppOpsManager.MODE_ALLOWED
    } catch(e:Exception){false}

    fun getTodayMinutes():Int=if(!hasPerm())0 else try{
        val usm=ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now=System.currentTimeMillis()
        val cal=java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY,0);cal.set(java.util.Calendar.MINUTE,0);cal.set(java.util.Calendar.SECOND,0)
        val start=cal.timeInMillis
        val stats=usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,start,now)
        stats.sumOf{it.totalTimeInForeground}.let{TimeUnit.MILLISECONDS.toMinutes(it).toInt()}
    }catch(e:Exception){0}

    fun getByApp():Map<String,Int>=if(!hasPerm())emptyMap() else try{
        val r=mutableMapOf<String,Int>()
        val usm=ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val cal=java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY,0);cal.set(java.util.Calendar.MINUTE,0)
        val stats=usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,cal.timeInMillis,System.currentTimeMillis())
        stats.forEach{r[it.packageName]=TimeUnit.MILLISECONDS.toMinutes(it.totalTimeInForeground).toInt()}
        r
    }catch(e:Exception){emptyMap()}
}
