package com.offlinepayment.ble

import java.util.UUID

/**
 * Custom GATT service for Offlink offline payment BLE handshake.
 * Receiver hosts GATT server + advertisement; sender scans and connects as central.
 */
object BleOfflinkContract {
    val SERVICE_UUID: UUID = UUID.fromString("6f9a4b2c-0001-4a2e-9c3d-8e1f2a3b4c5d")

    /** Receiver → sender: binary [BlePaymentWire] receiver ack (pubkey + ts + sig only; may be chunked). */
    val CHAR_ACK_NOTIFY: UUID = UUID.fromString("6f9a4b2c-0002-4a2e-9c3d-8e1f2a3b4c5d")

    /** Sender → receiver: binary [BlePaymentWire] sender OK (ts + sig only). */
    val CHAR_CMD_WRITE: UUID = UUID.fromString("6f9a4b2c-0003-4a2e-9c3d-8e1f2a3b4c5d")

    val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    const val ADV_NAME_PREFIX = "Offlink"

    const val REQUEST_MTU = 256
}
