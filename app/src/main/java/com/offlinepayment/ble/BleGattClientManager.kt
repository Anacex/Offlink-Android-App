package com.offlinepayment.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Sender side: scan for Offlink service, connect, subscribe to ack notifies, write SENDER_OK.
 */
class BleGattClientManager(
    private val context: Context,
    private val mainHandler: Handler = Handler(Looper.getMainLooper()),
) {

    private val bluetoothManager =
        context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter = bluetoothManager.adapter

    private var scanner: BluetoothLeScanner? = null
    private var gatt: BluetoothGatt? = null

    @Volatile
    private var ackCharacteristic: BluetoothGattCharacteristic? = null

    @Volatile
    private var cmdCharacteristic: BluetoothGattCharacteristic? = null

    @Volatile
    var negotiatedMtu: Int = 23
        private set

    private val reassemblyBuffers = ConcurrentHashMap<String, Reassembly>()

    var onReceiverAckPayload: ((ByteArray) -> Unit)? = null
    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null

    private data class Reassembly(
        val total: Int,
        val parts: Array<ByteArray?> = arrayOfNulls(256),
        var received: Int = 0,
    )

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result ?: return
            val uuids = result.scanRecord?.serviceUuids
            val match = uuids?.any { it?.uuid == BleOfflinkContract.SERVICE_UUID } == true
            if (match) {
                mainHandler.post { onScanResult?.invoke(result) }
            }
        }
    }

    var onScanResult: ((ScanResult) -> Unit)? = null

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                gatt.requestMtu(BleOfflinkContract.REQUEST_MTU)
                gatt.discoverServices()
                mainHandler.post { onConnected?.invoke() }
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                mainHandler.post { onDisconnected?.invoke() }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            negotiatedMtu = mtu
            Log.d(TAG, "Client MTU=$mtu status=$status")
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return
            val service = gatt.getService(BleOfflinkContract.SERVICE_UUID) ?: return
            ackCharacteristic = service.getCharacteristic(BleOfflinkContract.CHAR_ACK_NOTIFY)
            cmdCharacteristic = service.getCharacteristic(BleOfflinkContract.CHAR_CMD_WRITE)
            val ack = ackCharacteristic ?: return
            gatt.setCharacteristicNotification(ack, true)
            val cccd = ack.getDescriptor(BleOfflinkContract.CCCD_UUID)
            if (cccd != null) {
                cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(cccd)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            if (characteristic.uuid != BleOfflinkContract.CHAR_ACK_NOTIFY) return
            handleAckFragment(value)
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val value = characteristic.value ?: return
            if (characteristic.uuid != BleOfflinkContract.CHAR_ACK_NOTIFY) return
            handleAckFragment(value)
        }

        private fun handleAckFragment(packet: ByteArray) {
            if (packet.size < 2) return
            val total = packet[0].toInt() and 0xff
            val index = packet[1].toInt() and 0xff
            val body = packet.copyOfRange(2, packet.size)
            if (total <= 0 || index >= total) return

            val key = gatt?.device?.address ?: "default"
            if (total == 1) {
                mainHandler.post { onReceiverAckPayload?.invoke(body) }
                return
            }
            val state = reassemblyBuffers.getOrPut(key) {
                Reassembly(total, arrayOfNulls(total))
            }
            if (state.total != total) {
                reassemblyBuffers[key] = Reassembly(total, arrayOfNulls(total)).also {
                    it.parts[index] = body
                    it.received = 1
                }
                return
            }
            if (state.parts[index] == null) {
                state.parts[index] = body
                state.received++
            }
            if (state.received >= total) {
                var full = ByteArray(0)
                for (i in 0 until total) {
                    val p = state.parts[i] ?: continue
                    full += p
                }
                reassemblyBuffers.remove(key)
                mainHandler.post { onReceiverAckPayload?.invoke(full) }
            }
        }
    }

    fun isBluetoothAvailable(): Boolean = adapter?.isEnabled == true

    fun startScan() {
        stopScan()
        val ad = adapter ?: return
        scanner = ad.bluetoothLeScanner ?: return
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BleOfflinkContract.SERVICE_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner?.startScan(listOf(filter), settings, scanCallback)
    }

    fun stopScan() {
        runCatching { scanner?.stopScan(scanCallback) }
        scanner = null
    }

    fun connect(device: BluetoothDevice) {
        stopScan()
        gatt?.close()
        gatt = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            device.connectGatt(context.applicationContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            @Suppress("DEPRECATION")
            device.connectGatt(context.applicationContext, false, gattCallback)
        }
    }

    fun writeSenderOk(payload: ByteArray): Boolean {
        val c = cmdCharacteristic ?: return false
        val g = gatt ?: return false
        c.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            g.writeCharacteristic(c, payload, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) ==
                BluetoothGatt.GATT_SUCCESS
        } else {
            @Suppress("DEPRECATION")
            c.value = payload
            @Suppress("DEPRECATION")
            g.writeCharacteristic(c)
        }
    }

    fun disconnect() {
        runCatching { gatt?.disconnect() }
        runCatching { gatt?.close() }
        gatt = null
        ackCharacteristic = null
        cmdCharacteristic = null
        reassemblyBuffers.clear()
        negotiatedMtu = 23
    }

    companion object {
        private const val TAG = "BleGattClient"
    }
}
