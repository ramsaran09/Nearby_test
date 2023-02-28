package dev.muthuram.nearbytest

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import dev.muthuram.nearbytest.adapter.MessageAdapter
import dev.muthuram.nearbytest.databinding.ActivityMainBinding
import dev.muthuram.nearbytest.utils.BluetoothCallback
import dev.muthuram.nearbytest.utils.BluetoothManager
import dev.muthuram.nearbytest.utils.BluetoothState
import dev.muthuram.nearbytest.utils.showToast
import kotlin.random.Random


class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding
    private var bleState : BluetoothState? = null
    private var receivedData : String = ""
    private val endPoint : ArrayList<String> = arrayListOf()
    private val messages : ArrayList<String> = arrayListOf()
    private val messageAdapter by lazy {
        MessageAdapter(
            messages = arrayListOf()
        )
    }

    private val bluetoothManager by lazy {
        BluetoothManager(this, object : BluetoothCallback {
            override fun unSupportedDevice() {
                showToast(R.string.no_bluetooth_support)
            }

            override fun onBluetoothDisabled() {
                checkAndRequestBluetoothAndLocationPermission()
            }

            override fun onBlueToothStateChange(state: BluetoothState) {
                handleBluetoothState(state)
            }
        })
    }

    private val requestActivityResultLauncher by lazy {
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ::handleActivityResult
        )
    }

    private val requestPermissionLauncher by lazy {
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
            this::onPermissionResult
        )
    }

    private val connectionLifecycleCallback: ConnectionLifecycleCallback =
        object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
                showConfirmationDialog(endpointId, connectionInfo)
            }

            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                when (result.status.statusCode) {
                    ConnectionsStatusCodes.STATUS_OK -> {
                        val bytesPayload = Payload.fromBytes("Connection Initiated".toByteArray())
                        sendPayLoad(listOf(endpointId),bytesPayload)
                    }
                    ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {}
                    ConnectionsStatusCodes.STATUS_ERROR -> {}
                    else -> {}
                }
            }

            override fun onDisconnected(endpointId: String) {
                // We've been disconnected from this endpoint. No more data can be
                // sent or received.
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestActivityResultLauncher
        requestPermissionLauncher
        setUpAdapter()
        setUpListener()
        setUpBluetooth()
    }

    private fun setUpAdapter() {
        binding.uiRvMessage.adapter = messageAdapter
    }

    private fun setUpListener() {
        binding.uiBtAdvertise.setOnClickListener {
            startAdvertising()
            removeVisibility()
            showProgress()
        }
        binding.uiBtDiscover.setOnClickListener {
            startDiscovery()
            removeVisibility()
            showProgress()
        }
        binding.uiBtSend.setOnClickListener {
            actionSend()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showProgress() {
        binding.uiProgress.visibility = View.VISIBLE
        binding.uiTvLoading.apply {
            visibility = View.VISIBLE
            text = "Waiting for Other Device, Please Wait..."
        }
    }

    private fun hideProgress() {
        binding.uiProgress.visibility = View.GONE
        binding.uiTvLoading.apply {
            visibility = View.GONE
        }
    }

    private fun removeVisibility() {
        binding.uiBtAdvertise.visibility = View.INVISIBLE
        binding.uiBtDiscover.visibility = View.INVISIBLE
    }

    private fun showVisibility() {
        binding.uiEtMessage.visibility = View.VISIBLE
        binding.uiBtSend.visibility = View.VISIBLE
    }

    private fun actionSend() {
        val bytesPayload = Payload.fromBytes(binding.uiEtMessage.text.toString().toByteArray())
        sendPayLoad(endPoint,bytesPayload)
        binding.uiEtMessage.text?.clear()
    }

    private fun startDiscovery() {
        val random = Random(1000).nextInt()
        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        Nearby.getConnectionsClient(this).startDiscovery(SERVICE_ID, object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(
                endpointId: String,
                discoveredEndpointInfo: DiscoveredEndpointInfo
            ) {
                Nearby.getConnectionsClient(
                    this@MainActivity
                ).requestConnection("Muthuram${random}", endpointId, object : ConnectionLifecycleCallback() {
                    override fun onConnectionInitiated(
                        endpointId: String,
                        connectionInfo: ConnectionInfo
                    ) {
                        showConfirmationDialog(endpointId, connectionInfo)
                    }

                    override fun onConnectionResult(
                        s: String,
                        connectionResolution: ConnectionResolution
                    ) {
                        when (connectionResolution.status.statusCode) {
                            ConnectionsStatusCodes.STATUS_OK -> {
                                val bytesPayload = Payload.fromBytes("Connection Initiated".toByteArray())
                                sendPayLoad(listOf(endpointId),bytesPayload)
                            }
                            ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {}
                            ConnectionsStatusCodes.STATUS_ERROR -> {}
                            else -> {}
                        }
                    }

                    override fun onDisconnected(s: String) {}
                })
            }

            override fun onEndpointLost(s: String) {
                // disconnected
            }
        }, discoveryOptions)
    }

    private fun startAdvertising() {
        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        Nearby.getConnectionsClient(this)
            .startAdvertising(
                "Saran", SERVICE_ID, connectionLifecycleCallback, advertisingOptions
            )
            .addOnSuccessListener { unused: Void? ->
                Log.d("$this", "startAdvertising: $unused")
            }
            .addOnFailureListener{ e: Exception? ->
                Log.d("$this", "startAdvertising: $e")
            }
    }

    private fun sendPayLoad(endPointId: List<String>,bytesPayload: Payload) {
        Nearby.getConnectionsClient(this)
            .sendPayload(endPointId, bytesPayload)
            .addOnSuccessListener { }
            .addOnFailureListener { }
    }

    private fun showConfirmationDialog(endpointId: String, info: ConnectionInfo) {
        AlertDialog.Builder(this)
            .setTitle("Accept connection to " + info.endpointName)
            .setMessage("Confirm the code matches on both devices: " + info.authenticationDigits)
            .setPositiveButton("Accept") { _, _ ->  // The user confirmed, so we can accept the connection.
                endPoint.add(endpointId)
                Nearby.getConnectionsClient(this)
                    .acceptConnection(endpointId, object : PayloadCallback() {
                        override fun onPayloadReceived(s: String, payload: Payload) {
                            Log.d("${this@MainActivity}", "onPayloadReceived: $s")
                            Log.d("${this@MainActivity}", "onPayloadReceived: $payload")
                            val receivedBytes = payload.asBytes()
                            runOnUiThread {
                                val data = receivedBytes?.let { String(it) }
                                receivedData = data ?: ""
                                Log.d("${this@MainActivity}", "receivedData: $s")
                                if (receivedData.isNotEmpty()) {
                                    if (endPoint.size > 1) {
                                        sendPayLoadToMultipleEndPoint(endpointId,payload)
                                    }
                                    if (data != null) {
                                        messages.add(data)
                                        messageAdapter.updateList(messages)
                                    }
                                }else binding.uiTvReceivedText.text = receivedData
                            }
                        }

                        override fun onPayloadTransferUpdate(
                            s: String,
                            payloadTransferUpdate: PayloadTransferUpdate
                        ) {
                            if (payloadTransferUpdate.status == PayloadTransferUpdate.Status.SUCCESS) {
                                // Do something with is here...
                                Log.d("${this@MainActivity}", "onPayloadTransferUpdate: $s")
                            }
                        }
                    }
                )
                showVisibility()
                hideProgress()
            }
            .setNegativeButton(
                android.R.string.cancel
            ) { _, _ ->  // The user canceled, so we should reject the connection.
                Nearby.getConnectionsClient(this).rejectConnection(endpointId)
            }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun sendPayLoadToMultipleEndPoint(endpointId: String,bytesPayload: Payload) {
        val filteredEndPoint = endPoint.filter { it != endpointId }
        sendPayLoad(filteredEndPoint,bytesPayload)
    }

    private fun setUpBluetooth() {
        if (bluetoothManager.isMatchingRequirements()) {
            bleState = BluetoothState.STATE_ON
            checkAndRequestBluetoothAndLocationPermission()
        }
    }

    private fun handleBluetoothState(state: BluetoothState) {
        when (state) {
            BluetoothState.STATE_OFF -> {
                showToast(R.string.turn_on_bluetooth_message)
            }
            BluetoothState.STATE_TURNING_OFF -> Log.d("$this", "bluetooth turning off")
            BluetoothState.STATE_ON -> {
                bleState = BluetoothState.STATE_ON
                checkAndRequestBluetoothAndLocationPermission()
            }
            BluetoothState.STATE_TURNING_ON -> Log.d("$this", "bluetooth turning on")
        }
    }

    private fun checkAndRequestBluetoothAndLocationPermission() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            checkAndRequestPermissionAndroidOsLowerThan12()
        }else checkAndRequestPermissionAndroidOs12()
    }

    private fun checkAndRequestPermissionAndroidOsLowerThan12() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADMIN
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onPermissionResult(mapOf("" to true))
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                )
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun checkAndRequestPermissionAndroidOs12() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADMIN
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onPermissionResult(mapOf("" to true))
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                )
            )
        }
    }

    private fun onPermissionResult(result : Map<String,Boolean>) {
        result.onEach { (_, result) ->
            if (!result) {
                return
            }
        }
        turnOnBluetooth()
    }

    private fun handleActivityResult(activityResult : ActivityResult) {
        Log.d("$this", "handleActivityResult: $activityResult")
        //start advertising
    }

    private fun turnOnBluetooth() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (bleState == BluetoothState.STATE_ON) {
                //start advertising
            }else  {
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                intent.putExtra(
                    KEY_BLUETOOTH_EVENT,
                    REQUEST_ENABLE_BT
                )
                requestActivityResultLauncher.launch(intent)
            }
        }
    }

    companion object {
        const val KEY_BLUETOOTH_EVENT = "key.Bluetooth.event"
        const val SERVICE_ID = "dev.muthuram.nearbytest"
        const val REQUEST_ENABLE_BT = 2020
    }
}