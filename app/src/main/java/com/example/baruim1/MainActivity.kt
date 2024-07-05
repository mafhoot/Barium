package com.example.baruim1

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.READ_SMS
import android.Manifest.permission.RECEIVE_SMS
import android.Manifest.permission.SEND_SMS
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.CellInfo
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoWcdma
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.baruim1.ui.theme.Baruim1Theme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task

class MainActivity : ComponentActivity() {
    private val REQUEST_SMS_PERMISSION = 1
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val handler = Handler(Looper.getMainLooper())
    private var signalThreshold by mutableStateOf(-100)
    private var checkInterval by mutableStateOf(5000L)
    private var destinationPhoneNumber by mutableStateOf("1234567890")
    private val telephonyManager by lazy { getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }

    var cellSignalStrength by mutableStateOf<Int?>(null)
    var cellTechnology by mutableStateOf<String?>(null)
    var locationString by mutableStateOf<String?>(null)

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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black, shape = RoundedCornerShape(8.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                cellTechnology?.let {
                                    Text(
                                        text = "Cell Technology: $it",
                                        color = Color.White,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    Divider(color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                cellSignalStrength?.let {
                                    Text(
                                        text = "Signal Strength: $it dBm",
                                        color = Color.White,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    Divider(color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                locationString?.let {
                                    Text(
                                        text = "Location: $it",
                                        color = Color.White,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        var thresholdInput by remember { mutableStateOf(signalThreshold.toString()) }
                        var phoneNumberInput by remember { mutableStateOf(destinationPhoneNumber) }
                        var intervalInput by remember { mutableStateOf(checkInterval.toString()) }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                OutlinedTextField(
                                    value = thresholdInput,
                                    onValueChange = { thresholdInput = it },
                                    label = { Text("Threshold (dBm)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = phoneNumberInput,
                                    onValueChange = { phoneNumberInput = it },
                                    label = { Text("Destination Phone Number") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = intervalInput,
                                    onValueChange = { intervalInput = it },
                                    label = { Text("Check Interval (ms)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        val newThreshold = thresholdInput.toIntOrNull()
                                        val newInterval = intervalInput.toLongOrNull()
                                        if (newThreshold != null && newInterval != null && phoneNumberInput.isNotBlank()) {
                                            signalThreshold = newThreshold
                                            checkInterval = newInterval
                                            destinationPhoneNumber = phoneNumberInput
                                            handler.removeCallbacks(checkCellInfoRunnable) // Reset the checking interval
                                            startCheckingCellInfo() // Start with the new interval
                                            Toast.makeText(this@MainActivity, "Settings updated", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(this@MainActivity, "Invalid input", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Submit")
                                }
                            }
                        }
                    }
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
            startCheckingCellInfo()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_SMS_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startCheckingCellInfo()
            } else {
                Toast.makeText(this, "Permission denied to send/receive SMS and access location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startCheckingCellInfo() {
        handler.post(checkCellInfoRunnable)
    }

    private val checkCellInfoRunnable = object : Runnable {
        override fun run() {
            getCellAndLocationInfo()
            handler.postDelayed(this, checkInterval)
        }
    }

    private fun getCellAndLocationInfo() {
        val cellInfo: List<CellInfo> = telephonyManager.allCellInfo

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

        if (cellSignalStrength != null) {
            if (cellSignalStrength!! < signalThreshold) {
                if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return
                }

                val locationTask: Task<Location> = fusedLocationClient.lastLocation
                locationTask.addOnSuccessListener { location ->
                    if (location != null) {
                        locationString = "Lat: ${location.latitude}, Long: ${location.longitude}"
                        val cellInfoString = "Signal Strength: $cellSignalStrength dBm, Technology: $cellTechnology"
                        sendSmsWithInfo(destinationPhoneNumber, cellInfoString, locationString!!)
                    } else {
                        Log.e("MainActivity", "Location is null")
                    }
                }
            }
        }
    }

    private fun sendSmsWithInfo(phoneNumber: String, cellInfo: String, location: String) {
        val identityCode = "ps123wd"
        val message = "$identityCode\nCell Info: $cellInfo\nLocation: $location"
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
