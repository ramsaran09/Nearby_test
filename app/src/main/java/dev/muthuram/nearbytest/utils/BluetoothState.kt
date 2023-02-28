package dev.muthuram.nearbytest.utils

import androidx.annotation.StringRes
import dev.muthuram.nearbytest.R

enum class BluetoothState(@StringRes val text: Int) {
    STATE_OFF(R.string.bluetooth_disabled),
    STATE_TURNING_OFF(R.string.turning_bluetooth_off),
    STATE_ON(R.string.bluetooth_enabled),
    STATE_TURNING_ON(R.string.turning_bluetooth_on)
}