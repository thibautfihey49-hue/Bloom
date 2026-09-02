package com.bloom.parental

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class SplashActivity : AppCompatActivity() {
    private val PERMS = mutableListOf(
        Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS,
        Manifest.permission.ACCESS_FINE_LOCATION
    ).apply { if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS) }.toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        if (!PERMS.all { ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED })
            ActivityCompat.requestPermissions(this, PERMS, 100)

        findViewById<Button>(R.id.btn_parent).setOnClickListener {
            startActivity(Intent(this, ParentMainActivity::class.java)); finish()
        }
        findViewById<Button>(R.id.btn_enfant).setOnClickListener {
            startActivity(Intent(this, EnfantMainActivity::class.java)); finish()
        }
    }
}
