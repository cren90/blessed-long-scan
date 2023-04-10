package com.cren90.blessed.long_read

import android.Manifest
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cren90.blessed.long_read.ble.BTPermissions
import com.cren90.blessed.long_read.ble.BlePeripheral
import com.cren90.blessed.long_read.ble.PeripheralFoundListener
import com.cren90.blessed.long_read.ble.getBTPermissions
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
class MainViewModel: ViewModel(), PeripheralFoundListener {
    val permissionState: MultiplePermissionsState
        @Composable
        get() = rememberMultiplePermissionsState(permissions = getBTPermissions().permissions)

    fun requestPermissions(permissions: MultiplePermissionsState) {
        permissions.launchMultiplePermissionRequest()
    }

    override fun onPeripheralFound(peripheral: BlePeripheral) {
        viewModelScope.launch(Dispatchers.IO) {
            peripheral.connect()
            peripheral.getDeviceInfo()
            peripheral.disconnect()
        }
    }
}