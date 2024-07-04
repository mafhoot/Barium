package com.example.baruim1

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val bundle = intent.extras
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (message in messages) {
                val msgBody = message.messageBody
                val msgFrom = message.originatingAddress
                Log.d("SmsReceiver", "SMS received from $msgFrom: $msgBody")
            }
        }
    }
}
