package com.cren90.blessed.long_read

import android.os.Bundle
import android.view.WindowInsets.Side
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cren90.blessed.long_read.ble.BleAdvertiser
import com.cren90.blessed.long_read.ble.BleScanner
import com.cren90.blessed.long_read.ui.theme.MyApplicationTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.renfrowtech.kmp.log.LogLevel
import com.renfrowtech.kmp.log.Logger
import com.renfrowtech.kmp.log.strategy.LogcatStrategy
import com.welie.blessed.BluetoothCentralManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    var advertiser: BleAdvertiser? = null
    var scanner: BleScanner? = null
    var centralManager: BluetoothCentralManager? = null
    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Logger.addStrategies(LogcatStrategy(LogLevel.TRACE))

        setContent {
            val permissions = viewModel.permissionState

            MyApplicationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    if(permissions.allPermissionsGranted) {
                        LaunchedEffect("BLE") {
                            withContext(Dispatchers.IO) {
                                advertiser = BleAdvertiser(this@MainActivity)
                                val central = BluetoothCentralManager(this@MainActivity)
                                centralManager = central
                                scanner = BleScanner(central)
                                scanner?.addListener(viewModel)
                                advertiser?.startAdvertising(byteArrayOf())
                                scanner?.startScanning()
                            }
                        }
                    } else {
                        SideEffect {
                            cleanup()

                            viewModel.requestPermissions(permissions)
                        }
                    }
                }
            }
        }
    }
    override fun onStop() {
        super.onStop()
        cleanup()
    }

    fun cleanup() {
        CoroutineScope(Dispatchers.IO).launch {
            advertiser?.stopAdvertising()
            scanner?.removeListener(viewModel)
            scanner?.stopScanning()
            centralManager?.close()
            advertiser = null
            scanner = null
            centralManager = null
        }
    }

}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MyApplicationTheme {
        Greeting("Android")
    }
}