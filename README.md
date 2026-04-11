# Offline Payment System — Android app

## Bluetooth (BLE) offline payment confirmation

Send and receive flows that use **Bluetooth Low Energy** replace the old manual **“Sent”** tap with a **signed acknowledgment** over a GATT link. The sender and receiver must stay connected for the whole in-flight payment; if the link drops, the app **aborts** the payment and does **not** treat it as complete.

---

## Data channels: what travels where

| Channel | Carries |
|--------|---------|
| **Payee identity QR** | Payee id, name, device id, nonce, optional limits / public key (JSON). |
| **Transaction payload QR** | Full payment instruction: `txId`, `payerId`, `payeeId`, `amount`, `timestamp`, `nonce`, `payerName`, optional `note`, optional `payerPkB64` (sender TEE public key for verifying BLE OK). Base64-wrapped JSON as today. |
| **BLE GATT (this app)** | **Only** cryptographic handshake material: see [BLE wire format](#ble-wire-format-binary) below. **No** transaction ids, amounts, payer/payee ids, names, or notes are placed on the BLE link. |
| **BLE connection** | Provides **liveness** (proximity session). There is **no** separate application-level keep-alive payload; disconnect during an atomic payment triggers abort. |

Transaction **meaning** (who pays whom, how much) is established by the **QR** payload on each device. BLE proves **agreement** at that moment via **signatures** bound to the same logical message, without re-transmitting that message over the radio.

---

## BLE wire format (binary)

Implementation: `ble/BlePaymentWire.kt`. Payloads may be **chunked** over notifications/writes (see `BleGattServerManager` / `BleGattClientManager`).

### Receiver → sender (characteristic notify, `CHAR_ACK_NOTIFY`)

| Field | Description |
|--------|--------------|
| `0x02` | Wire version (`VERSION_RECEIVER_ACK`). |
| `u16` BE | Length of SPKI blob (X.509 subject public key info for receiver’s P-256 key). |
| `bytes` | SPKI (receiver TEE signing **public** key only). |
| `i64` BE | Timestamp `ts` (milliseconds), used in the signed canonical string. |
| `u16` BE | ECDSA signature length (DER). |
| `bytes` | ECDSA signature over the **canonical string** (see below)—not the string itself. |

### Sender → receiver (characteristic write, `CHAR_CMD_WRITE`)

| Field | Description |
|--------|--------------|
| `0x03` | Wire version (`VERSION_SENDER_OK`). |
| `i64` BE | Timestamp `ts` (milliseconds). |
| `u16` BE | ECDSA signature length (DER). |
| `bytes` | ECDSA signature over the **canonical string** (see below). |

---

## Canonical strings (in memory / implied by QR only)

These strings are **never sent as plaintext on BLE**. Both peers reconstruct them locally from the **same** `TransactionPayloadQR` (sender created it; receiver parsed it from the transaction QR) plus the **`ts` values from the wire**.

Defined in `ble/BlePaymentMessages.kt`:

- **Receiver ack signing input:**  
  `RECEIVER_ACK|<txId>|<payeeId>|<payerId>|<amount>|<ts>`  
  (UTF-8 bytes; signed by the receiver’s TEE key.)

- **Sender OK signing input:**  
  `SENDER_OK|<txId>|<ts>`  
  (UTF-8 bytes; signed by the sender’s TEE key; receiver verifies using `payerPkB64` from the scanned QR.)

Verification flow:

1. Receiver scans transaction QR → has full payload → signs receiver ack with chosen `ts` → sends **only** SPKI + `ts` + sig on BLE.  
2. Sender verifies sig using SPKI from wire and canonical string built from **local** payload + wire `ts`.  
3. Sender signs sender OK with new `ts` → sends **only** `ts` + sig on BLE.  
4. Receiver verifies using `payerPkB64` from QR and canonical `SENDER_OK|txId|ts`.

---

## End-to-end flow (UI)

1. **Wallet → Send**: device must pass **sender** checks (classic + BLE, LE scanner, Bluetooth on, Android Keystore signing healthy). User scans for the receiver and opens **Send** after GATT connect.
2. **Wallet → Receive payment (Bluetooth)**: device must pass **receiver** checks (BLE, **LE multi-advertisement** support, advertiser available, Keystore healthy). Receiver advertises and continues to the transaction QR scanner (`bleHost=1`).
3. **QRs**: payee QR, then sender generates **transaction payload QR** (includes `payerPkB64` when hardware signing works—required for the Bluetooth path to verify sender OK).
4. **Atomic BLE phase**: while the payment QR is shown (sender) or the receive handshake runs (receiver), the GATT session is considered **atomic**. **Link loss cancels** the operation; ledgers are only updated after a full ack exchange.
5. **Handshake**: as described above—**binary** frames only on BLE; ledger **SENT** / **RECEIVED** after successful verify + persist logic in `BleHandshake`.

---

## Device and security requirements

| Check | Sender | Receiver |
|--------|--------|----------|
| `FEATURE_BLUETOOTH` / `FEATURE_BLUETOOTH_LE` | Yes | Yes |
| Bluetooth on | Yes | Yes |
| `BluetoothLeScanner` | Yes | — |
| `isMultipleAdvertisementSupported` + `BluetoothLeAdvertiser` | — | Yes |
| `TeeEcdsaSigner.healthCheck()` (AndroidKeyStore P-256, StrongBox when available) | Yes | Yes |

If any check fails, **Send** / **Receive payment (Bluetooth)** show a **Toast** and an explanatory card on the wallet. The scan/receive screens re-check before starting BLE.

**Keystore errors** (e.g. `KeyPermanentlyInvalidatedException` after lock screen / biometric changes) block BLE payments until the keystore is usable again.

---

## Key source files

- `ble/BleOfflinkContract.kt` — GATT service and characteristic UUIDs.
- `ble/BlePaymentMessages.kt` — canonical signing strings (in memory / QR only; **not** BLE plaintext).
- `ble/BlePaymentWire.kt` — binary BLE codec (pubkey + ts + sig / ts + sig only).
- `ble/TeeEcdsaSigner.kt` — AndroidKeyStore ECDSA P-256 + `healthCheck()`.
- `ble/BleGattClientManager.kt` / `BleGattServerManager.kt` — central / peripheral roles.
- `ble/BlePaymentLink.kt` — shared session, **atomic** flag, **`sessionAbortFlow`** on link loss.
- `ble/BleHandshake.kt` — verify/sign and ledger persistence; races ack/OK waits against abort.
- `ble/BleOfflinkEligibility.kt` — user-facing **blocked** reasons.
- `ui/SendPaymentScreen.kt` — waits for BLE ack; **no manual “Sent”** when `wasLinkedForBleSend`; link-loss UI.
- `ui/qr/TransactionQRScannerScreen.kt` — BLE receive path with abort handling + Toast.
- `AndroidManifest.xml` — `BLUETOOTH_*` permissions and `bluetooth_le` feature.

---

## Building

From `Android-App/`:

```bash
./gradlew :app:assembleDebug
```

Use **two physical devices** for BLE testing; emulators are often unreliable for GATT and advertising.

---

## Related documentation (repository root)

- **[OFFLINE_TRANSACTION_WORKFLOW.md](../OFFLINE_TRANSACTION_WORKFLOW.md)** — Overall offline QR workflow; includes how the **Bluetooth confirmation path** relates to persistence timing.
- **[README.md](../README.md)** — Project entry point and doc index.
