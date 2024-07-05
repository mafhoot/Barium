package com.example.baruim1

import android.Manifest.permission.*
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telephony.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import java.util.*

data class MessageStatus(
    val id: String,
    var status: String
)

class MainActivity : ComponentActivity() {
    private val REQUEST_SMS_PERMISSION = 1
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val handler = Handler(Looper.getMainLooper())
    private var signalThreshold by mutableStateOf(-100)
    private var checkInterval by mutableStateOf(20000L)
    private var destinationPhoneNumber by mutableStateOf("09335872053")
    private val telephonyManager by lazy { getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }

    var cellSignalStrength by mutableStateOf<Int?>(null)
    var cellTechnology by mutableStateOf<String?>(null)
    var locationString by mutableStateOf<String?>(null)
    var messageStatuses by mutableStateOf(listOf<MessageStatus>())
    var locationEnabled by mutableStateOf(false)

    private val locationSettingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        checkLocationEnabled()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestPermissions()

        registerReceiver(smsReceiver, IntentFilter("android.provider.Telephony.SMS_RECEIVED"))

        checkLocationEnabled()

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
                        if (!locationEnabled) {
                            LocationAlertDialog {
                                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                locationSettingsLauncher.launch(intent)
                            }
                        } else {
                            Content()
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun LocationAlertDialog(onEnableLocation: () -> Unit) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(text = "Location Required") },
            text = { Text(text = "This app requires location services to be enabled. Please turn on location services to continue.") },
            confirmButton = {
                Button(onClick = onEnableLocation) {
                    Text("Enable Location")
                }
            }
        )
    }

    @Composable
    fun Content() {
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

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(messageStatuses) { messageStatus ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = messageStatus.id)
                    Text(text = messageStatus.status)
                }
            }
        }
    }

    private fun checkLocationEnabled() {
        val locationMode = try {
            Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE)
        } catch (e: Settings.SettingNotFoundException) {
            Settings.Secure.LOCATION_MODE_OFF
        }

        locationEnabled = locationMode != Settings.Secure.LOCATION_MODE_OFF

        if (locationEnabled) {
            startCheckingCellInfo()
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
                // Add more types if needed
            }
        }

        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val locationTask: Task<Location> = fusedLocationClient.lastLocation
        locationTask.addOnSuccessListener { location: Location? ->
            location?.let {
                val lat = location.latitude
                val lon = location.longitude
                locationString = "Lat: $lat, Long: $lon"

                if (cellSignalStrength != null && cellTechnology != null) {
                    if (cellSignalStrength!! < signalThreshold) {
                        val messageId = UUID.randomUUID().toString()
                        sendSmsWithInfo(destinationPhoneNumber, "Signal Strength: $cellSignalStrength dBm, Technology: $cellTechnology", locationString!!, messageId)
                        messageStatuses = messageStatuses + MessageStatus(messageId, "Sent")
                    }
                }
            }
        }
    }

    private fun sendSmsWithInfo(phoneNumber: String, cellInfo: String, location: String, id: String) {
        val identityCode = "ps123wd"
        val message = "$identityCode\nid: $id\nCell Info: $cellInfo\nLocation: $location"
        val intent = Intent(this, SmsService::class.java).apply {
            putExtra("phone_number", phoneNumber)
            putExtra("message", message)
        }
        startService(intent)
    }

    private val smsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
                val bundle = intent.extras
                if (bundle != null) {
                    val pdus = bundle.get("pdus") as Array<*>
                    val messages: Array<SmsMessage?> = arrayOfNulls(pdus.size)
                    for (i in pdus.indices) {
                        messages[i] = SmsMessage.createFromPdu(pdus[i] as ByteArray)
                        val sender = messages[i]?.originatingAddress
                        val messageBody = messages[i]?.messageBody
                        if (sender != null && messageBody != null) {
                            if (messageBody.startsWith("Acknowledgment:")) {
                                val id = messageBody.substringAfter("id: ").trim()
                                messageStatuses = messageStatuses.map {
                                    if (it.id == id) it.copy(status = "Delivered") else it
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(smsReceiver)
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
