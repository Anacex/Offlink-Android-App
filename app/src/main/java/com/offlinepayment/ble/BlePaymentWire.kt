package com.offlinepayment.ble

import android.util.Base64
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Compact binary payloads for BLE only. No transaction fields (txId, amounts, payer/payee ids, names)
 * appear on the wire—only crypto material needed to verify bindings against data both sides already
 * hold from the QR channel.
 */
data class BleReceiverAckWire(
    /** X.509 SPKI of receiver's TEE signing key, Base64 (for [TeeEcdsaSigner.verifySignature]). */
    val receiverPubKeySpkiB64: String,
    val ts: Long,
    val signatureDer: ByteArray,
)

data class BleSenderOkWire(
    val ts: Long,
    val signatureDer: ByteArray,
)

object BlePaymentWire {
    const val VERSION_RECEIVER_ACK: Byte = 0x02
    const val VERSION_SENDER_OK: Byte = 0x03

    private const val MAX_SPKI = 512
    private const val MAX_SIG = 256

    fun encodeReceiverAck(pubSpki: ByteArray, ts: Long, signatureDer: ByteArray): ByteArray {
        require(pubSpki.isNotEmpty() && pubSpki.size <= MAX_SPKI)
        require(signatureDer.isNotEmpty() && signatureDer.size <= MAX_SIG)
        val out = ByteArrayOutputStream(4 + pubSpki.size + signatureDer.size)
        out.write(VERSION_RECEIVER_ACK.toInt())
        writeU16(out, pubSpki.size)
        out.write(pubSpki)
        writeU64(out, ts)
        writeU16(out, signatureDer.size)
        out.write(signatureDer)
        return out.toByteArray()
    }

    fun decodeReceiverAck(bytes: ByteArray): BleReceiverAckWire? = runCatching {
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        val v = bb.get()
        require(v == VERSION_RECEIVER_ACK)
        val pkLen = bb.short.toInt() and 0xffff
        require(pkLen in 1..MAX_SPKI)
        val pk = ByteArray(pkLen)
        bb.get(pk)
        val ts = bb.long
        val sigLen = bb.short.toInt() and 0xffff
        require(sigLen in 1..MAX_SIG)
        val sig = ByteArray(sigLen)
        bb.get(sig)
        require(bb.remaining() == 0)
        BleReceiverAckWire(
            receiverPubKeySpkiB64 = Base64.encodeToString(pk, Base64.NO_WRAP),
            ts = ts,
            signatureDer = sig,
        )
    }.getOrNull()

    fun encodeSenderOk(ts: Long, signatureDer: ByteArray): ByteArray {
        require(signatureDer.isNotEmpty() && signatureDer.size <= MAX_SIG)
        val out = ByteArrayOutputStream(12 + signatureDer.size)
        out.write(VERSION_SENDER_OK.toInt())
        writeU64(out, ts)
        writeU16(out, signatureDer.size)
        out.write(signatureDer)
        return out.toByteArray()
    }

    fun decodeSenderOk(bytes: ByteArray): BleSenderOkWire? = runCatching {
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        val v = bb.get()
        require(v == VERSION_SENDER_OK)
        val ts = bb.long
        val sigLen = bb.short.toInt() and 0xffff
        require(sigLen in 1..MAX_SIG)
        val sig = ByteArray(sigLen)
        bb.get(sig)
        require(bb.remaining() == 0)
        BleSenderOkWire(ts = ts, signatureDer = sig)
    }.getOrNull()

    private fun writeU16(out: ByteArrayOutputStream, v: Int) {
        out.write((v shr 8) and 0xff)
        out.write(v and 0xff)
    }

    private fun writeU64(out: ByteArrayOutputStream, v: Long) {
        for (i in 7 downTo 0) {
            out.write(((v shr (i * 8)) and 0xffL).toInt())
        }
    }
}
