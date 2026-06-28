package com.customkeyboard.ui

import android.content.Intent
import android.provider.Settings
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnEnable: Button = findViewById(R.id.btn_enable)
        val btnSelect: Button = findViewById(R.id.btn_select)
        val tvStatus: TextView = findViewById(R.id.tv_status)

        btnEnable.setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        btnSelect.setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }

        updateStatus(tvStatus)
    }

    override fun onResume() {
        super.onResume()
        val tvStatus: TextView = findViewById(R.id.tv_status)
        updateStatus(tvStatus)
    }

    private fun updateStatus(tv: TextView) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val isEnabled = imm.enabledInputMethodList.any { it.packageName == packageName }
        tv.text = if (isEnabled) "✅ Enabled" else "❌ Not enabled"
    }
}
