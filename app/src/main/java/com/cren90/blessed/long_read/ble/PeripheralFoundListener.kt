package com.cren90.blessed.long_read.ble

interface PeripheralFoundListener {
    fun onPeripheralFound(peripheral: BlePeripheral)
}