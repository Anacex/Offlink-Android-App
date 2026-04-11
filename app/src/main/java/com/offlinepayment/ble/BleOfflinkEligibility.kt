package com.offlinepayment.ble

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.security.keystore.KeyPermanentlyInvalidatedException
import java.security.ProviderException
import java.security.UnrecoverableKeyException

sealed class BleOfflinkAssessment {
    data object Ok : BleOfflinkAssessment()
    data class Blocked(val userMessage: String) : BleOfflinkAssessment()

    val isOk: Boolean get() = this is Ok
}

/**
 * Gate Bluetooth payment flows: classic + LE presence, role-specific capability, and AndroidKeyStore signing health.
 */
object BleOfflinkEligibility {

    fun assessSender(context: Context): BleOfflinkAssessment {
        val app = context.applicationContext
        basicBluetooth(app)?.let { return it }
        val adapter = (app.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
            ?: return BleOfflinkAssessment.Blocked("Bluetooth is not available on this device.")
        if (!adapter.isEnabled) {
            return BleOfflinkAssessment.Blocked("Turn on Bluetooth to send with Bluetooth confirmation.")
        }
        if (adapter.bluetoothLeScanner == null) {
            return BleOfflinkAssessment.Blocked(
                "This device cannot scan for Bluetooth Low Energy receivers (LE scanner is unavailable).",
            )
        }
        return TeeEcdsaSigner.healthCheck(app).fold(
            onSuccess = { BleOfflinkAssessment.Ok },
            onFailure = { BleOfflinkAssessment.Blocked(teeUserMessage(it)) },
        )
    }

    fun assessReceiver(context: Context): BleOfflinkAssessment {
        val app = context.applicationContext
        basicBluetooth(app)?.let { return it }
        val adapter = (app.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
            ?: return BleOfflinkAssessment.Blocked("Bluetooth is not available on this device.")
        if (!adapter.isEnabled) {
            return BleOfflinkAssessment.Blocked("Turn on Bluetooth to receive with Bluetooth confirmation.")
        }
        if (!adapter.isMultipleAdvertisementSupported) {
            return BleOfflinkAssessment.Blocked(
                "This phone cannot run Bluetooth receive mode (LE multi-advertisement not supported).",
            )
        }
        if (adapter.bluetoothLeAdvertiser == null) {
            return BleOfflinkAssessment.Blocked(
                "Bluetooth Low Energy advertising is not available on this device.",
            )
        }
        return TeeEcdsaSigner.healthCheck(app).fold(
            onSuccess = { BleOfflinkAssessment.Ok },
            onFailure = { BleOfflinkAssessment.Blocked(teeUserMessage(it)) },
        )
    }

    private fun basicBluetooth(app: Context): BleOfflinkAssessment.Blocked? {
        if (!app.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            return BleOfflinkAssessment.Blocked(
                "This device has no Bluetooth. Bluetooth payments are unavailable.",
            )
        }
        if (!app.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return BleOfflinkAssessment.Blocked(
                "This device does not support Bluetooth Low Energy (BLE). Bluetooth payments are unavailable.",
            )
        }
        return null
    }

    private fun teeUserMessage(t: Throwable): String {
        val chain = buildList {
            var x: Throwable? = t
            while (x != null) {
                add(x)
                x = x.cause
            }
        }
        return when {
            chain.any { it is KeyPermanentlyInvalidatedException } ->
                "Secure signing key was invalidated (for example after a fingerprint or lock screen change). " +
                    "Restart the app. Bluetooth payments require a working hardware keystore."

            chain.any { it is UnrecoverableKeyException } ->
                "The secure signing key cannot be read from hardware. Bluetooth payments are unavailable."

            chain.any { it is ProviderException } ->
                "Android Keystore could not sign on this device. Bluetooth payments are unavailable."

            else ->
                t.message?.takeIf { it.isNotBlank() }?.let { m ->
                    "Secure signing failed: $m"
                } ?: "Hardware-backed secure signing is not working. Bluetooth payments are unavailable."
        }
    }
}
