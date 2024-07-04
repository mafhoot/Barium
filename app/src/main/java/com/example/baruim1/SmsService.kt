package com.example.baruim1

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.Toast

class SmsService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        // We don't provide binding, so return null
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sender = intent?.getStringExtra("sender")
        val message = intent?.getStringExtra("message")

        if (sender != null && message != null) {
            handleReceivedSms(sender, message)
        } else {
            Toast.makeText(this, "Sender or message is missing", Toast.LENGTH_SHORT).show()
        }

        // If we get killed, after returning from here, restart
        return START_STICKY
    }

    private fun handleReceivedSms(sender: String, message: String) {
        // Process the received SMS here
        Toast.makeText(this, "Received SMS from $sender: $message", Toast.LENGTH_SHORT).show()
        // Add your processing logic here
    }
}
