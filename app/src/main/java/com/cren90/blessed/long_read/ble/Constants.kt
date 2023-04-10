package com.cren90.blessed.long_read.ble

import java.util.UUID

const val SIG_SUFFIX: String = "-0000-1000-8000-00805f9b34fb"
const val PREFIX = "0000"
const val SERVICE = "${PREFIX}DEC3$SIG_SUFFIX"
val SERVICE_UUID = UUID.fromString(SERVICE)
const val CHARACTERISTIC = "${PREFIX}E8A2$SIG_SUFFIX"
val CHARACTERISTIC_UUID = UUID.fromString(CHARACTERISTIC)