package com.cren90.blessed.long_read.ble

import android.bluetooth.le.ScanResult
import android.os.Handler
import android.os.HandlerThread
import com.renfrowtech.kmp.log.LoggerDelegate
import com.welie.blessed.BluetoothCentralManager
import com.welie.blessed.BluetoothPeripheral
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class BleScanner(private val centralManager: BluetoothCentralManager) {

    private val logger by LoggerDelegate()

    private var isScanning = false

    private val listeners = mutableSetOf<PeripheralFoundListener>()

    private val foundDevices = mutableSetOf<UUID>()

    private fun String.isServiceUUID(): Boolean {
        return this.equals(SERVICE_UUID.toString(), ignoreCase = true)
    }

    private val resultCallback = { bluetoothPeripheral: BluetoothPeripheral, scanResult: ScanResult ->
            if (bluetoothPeripheral.services.any { it.uuid.toString().isServiceUUID() } or
                (scanResult.scanRecord?.serviceUuids?.any {
                    it.uuid.toString().isServiceUUID()
                } == true)) {
                logger.withData(
                    "name" to bluetoothPeripheral.name,
                )
                    .debug("BLE device found")

                val peripheral = BlePeripheral(
                    bluetoothPeripheral,
                    centralManager,
                    scanResult
                )

                listeners.forEach {
                    it.onPeripheralFound(peripheral)
                }
            }
        }

    private val scanRestartMillis = 30000L

    private val looper = HandlerThread("RestartScan").apply {
        start()
    }.looper

    private val scanRestartHandler by lazy { Handler(looper) }

    private val restartRunnable: Runnable = Runnable {
        stopWithRestart()
    }

    fun addListener(listener: PeripheralFoundListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: PeripheralFoundListener) {
        listeners.remove(listener)
    }

    fun startScanning() {
        logger.info("Starting BLE scanning")
        if (!isScanning) {
            isScanning = true

            scan()
            scanRestartHandler.postDelayed(restartRunnable, scanRestartMillis)
        }
    }

    fun stopScanning() {
        logger.info("Stopping BLE scanning")

        isScanning = false
        scanRestartHandler.removeCallbacks(restartRunnable)
    }

    private fun scan() {
        centralManager.scanForPeripheralsWithServices(
            serviceUUIDs = arrayOf(SERVICE_UUID),
            resultCallback = resultCallback,
            scanError = {
                logger
                    .withData(
                        "scanFailure" to it
                    )
                    .error("Scan failed")
            }
        )
    }

    private fun stopWithRestart() {
        CoroutineScope(Dispatchers.IO).launch {
            centralManager.stopScan()

            delay(10000L)

            if (isScanning) {
                scan()
                scanRestartHandler.postDelayed(restartRunnable, scanRestartMillis)
            }
        }
    }
}