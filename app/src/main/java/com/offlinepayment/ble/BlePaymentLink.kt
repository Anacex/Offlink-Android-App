package com.offlinepayment.ble

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Holds active BLE managers for the payment handshake (survives short navigation).
 * Tear down via [clear] only from explicit user actions (disconnect / cancel / exit), not automatically
 * after a successful transaction, so the GATT link is not dropped mid-handshake on the peer.
 */
object BlePaymentLink {
    @Volatile
    var server: BleGattServerManager? = null

    @Volatile
    var client: BleGattClientManager? = null

    @Volatile
    var isReceiverHosting: Boolean = false

    /** Sender completed BLE scan + GATT connection before opening Send Payment. */
    @Volatile
    var senderBleSessionActive: Boolean = false

    /**
     * Sender opened Send after BLE link; payment must use BLE ack (no manual "Sent"), and link loss aborts the flow.
     */
    @Volatile
    var wasLinkedForBleSend: Boolean = false

    /**
     * True while payment QR is shown (sender) or BLE receive handshake is in progress (receiver).
     * Used to abort if GATT drops mid-transaction.
     */
    @Volatile
    var isAtomicBleTransactionActive: Boolean = false

    private val _sessionAbort = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val sessionAbortFlow: SharedFlow<String> = _sessionAbort.asSharedFlow()

    /** Parsed receiver ack; transaction binding is verified locally against QR payload + signatures. */
    val receiverAckFlow = MutableSharedFlow<BleReceiverAckWire>(extraBufferCapacity = 8)

    /**
     * Sender OK from GATT write. A conflated channel avoids dropping the only OK when it arrives
     * before a SharedFlow collector is subscribed (tryEmit with no subscribers can fail / race).
     */
    private val senderOkChannel = Channel<BleSenderOkWire>(capacity = Channel.CONFLATED)

    fun publishReceiverAck(msg: BleReceiverAckWire) {
        receiverAckFlow.tryEmit(msg)
    }

    fun drainSenderOkChannel() {
        while (senderOkChannel.tryReceive().isSuccess) {
            // drop stale OKs from a previous attempt
        }
    }

    suspend fun receiveSenderOkWire(): BleSenderOkWire = senderOkChannel.receive()

    fun publishSenderOk(msg: BleSenderOkWire) {
        senderOkChannel.trySend(msg)
    }

    fun beginAtomicBleTransaction() {
        isAtomicBleTransactionActive = true
    }

    fun endAtomicBleTransaction() {
        isAtomicBleTransactionActive = false
    }

    /**
     * Emits once if an atomic BLE payment was in progress; clears the atomic flag.
     */
    fun notifySessionAbortIfAtomic(reason: String) {
        if (!isAtomicBleTransactionActive) return
        isAtomicBleTransactionActive = false
        _sessionAbort.tryEmit(reason)
    }

    fun onSenderGattDisconnected() {
        if (isAtomicBleTransactionActive) {
            isAtomicBleTransactionActive = false
            _sessionAbort.tryEmit(
                "Bluetooth connection lost. The payment was cancelled and nothing was saved.",
            )
        }
        senderBleSessionActive = false
    }

    fun onReceiverCentralDisconnected() {
        if (isAtomicBleTransactionActive) {
            isAtomicBleTransactionActive = false
            _sessionAbort.tryEmit(
                "Bluetooth connection lost. The payment was cancelled and nothing was saved.",
            )
        }
    }

    fun clear() {
        endAtomicBleTransaction()
        drainSenderOkChannel()
        server?.stop()
        server = null
        client?.disconnect()
        client?.stopScan()
        client = null
        isReceiverHosting = false
        senderBleSessionActive = false
        wasLinkedForBleSend = false
    }
}
