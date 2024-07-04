package com.example.baruim1

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log

class SmsService : Service() {

    private val TAG = "SmsService"

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    fun sendSMS(phoneNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.d(TAG, "SMS sent to $phoneNumber: $message")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS", e)
        }
    }
}

