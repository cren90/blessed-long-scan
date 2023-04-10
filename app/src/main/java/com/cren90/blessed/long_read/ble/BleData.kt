package com.cren90.blessed.long_read.ble

import kotlinx.serialization.Serializable

@Serializable
data class BleData(
    val uuid: String,
    val seq: UByte,
    val priv: UByte,
    val name: String,
    val vers: String,
    val mfg: String,
    val model: String,
    val os: String,
    val osVers: String
)
