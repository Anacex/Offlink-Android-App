package com.offlinepayment.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Receiver side: BLE advertisement + GATT server.
 * Notifies sender via [CHAR_ACK_NOTIFY]; receives [CHAR_CMD_WRITE] (SENDER_OK).
 */
class BleGattServerManager(
    private val context: Context,
    private val mainHandler: Handler = Handler(Looper.getMainLooper()),
) {

    private val bluetoothManager =
        context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter = bluetoothManager.adapter

    private var gattServer: BluetoothGattServer? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private var ackCharacteristic: BluetoothGattCharacteristic? = null
    private var cmdCharacteristic: BluetoothGattCharacteristic? = null

    @Volatile
    var connectedDevice: BluetoothDevice? = null
        private set

    /**
     * Central that enabled notifications on [CHAR_ACK_NOTIFY] (CCCD). Prefer this for
     * [notifyAckPayload] when multiple GATT centrals are connected (last-writer wins).
     */
    @Volatile
    private var ackNotifyTarget: BluetoothDevice? = null

    @Volatile
    var negotiatedMtu: Int = 23
        private set

    var onSenderOkPayload: ((ByteArray) -> Unit)? = null
    var onCentralConnected: ((BluetoothDevice) -> Unit)? = null
    var onCentralDisconnected: (() -> Unit)? = null

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d(TAG, "Advertise started")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "Advertise failed: $errorCode")
        }
    }

    private val gattCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                connectedDevice = device
                mainHandler.post { onCentralConnected?.invoke(device) }
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                if (connectedDevice?.address == device.address) {
                    connectedDevice = null
                }
                if (ackNotifyTarget?.address == device.address) {
                    ackNotifyTarget = null
                }
                mainHandler.post { onCentralDisconnected?.invoke() }
            }
        }

        override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
            negotiatedMtu = mtu
            Log.d(TAG, "Server MTU=$mtu")
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray,
        ) {
            if (characteristic.uuid == BleOfflinkContract.CHAR_CMD_WRITE) {
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                }
                mainHandler.post { onSenderOkPayload?.invoke(value.copyOf()) }
            } else if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
            }
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray,
        ) {
            if (descriptor.uuid == BleOfflinkContract.CCCD_UUID) {
                val parent = descriptor.characteristic
                if (parent?.uuid == BleOfflinkContract.CHAR_ACK_NOTIFY) {
                    val enabled = value.isNotEmpty() &&
                        (value.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) ||
                            value.contentEquals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE))
                    if (enabled) {
                        ackNotifyTarget = device
                        Log.d(TAG, "ACK notifications enabled by ${device.address}")
                    } else if (ackNotifyTarget?.address == device.address) {
                        ackNotifyTarget = null
                    }
                }
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value)
            } else if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
            }
        }
    }

    fun isBluetoothAvailable(): Boolean = adapter?.isEnabled == true

    /** True while GATT server (and typically advertising) is active — used to restore UI without restarting. */
    fun isAdvertisingSessionActive(): Boolean = gattServer != null

    fun startAdvertisingAndServer(displaySuffix: String) {
        stop()
        val ad = adapter ?: return
        advertiser = ad.bluetoothLeAdvertiser ?: return

        val ack = BluetoothGattCharacteristic(
            BleOfflinkContract.CHAR_ACK_NOTIFY,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ,
        )
        val cccd = BluetoothGattDescriptor(
            BleOfflinkContract.CCCD_UUID,
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE,
        )
        ack.addDescriptor(cccd)

        val cmd = BluetoothGattCharacteristic(
            BleOfflinkContract.CHAR_CMD_WRITE,
            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE or BluetoothGattCharacteristic.PERMISSION_READ,
        )

        val service = BluetoothGattService(
            BleOfflinkContract.SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY,
        )
        service.addCharacteristic(ack)
        service.addCharacteristic(cmd)

        ackCharacteristic = ack
        cmdCharacteristic = cmd

        gattServer = bluetoothManager.openGattServer(context.applicationContext, gattCallback)
        gattServer?.addService(service)

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .build()

        val advData = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(BleOfflinkContract.SERVICE_UUID))
            .setIncludeDeviceName(false)
            .build()

        val name = "${BleOfflinkContract.ADV_NAME_PREFIX}-${displaySuffix.takeLast(6)}"
        val scanResp = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            @Suppress("DEPRECATION")
            ad.name = name
        }

        advertiser?.startAdvertising(settings, advData, scanResp, advertiseCallback)
    }

    /**
     * Sends a binary ack payload (no transaction fields) to the central via notify (chunked if needed).
     * Must be called from a coroutine: uses [delay] between chunks instead of [Thread.sleep] so the
     * main thread is never blocked (chunked acks are larger than one ATT PDU).
     */
    suspend fun notifyAckPayload(payload: ByteArray): Boolean {
        val device = ackNotifyTarget ?: connectedDevice ?: run {
            Log.w(TAG, "notifyAckPayload: no central (CCCD not enabled yet or disconnected)")
            return false
        }
        val char = ackCharacteristic ?: return false
        val g = gattServer ?: return false

        // Conservative chunk size so notifies succeed even if server MTU callback never fires.
        val mtuPayload = 18
        if (payload.size <= mtuPayload) {
            val packet = ByteArray(2 + payload.size)
            packet[0] = 1
            packet[1] = 0
            System.arraycopy(payload, 0, packet, 2, payload.size)
            return withContext(Dispatchers.Main) {
                char.value = packet
                g.notifyCharacteristicChanged(device, char, false)
            }
        }

        val total = (payload.size + mtuPayload - 1) / mtuPayload
        if (total > 255) return false

        for (i in 0 until total) {
            val from = i * mtuPayload
            val to = minOf(from + mtuPayload, payload.size)
            val chunk = payload.copyOfRange(from, to)
            val packet = ByteArray(2 + chunk.size)
            packet[0] = total.toByte()
            packet[1] = i.toByte()
            System.arraycopy(chunk, 0, packet, 2, chunk.size)
            val ok = withContext(Dispatchers.Main) {
                char.value = packet
                g.notifyCharacteristicChanged(device, char, false)
            }
            if (!ok) {
                Log.w(TAG, "notifyAckPayload: chunk $i/$total failed")
                return false
            }
            if (i < total - 1) {
                delay(25)
            }
        }
        return true
    }

    fun stop() {
        runCatching { advertiser?.stopAdvertising(advertiseCallback) }
        advertiser = null
        runCatching { gattServer?.close() }
        gattServer = null
        ackCharacteristic = null
        cmdCharacteristic = null
        connectedDevice = null
        ackNotifyTarget = null
        negotiatedMtu = 23
    }

    companion object {
        private const val TAG = "BleGattServer"
    }
}
