package com.cren90.blessed.long_read.ble

import android.bluetooth.le.ScanResult
import com.renfrowtech.kmp.log.LoggerDelegate
import com.welie.blessed.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.*

data class BlePeripheral(
    private val peripheral: BluetoothPeripheral,
    private val centralManager: BluetoothCentralManager,
    private val scanResult: ScanResult
) {
    private val logger by LoggerDelegate()

    private val isConnected: Boolean
        get() = peripheral.getState() == ConnectionState.CONNECTED

    suspend fun connect(): Boolean {
        return try {
            centralManager.connectPeripheral(peripheral)

            peripheral.requestMtu(BluetoothPeripheral.MAX_MTU)
            true
        } catch (exception: ConnectionFailedException) {
            false
        }
    }

    private suspend fun read(service: UUID, characteristic: UUID): String? {
        return try {
            if (isConnected) {
                val readData = peripheral.readCharacteristic(
                    service,
                    characteristic
                ).asString()

                logger.withData(
                    "Characteristic" to mapOf(
                        "Uuid" to characteristic.toString(),
                        "Value" to readData
                    )
                ).debug("Characteristic read")

                readData
            } else {
                logger.warning("Attempt to read characteristic from disconnected peripheral")
                null
            }
        } catch (e: IllegalArgumentException) {
            logger
                .withException(e)
                .withData(
                    "ServiceUuid" to service.toString(),
                    "CharacteristicUuid" to characteristic.toString()
                )
                .error("Attempted to read an unreadable characteristic")
            null
        }
    }

    suspend fun disconnect() {
        centralManager.cancelConnection(peripheral)
    }

    suspend fun getDeviceInfo(): BleData? {
        return read(
            SERVICE_UUID,
            CHARACTERISTIC_UUID
        )?.let {
            try {
                Json.decodeFromString<BleData>(it)
            } catch (e: SerializationException) {
                logger
                    .withException(e)
                    .error("Failed to parse device info")
                null
            }
        }
    }
}
