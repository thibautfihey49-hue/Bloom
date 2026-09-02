package com.bloom.parental

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        findViewById<Button>(R.id.btn_parent).setOnClickListener {
            startActivity(Intent(this, ParentMainActivity::class.java))
            finish()
        }

        findViewById<Button>(R.id.btn_enfant).setOnClickListener {
            startActivity(Intent(this, EnfantMainActivity::class.java))
            finish()
        }
    }
}
