package dev.muthuram.nearbytest.utils

import android.R.attr.capitalize
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.widget.Toast
import androidx.annotation.StringRes


class BluetoothManager(
    private val activity: Activity,
    val bluetoothCallback: BluetoothCallback
) {

    private val bluetoothReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (BluetoothAdapter.ACTION_STATE_CHANGED == intent.action) {
                    val state = getStateFromAdapterState(
                        intent.getIntExtra(
                            BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR
                        )
                    )
                    bluetoothCallback.onBlueToothStateChange(state)
                }
            }
        }
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy { (activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter }

    init {
        activity.registerReceiver(
            bluetoothReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
        if (bluetoothAdapter == null) {
            bluetoothCallback.unSupportedDevice()
        }
        if (bluetoothAdapter?.isEnabled == false) {
            bluetoothCallback.onBluetoothDisabled()
        }
    }

    fun isMatchingRequirements(): Boolean {
        return bluetoothAdapter?.isEnabled ?: false
    }

    fun getStateFromAdapterState(state: Int): BluetoothState {
        return when (state) {
            BluetoothAdapter.STATE_OFF -> BluetoothState.STATE_OFF
            BluetoothAdapter.STATE_TURNING_OFF -> BluetoothState.STATE_TURNING_OFF
            BluetoothAdapter.STATE_ON -> BluetoothState.STATE_ON
            BluetoothAdapter.STATE_TURNING_ON -> BluetoothState.STATE_TURNING_ON
            else -> BluetoothState.STATE_OFF
        }
    }
}

fun Context.showToast(@StringRes messageId: Int, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, getString(messageId), length).show()
}

fun Context.showToast(message: String?, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, length).show()
}

fun getDeviceName(): String? {
    val manufacturer = Build.MANUFACTURER
    val model = Build.MODEL
    return if (model.startsWith(manufacturer)) {
        model
    } else "$manufacturer $model"

}