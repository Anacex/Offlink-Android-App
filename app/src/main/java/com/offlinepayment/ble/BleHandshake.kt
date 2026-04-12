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
import org.json.JSONObject
import com.offlinepayment.security.OfflineLedgerChain
import com.offlinepayment.utils.EncryptionHelper
import com.offlinepayment.utils.TransactionSigner
import kotlinx.coroutines.CoroutineStart
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
        if (payload.payerPkB64.isNullOrBlank()) {
            return Result.failure(
                IllegalStateException(
                    "This QR does not include the sender Bluetooth key (payerPkB64). " +
                        "The sender must connect over Bluetooth first, then generate the payment QR from the linked Send screen.",
                ),
            )
        }
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

        BlePaymentLink.drainSenderOkChannel()

        val okResult: Result<BleSenderOkWire> = coroutineScope {
            val abortWait = async { BlePaymentLink.sessionAbortFlow.first() }
            // Subscribe before notify: sender can write OK immediately after the last notify chunk;
            // SharedFlow tryEmit + late first() used to drop that OK so the receiver never advanced.
            val okCollector = async<BleSenderOkWire>(start = CoroutineStart.UNDISPATCHED) {
                while (true) {
                    val w = BlePaymentLink.receiveSenderOkWire()
                    val payerPk = payload.payerPkB64
                    if (payerPk.isNullOrBlank()) continue
                    val canon = BlePaymentMessages.canonicalSenderOkSignString(payload.txId, w.ts)
                    if (TeeEcdsaSigner.verifySignature(payerPk, canon, w.signatureDer)) {
                        return@async w
                    }
                }
                error("unreachable")
            }

            var notified = false
            repeat(8) { attempt ->
                if (server.connectedDevice == null) {
                    okCollector.cancel()
                    abortWait.cancel()
                    return@coroutineScope Result.failure(
                        IllegalStateException("Bluetooth connection lost before the acknowledgment could be sent."),
                    )
                }
                val ok = server.notifyAckPayload(wire)
                if (ok) {
                    notified = true
                    return@repeat
                }
                delay(250L * (attempt + 1))
            }
            if (!notified) {
                okCollector.cancel()
                abortWait.cancel()
                return@coroutineScope Result.failure(IllegalStateException("Could not send BLE acknowledgment"))
            }

            val picked = withTimeoutOrNull(timeoutMs) {
                select<Result<BleSenderOkWire>> {
                    abortWait.onAwait { reason ->
                        okCollector.cancel()
                        Result.failure(IllegalStateException(reason))
                    }
                    okCollector.onAwait { wire ->
                        abortWait.cancel()
                        Result.success(wire)
                    }
                }
            }
            if (picked != null) {
                picked
            } else {
                okCollector.cancel()
                abortWait.cancel()
                Result.failure(
                    IllegalStateException("Timed out waiting for sender Bluetooth confirmation"),
                )
            }
        }

        if (okResult.isFailure) {
            return Result.failure(
                okResult.exceptionOrNull() ?: IllegalStateException("Bluetooth receive handshake failed"),
            )
        }
        val ok = okResult.getOrNull()!!

        persistReceiverLedger(context, repository, payload, currentPayeeId).getOrElse { return Result.failure(it) }
        return Result.success(ok)
    }

    suspend fun persistReceiverLedger(
        context: Context,
        repository: WalletRepository,
        payload: TransactionPayloadQR,
        currentPayeeId: String,
    ): Result<Unit> {
        val rawPayload = payloadAdapter.toJson(payload)
        val amountInPkr = BigDecimal(payload.amount.toString())
            .divide(BigDecimal("100"))
            .toPlainString()

        val payeeUserId = currentPayeeId.toIntOrNull()
        val receiverWallet = payeeUserId?.let {
            repository.getOfflineWalletByUserIdAndType(it, "offline")
        } ?: return Result.failure(IllegalStateException("No offline wallet for this payee account on this device."))

        val walletPrivatePem = repository.getCachedPrivateKey(receiverWallet.walletId)
            ?: return Result.failure(
                IllegalStateException(
                    "Offline wallet private key is not on this device. Open the app online once so the key can be cached, then receive the payment again before syncing.",
                ),
            )

        val timestampIso = java.time.Instant.ofEpochMilli(payload.timestamp).toString()
        val txDataForServerSign = mapOf<String, Any>(
            "amount" to amountInPkr,
            "currency" to "PKR",
            "direction" to "RECEIVED",
            "nonce" to payload.nonce,
            "payee_id" to payload.payeeId,
            "payer_id" to payload.payerId,
            "receiver_wallet_id" to receiverWallet.walletId,
            "timestamp" to timestampIso,
            "tx_id" to payload.txId,
        )
        val rsaSigForSync = try {
            TransactionSigner.signRsaPssSha256(walletPrivatePem, txDataForServerSign)
        } catch (e: Exception) {
            return Result.failure(IllegalStateException("Could not sign receiver attestation for server sync: ${e.message}", e))
        }

        val teePayloadSig = try {
            val s = TeeEcdsaSigner.getInstance(context)
            s.getOrCreateKeyPair()
            Base64.encodeToString(s.sign(rawPayload.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        } catch (_: Exception) {
            "tee-unavailable"
        }

        val encReceipt = runCatching {
            val innerCipher = EncryptionHelper.encrypt(context, rawPayload)
            val binding = JSONObject()
                .put("tee_payload_sig_b64", teePayloadSig)
                .put("payload_cipher", innerCipher)
                .toString()
            EncryptionHelper.encrypt(context, binding)
        }.getOrElse { "{}" }
        val payloadContentHash = OfflineLedgerChain.sha256HexUtf8(rawPayload)

        // senderWalletId column stores the syncing party's offline wallet id (here: receiver).
        val localTransaction = LocalTransaction(
            txId = payload.txId,
            senderWalletId = receiverWallet.walletId,
            receiverPublicKey = receiverWallet.publicKey ?: "pending_${payload.payeeId}",
            amount = amountInPkr,
            currency = "PKR",
            transactionSignature = rsaSigForSync,
            nonce = payload.nonce,
            receiptHash = payloadContentHash,
            receiptData = encReceipt,
            status = "pending",
            createdAtDevice = payload.timestamp,
            deviceFingerprint = DeviceFingerprintProvider.getFingerprint(),
            payerId = payload.payerId,
            payeeId = payload.payeeId,
            direction = "RECEIVED",
            rawPayload = rawPayload,
        )
        val transactionAmount = BigDecimal(payload.amount.toString()).divide(BigDecimal("100"))
        return repository.recordReceivedOfflinePayment(
            transaction = localTransaction,
            walletId = receiverWallet.walletId,
            creditAmount = transactionAmount,
        )
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

        val walletPrivatePem = repository.getCachedPrivateKey(walletId)
            ?: return Result.failure(
                IllegalStateException(
                    "Offline wallet private key is not on this device. Open the app online once so the key can be cached, then complete the payment again before syncing.",
                ),
            )

        val timestampIso = java.time.Instant.ofEpochMilli(payload.timestamp).toString()
        val txDataForServerSign = mapOf<String, Any>(
            "amount" to amountInPkr,
            "currency" to "PKR",
            "nonce" to payload.nonce,
            "receiver_public_key" to receiverPublicKey,
            "sender_wallet_id" to walletId,
            "timestamp" to timestampIso,
        )
        val rsaSigForSync = try {
            TransactionSigner.signRsaPssSha256(walletPrivatePem, txDataForServerSign)
        } catch (e: Exception) {
            return Result.failure(IllegalStateException("Could not sign transaction for server sync: ${e.message}", e))
        }

        val teePayloadSig = try {
            val s = TeeEcdsaSigner.getInstance(context)
            s.getOrCreateKeyPair()
            Base64.encodeToString(s.sign(rawPayload.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        } catch (_: Exception) {
            "tee-unavailable"
        }

        val encReceipt = runCatching {
            val innerCipher = EncryptionHelper.encrypt(context, rawPayload)
            val binding = JSONObject()
                .put("tee_payload_sig_b64", teePayloadSig)
                .put("payload_cipher", innerCipher)
                .toString()
            EncryptionHelper.encrypt(context, binding)
        }.getOrElse { "{}" }
        val payloadContentHash = OfflineLedgerChain.sha256HexUtf8(rawPayload)

        val localTransaction = LocalTransaction(
            txId = payload.txId,
            senderWalletId = walletId,
            receiverPublicKey = receiverPublicKey,
            amount = amountInPkr,
            currency = "PKR",
            transactionSignature = rsaSigForSync,
            nonce = payload.nonce,
            receiptHash = payloadContentHash,
            receiptData = encReceipt,
            status = "pending",
            createdAtDevice = payload.timestamp,
            deviceFingerprint = DeviceFingerprintProvider.getFingerprint(),
            payerId = payload.payerId,
            payeeId = payload.payeeId,
            direction = "SENT",
            rawPayload = rawPayload,
        )
        return repository.recordSentOfflinePayment(
            transaction = localTransaction,
            walletId = walletId,
            debitAmount = transactionAmount,
        )
    }
}
