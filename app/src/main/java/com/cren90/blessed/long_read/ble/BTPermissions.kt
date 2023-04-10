package com.cren90.blessed.long_read.ble

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Strategy interface for retrieving relavent BT permissions for the device
 */
sealed interface BTPermissions {
    val permissions: List<String>

    /**
     * Used for Pre-Android 5.1 since all permissions were install time
     */
    object Version22AndLower : BTPermissions {
        override val permissions: List<String> = listOf()
    }

    /**
     * Used for Android 6 - 10
     */
    object Version23Through29 : BTPermissions {
        override val permissions: List<String> = listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
    }

    /**
     * Used for Android 11
     */
    object Version30 : BTPermissions {
        override val permissions: List<String> = listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
    }


    /**
     * Used for Android 12+
     */
    @RequiresApi(Build.VERSION_CODES.S)
    object Version31AndHigher : BTPermissions {
        override val permissions: List<String> = listOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE
        )
    }
}

fun getBTPermissions(): BTPermissions {
    return when (Build.VERSION.SDK_INT) {
        in 0..Build.VERSION_CODES.LOLLIPOP_MR1 -> BTPermissions.Version22AndLower
        in Build.VERSION_CODES.M..Build.VERSION_CODES.Q -> BTPermissions.Version23Through29
        Build.VERSION_CODES.R -> BTPermissions.Version30
        else -> BTPermissions.Version31AndHigher
    }
}