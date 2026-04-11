package com.offlinepayment.ble

import android.content.Context
import android.util.Base64
import com.offlinepayment.data.PayeeQRPayload
import com.offlinepayment.data.TransactionPayloadQR
import com.offlinepayment.data.local.LocalTransaction
import com.offlinepayment.data.repository.WalletRepository
import com.offlinepayment.data.session.DeviceFingerprintProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.offlinepayment.utils.EncryptionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.math.BigDecimal
import kotlin.text.Charsets

/**
 * BLE + TEE signing: receiver sends signed ack; sender verifies and replies with signed OK.
 */
object BleHandshake {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val payloadAdapter = moshi.adapter(TransactionPayloadQR::class.java)

    suspend fun receiverSendAckAndAwaitOk(
        context: Context,
        repository: WalletRepository,
        payload: TransactionPayloadQR,
        currentPayeeId: String,
        timeoutMs: Long = 120_000L,
    ): Result<BleSenderOkWire> {
        val server = BlePaymentLink.server
            ?: return Result.failure(IllegalStateException("BLE receiver session not started"))

        BlePaymentLink.beginAtomicBleTransaction()
        try {
            return receiverSendAckAndAwaitOkInner(
                context = context,
                repository = repository,
                server = server,
                payload = payload,
                currentPayeeId = currentPayeeId,
                timeoutMs = timeoutMs,
            )
        } finally {
            BlePaymentLink.endAtomicBleTransaction()
        }
    }

    private suspend fun receiverSendAckAndAwaitOkInner(
        context: Context,
        repository: WalletRepository,
        server: BleGattServerManager,
        payload: TransactionPayloadQR,
        currentPayeeId: String,
        timeoutMs: Long,
    ): Result<BleSenderOkWire> {
        val signer = TeeEcdsaSigner.getInstance(context)
        val keyPair = signer.getOrCreateKeyPair()
        val pubSpki = keyPair.public.encoded
        val ts = System.currentTimeMillis()
        val canonical = BlePaymentMessages.canonicalReceiverAckSignString(
            payload.txId,
            currentPayeeId,
            payload.payerId,
            payload.amount,
            ts,
        )
        val sig = signer.sign(canonical)
        val wire = BlePaymentWire.encodeReceiverAck(pubSpki, ts, sig)

        var notified = false
        repeat(8) { attempt ->
            if (server.connectedDevice == null) {
                return Result.failure(
                    IllegalStateException("Bluetooth connection lost before the acknowledgment could be sent."),
                )
            }
            val ok = withContext(Dispatchers.Main) { server.notifyAckPayload(wire) }
            if (ok) {
                notified = true
                return@repeat
            }
            delay(250L * (attempt + 1))
        }
        if (!notified) {
            return Result.failure(IllegalStateException("Could not send BLE acknowledgment"))
        }

        val okResult: Result<BleSenderOkWire> = coroutineScope {
            val abortWait = async { BlePaymentLink.sessionAbortFlow.first() }
            val okWait = async {
                withTimeoutOrNull(timeoutMs) {
                    BlePaymentLink.senderOkFlow.first { w ->
                        val payerPk = payload.payerPkB64
                        if (payerPk.isNullOrBlank()) return@first false
                        val canon = BlePaymentMessages.canonicalSenderOkSignString(payload.txId, w.ts)
                        TeeEcdsaSigner.verifySignature(payerPk, canon, w.signatureDer)
                    }
                }
            }
            select<Result<BleSenderOkWire>> {
                abortWait.onAwait { reason ->
                    okWait.cancel()
                    Result.failure(IllegalStateException(reason))
                }
                okWait.onAwait { msg ->
                    abortWait.cancel()
                    if (msg == null) {
                        Result.failure(IllegalStateException("Timed out waiting for sender Bluetooth confirmation"))
                    } else {
                        Result.success(msg)
                    }
                }
            }
        }

        if (okResult.isFailure) {
            return Result.failure(
                okResult.exceptionOrNull() ?: IllegalStateException("Bluetooth receive handshake failed"),
            )
        }
        val ok = okResult.getOrNull()!!

        persistReceiverLedger(context, repository, payload, currentPayeeId)
        return Result.success(ok)
    }

