package com.example.baruim1

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast

class SmsService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        // We don't provide binding, so return null
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sender = intent?.getStringExtra("sender")
        val message = intent?.getStringExtra("message")
        val phoneNumber = intent?.getStringExtra("phone_number")

        if (sender != null && message != null) {
            handleReceivedSms(sender, message)
        } else if (phoneNumber != null && message != null) {
            sendSms(phoneNumber, message)
        } else {
            Toast.makeText(this, "Missing parameters", Toast.LENGTH_SHORT).show()
        }

        return START_STICKY
    }

    private fun handleReceivedSms(sender: String, message: String) {
        // Process the received SMS here
        Toast.makeText(this, "Received SMS from $sender: $message", Toast.LENGTH_SHORT).show()
        Log.d("SmsService", "Received SMS from $sender: $message")
        // Add your processing logic here
    }

    private fun sendSms(phoneNumber: String, message: String) {
        val sentPI: PendingIntent = PendingIntent.getBroadcast(this, 0, Intent("SMS_SENT"),
            PendingIntent.FLAG_IMMUTABLE)
        SmsManager.getDefault().sendTextMessage(phoneNumber, null, message, sentPI, null)
        Toast.makeText(this, "SMS sent to $phoneNumber: $message", Toast.LENGTH_SHORT).show()
        Log.d("SmsService", "SMS sent to $phoneNumber: $message")
    }
}
