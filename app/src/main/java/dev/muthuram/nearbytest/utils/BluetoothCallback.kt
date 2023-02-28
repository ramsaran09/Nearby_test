package dev.muthuram.nearbytest.utils

interface BluetoothCallback {
    fun unSupportedDevice()
    fun onBluetoothDisabled()
    fun onBlueToothStateChange(state: BluetoothState)
}