    suspend fun persistReceiverLedger(
        context: Context,
        repository: WalletRepository,
        payload: TransactionPayloadQR,
        currentPayeeId: String,
    ) {
        val rawPayload = payloadAdapter.toJson(payload)
        val amountInPkr = BigDecimal(payload.amount.toString())
            .divide(BigDecimal("100"))
            .toPlainString()

        val payeeUserId = currentPayeeId.toIntOrNull()
        val receiverWallet = payeeUserId?.let {
            repository.getOfflineWalletByUserIdAndType(it, "offline")
        }

        val teeSig = try {
            val s = TeeEcdsaSigner.getInstance(context)
            s.getOrCreateKeyPair()
            Base64.encodeToString(
                s.sign(rawPayload.toByteArray(Charsets.UTF_8)),
                Base64.NO_WRAP,
            )
        } catch (_: Exception) {
            "tee-unavailable"
        }

        val encReceipt = runCatching { EncryptionHelper.encrypt(context, rawPayload) }.getOrElse { "{}" }

        val localTransaction = LocalTransaction(
            txId = payload.txId,
            senderWalletId = 0,
            receiverPublicKey = receiverWallet?.publicKey ?: "pending_${payload.payeeId}",
            amount = amountInPkr,
            currency = "PKR",
            transactionSignature = teeSig,
            nonce = payload.nonce,
            receiptHash = "",
            receiptData = encReceipt,
            status = "pending",
            createdAtDevice = payload.timestamp,
            deviceFingerprint = DeviceFingerprintProvider.getFingerprint(),
            payerId = payload.payerId,
            payeeId = payload.payeeId,
            direction = "RECEIVED",
            rawPayload = rawPayload,
        )
        repository.saveLocalTransaction(localTransaction)

        try {
            if (payeeUserId != null) {
                val offlineWallet = repository.getOfflineWalletByUserIdAndType(payeeUserId, "offline")
                if (offlineWallet != null) {
                    val currentBalance = BigDecimal(offlineWallet.balance)
                    val transactionAmount = BigDecimal(payload.amount.toString()).divide(BigDecimal("100"))
                    val newBalance = currentBalance.add(transactionAmount).min(BigDecimal("5000"))
                    repository.updateOfflineWalletBalance(offlineWallet.walletId, newBalance.toPlainString())
                }
            }
        } catch (_: Exception) {
        }
    }

    suspend fun senderVerifyAckAndReplyOk(
        context: Context,
        ack: BleReceiverAckWire,
        expectedPayload: TransactionPayloadQR,
    ): Boolean {
        val canonical = BlePaymentMessages.canonicalReceiverAckSignString(
            expectedPayload.txId,
            expectedPayload.payeeId,
            expectedPayload.payerId,
            expectedPayload.amount,
            ack.ts,
        )
        if (!TeeEcdsaSigner.verifySignature(ack.receiverPubKeySpkiB64, canonical, ack.signatureDer)) {
            return false
        }
        val client = BlePaymentLink.client ?: return false
        val signer = TeeEcdsaSigner.getInstance(context)
        signer.getOrCreateKeyPair()
        val ts = System.currentTimeMillis()
        val okCanon = BlePaymentMessages.canonicalSenderOkSignString(expectedPayload.txId, ts)
        val okSig = signer.sign(okCanon)
        val wire = BlePaymentWire.encodeSenderOk(ts, okSig)
        repeat(5) { i ->
            val w = withContext(Dispatchers.Main) { client.writeSenderOk(wire) }
            if (w) return true
            delay(200L * (i + 1))
        }
        return false
    }

    suspend fun persistSenderLedger(
        context: Context,
        repository: WalletRepository,
        payload: TransactionPayloadQR,
        walletId: Int,
        scannedPayeeQR: PayeeQRPayload?,
    ): Result<Unit> {
        val rawPayload = payloadAdapter.toJson(payload)
        val amountInPkr = BigDecimal(payload.amount.toString())
            .divide(BigDecimal("100"))
            .toPlainString()

        val offlineWallet = repository.getOfflineWalletById(walletId)
            ?: return Result.failure(IllegalStateException("Offline wallet not found"))
        val currentBalance = BigDecimal(offlineWallet.balance)
        val transactionAmount = BigDecimal(payload.amount.toString()).divide(BigDecimal("100"))
        if (currentBalance < transactionAmount) {
            return Result.failure(IllegalStateException("Insufficient offline balance"))
        }

        var receiverPublicKey = scannedPayeeQR?.public_key
        if (receiverPublicKey.isNullOrBlank()) {
            val payeeUserId = scannedPayeeQR?.payeeId?.toIntOrNull()
            val receiverWallet = payeeUserId?.let {
                repository.getOfflineWalletByUserIdAndType(it, "offline")
            }
            receiverPublicKey = receiverWallet?.publicKey
        }
        if (receiverPublicKey.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Receiver public key missing"))
        }

        val teeSig = try {
            val s = TeeEcdsaSigner.getInstance(context)
            s.getOrCreateKeyPair()
            Base64.encodeToString(s.sign(rawPayload.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        } catch (_: Exception) {
            "tee-unavailable"
        }

        val encReceipt = runCatching { EncryptionHelper.encrypt(context, rawPayload) }.getOrElse { "{}" }

        val localTransaction = LocalTransaction(
            txId = payload.txId,
            senderWalletId = walletId,
            receiverPublicKey = receiverPublicKey,
            amount = amountInPkr,
            currency = "PKR",
            transactionSignature = teeSig,
            nonce = payload.nonce,
            receiptHash = "",
            receiptData = encReceipt,
            status = "pending",
            createdAtDevice = payload.timestamp,
            deviceFingerprint = DeviceFingerprintProvider.getFingerprint(),
            payerId = payload.payerId,
            payeeId = payload.payeeId,
            direction = "SENT",
            rawPayload = rawPayload,
        )
        repository.saveLocalTransaction(localTransaction)
        val newBalance = currentBalance.subtract(transactionAmount)
        repository.updateOfflineWalletBalance(walletId, newBalance.toPlainString())
        return Result.success(Unit)
    }
}
