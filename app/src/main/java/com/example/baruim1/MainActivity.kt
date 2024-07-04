package com.example.baruim1

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.util.Log

class MainActivity : AppCompatActivity() {

    private val SMS_PERMISSION_CODE = 1001
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "onCreate: App started")

        if (!hasSMSPermissions()) {
            Log.d(TAG, "onCreate: Requesting SMS permissions")
            requestSMSPermissions()
        } else {
            Log.d(TAG, "onCreate: SMS permissions already granted")
        }

        val sendButton: Button = findViewById(R.id.sendButton)
        val phoneNumberEditText: EditText = findViewById(R.id.phoneNumberEditText)
        val messageEditText: EditText = findViewById(R.id.messageEditText)

        sendButton.setOnClickListener {
            val phoneNumber = phoneNumberEditText.text.toString()
            val message = messageEditText.text.toString()
            Log.d(TAG, "Send button clicked with phoneNumber: $phoneNumber and message: $message")
            sendSMS(phoneNumber, message)
        }
    }

    private fun hasSMSPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestSMSPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS
        ), SMS_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Log.d(TAG, "SMS permissions granted")
            } else {
                Log.d(TAG, "SMS permissions denied")
            }
        }
    }

    private fun sendSMS(phoneNumber: String, message: String) {
        try {
            val smsService = SmsService()
            Log.d(TAG, "Attempting to send SMS")
            smsService.sendSMS(phoneNumber, message)
            Log.d(TAG, "SMS sent successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS", e)
        }
    }
}
