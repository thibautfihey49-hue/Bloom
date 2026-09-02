package com.bloom.parental

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.WindowManager
import android.view.WindowManager.LayoutParams

class ScreenLockReceiver : BroadcastReceiver() {
    companion object {
        private var verrouillageActif = false
        private var lockOverlay: LockOverlay? = null
        
        fun verrouiller(context: Context) {
            verrouillageActif = true
            if (lockOverlay == null) {
                lockOverlay = LockOverlay(context)
            }
            lockOverlay!!.afficher()
            val intent = Intent("com.bloom.parental.ETAT_VERROUILLE")
            intent.setPackage(context.packageName)
            context.sendBroadcast(intent)
        }
        
        fun deverrouiller(context: Context) {
            verrouillageActif = false
            lockOverlay?.cacher()
            lockOverlay = null
            val intent = Intent("com.bloom.parental.ETAT_DEVERROUILLE")
            intent.setPackage(context.packageName)
            context.sendBroadcast(intent)
        }
        
        fun estVerrouille(): Boolean = verrouillageActif
    }
    
    override fun onReceive(context: Context, intent: Intent) {}
    
    // ✅ Overlay plein écran pour verrouiller
    class LockOverlay(private val context: Context) {
        private var windowManager: WindowManager? = null
        private var vue: android.view.View? = null
        
        fun afficher() {
            if (vue != null) return
            
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            
            val params = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.TYPE_APPLICATION_OVERLAY,
                    LayoutParams.FLAG_NOT_FOCUSABLE or 
                    LayoutParams.FLAG_NOT_TOUCH_MODAL or 
                    LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    -0x2
                )
            } else {
                @Suppress("DEPRECATION")
                LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.TYPE_PHONE,
                    LayoutParams.FLAG_NOT_FOCUSABLE or 
                    LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    -0x2
                )
            }
            
            vue = android.view.View(context).apply {
                setBackgroundColor(0xFF121212.toInt())
            }
            
            windowManager?.addView(vue, params)
        }
        
        fun cacher() {
            vue?.let { windowManager?.removeView(it) }
            vue = null
        }
    }
}
