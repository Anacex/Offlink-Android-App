package com.offlinepayment.ble

import java.nio.charset.StandardCharsets

/**
 * Canonical byte strings signed in-app. Transaction fields exist only here (memory / QR), never on the BLE wire.
 * Over-the-air payloads are [BlePaymentWire] (pubkey + timestamp + signatures only).
 */
object BlePaymentMessages {

    fun canonicalReceiverAckSignString(
        txId: String,
        payeeId: String,
        payerId: String,
        amount: Long,
        ts: Long,
    ): ByteArray =
        "RECEIVER_ACK|$txId|$payeeId|$payerId|$amount|$ts"
            .toByteArray(StandardCharsets.UTF_8)

    fun canonicalSenderOkSignString(txId: String, ts: Long): ByteArray =
        "SENDER_OK|$txId|$ts".toByteArray(StandardCharsets.UTF_8)
}
