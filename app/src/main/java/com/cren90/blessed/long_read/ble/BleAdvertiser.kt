package com.cren90.blessed.long_read.ble

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.PERMISSION_READ
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertiseSettings.ADVERTISE_MODE_LOW_POWER
import android.bluetooth.le.AdvertiseSettings.ADVERTISE_TX_POWER_HIGH
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import com.renfrowtech.kmp.log.LoggerDelegate
import com.welie.blessed.AdvertiseError
import com.welie.blessed.BluetoothCentral
import com.welie.blessed.BluetoothPeripheralManager
import com.welie.blessed.BluetoothPeripheralManagerCallback
import com.welie.blessed.GattStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BleAdvertiser(
    context: Context
) {

    private val logger by LoggerDelegate()

    private val timeoutDisabled = 0

    private val bluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    /**
     * Indicates that BLE Advertising has started
     */
    var isAdvertising: Boolean = false
        private set

    /**
     * Indicates that a request to start BLE Advertising is pending, but scanning has not been
     * confirmed to have started.
     */
    var isAdvertisingPending: Boolean = false
        private set

    private var startAdvertisingContinuations: MutableSet<Continuation<Boolean>> = mutableSetOf()
    private var stopAdvertisingContinuations: MutableSet<Continuation<Boolean>> = mutableSetOf()

    private val callback = object : BluetoothPeripheralManagerCallback() {
        override fun onServiceAdded(status: GattStatus, service: BluetoothGattService) {
        }

        //{"uuid":"9124914a-be5b
        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(
            bluetoothCentral: BluetoothCentral,
            characteristic: BluetoothGattCharacteristic
        ) {
            runBlocking(Dispatchers.IO) {
                val data = when (characteristic.uuid) {
                    CHARACTERISTIC_UUID -> Json.encodeToString(
                        BleData(
                            uuid = UUID.randomUUID().toString(),
                            seq = 1u,
                            priv = 1u,
                            name = "Device Name",
                            vers = "BuildConfig.VERSION_NAME",
                            mfg = Build.MANUFACTURER,
                            model = "${Build.DEVICE} - ${Build.MODEL}",
                            os = "Android",
                            osVers = Build.VERSION.RELEASE,
                        )
                    )
                    else -> null
                }

                logger.withData(
                    "Characteristic" to mapOf(
                        "UUID" to characteristic.uuid.toString(),
                        "Value" to data
                    ),
                    "Central" to mapOf(
                        "Address" to bluetoothCentral.address,
                        "Name" to bluetoothCentral.getName()
                    )
                )
                    .info("Characteristic read requested")

                characteristic.setValue(data)
            }
        }

        override fun onAdvertisingStarted(settingsInEffect: AdvertiseSettings) {
            logger.info("onAdvertisingStarted")
            isAdvertising = true
            isAdvertisingPending = false
            startAdvertisingContinuations.forEach { it.resume(isAdvertising) }
            startAdvertisingContinuations.clear()
        }

        override fun onAdvertiseFailure(advertiseError: AdvertiseError) {
            logger.info("onAdvertiseFailure")
            isAdvertising = false
            isAdvertisingPending = false
            startAdvertisingContinuations.forEach { it.resume(isAdvertising) }
            startAdvertisingContinuations.clear()
        }

        override fun onAdvertisingStopped() {
            logger.info("onAdvertisingStopped")
            isAdvertising = false
            isAdvertisingPending = false
            stopAdvertisingContinuations.forEach { it.resume(true) }
            stopAdvertisingContinuations.clear()
        }
    }

    private val deviceInfoCharacteristic =
        BluetoothGattCharacteristic(
            CHARACTERISTIC_UUID,
            PROPERTY_READ,
            PERMISSION_READ
        )

    private val service =
        BluetoothGattService(
            SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        ).apply {
            addCharacteristic(deviceInfoCharacteristic)
        }

    private val peripheralManager by lazy {
        BluetoothPeripheralManager(
            context,
            bluetoothManager,
            callback
        ).apply {
            add(service)
        }
    }

    private val advertiseSettings: AdvertiseSettings = AdvertiseSettings.Builder()
        .setAdvertiseMode(ADVERTISE_MODE_LOW_POWER)
        .setConnectable(true)
        .setTimeout(timeoutDisabled)
        .setTxPowerLevel(ADVERTISE_TX_POWER_HIGH)
        .build()


    suspend fun startAdvertising(advertisingPayload: ByteArray) = suspendCoroutine { continuation ->
        logger.info("Starting BLE advertising")

        startAdvertisingContinuations.add(continuation)
        when {
            !isAdvertisingPending -> {
                isAdvertisingPending = true

                val advertiseData = AdvertiseData.Builder()
                    .addServiceUuid(ParcelUuid(SERVICE_UUID))
                    .build()

                val scanResponse = AdvertiseData.Builder()
                    .setIncludeDeviceName(true)
                    .setIncludeTxPowerLevel(true)
                    .build()

                peripheralManager.startAdvertising(advertiseSettings, advertiseData, scanResponse)
            }

            isAdvertising -> {
                startAdvertisingContinuations.forEach { it.resume(true) }
                startAdvertisingContinuations.clear()
            }
        }
    }

    suspend fun stopAdvertising() = suspendCoroutine { continuation ->
        logger.info("Stopping BLE advertising")

        stopAdvertisingContinuations.add(continuation)
        peripheralManager.stopAdvertising()
    }
}