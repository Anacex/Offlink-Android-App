# Transaction Flow - FYP-1 (Current Semester Deliverables)

## Scope of FYP-1 (Current Semester Deliverables)

FYP-1 is focused on demonstrating a working prototype of the offline transaction flow using QR-based data transfer only, without the added complexity of BLE security or cryptographic enhancements.

---

## Core Deliverables Completed in FYP-1

### 1. Fully Functional Android App UI/UX

✅ **All Authentication Screens**
- User registration with email verification
- Two-step login (credentials → OTP → tokens)
- Password complexity validation
- Email verification flow
- Session management with JWT tokens

✅ **Wallet Creation & Setup**
- Wallet creation with bank account verification
- OTP-based wallet creation verification
- Wallet listing and management
- Balance display and updates
- Top-up functionality with OTP verification

✅ **Send / Receive Payment Screens**
- Send payment screen with QR scanner
- Payee confirmation dialog
- Transaction amount input with validation
- Dynamic transaction limit calculation
- QR code generation for transactions
- Receive payment screen with QR code display
- Transaction receipt generation

✅ **QR Scanner & QR Generator Screens**
- Payee identity QR scanner
- Transaction QR scanner (receiver side)
- QR code generator with dynamic limits
- Camera integration with ML Kit Barcode Scanning
- Real-time QR code validation

---

### 2. Cloud Backend Using Python FastAPI

✅ **User Registration**
- Signup endpoint with email verification
- Password hashing with bcrypt
- Email OTP generation and verification
- Device fingerprinting for security

✅ **Device Binding**
- Device fingerprint tracking
- Session management per device
- Token refresh mechanism

✅ **Wallet Creation**
- Two-step wallet creation (request → verify)
- Bank account number validation
- OTP-based verification
- Cryptographic key pair generation (RSA 2048-bit)

✅ **Identity Verification**
- Email verification system
- MFA (Multi-Factor Authentication) with OTP
- Device security enforcement

✅ **Secure API Endpoints**
- JWT-based authentication
- Rate limiting
- CORS configuration
- Security headers middleware
- Request logging

---

### 3. PostgreSQL Database

✅ **Users Table**
- User profile information
- Email verification status
- Password hashes
- MFA settings
- Device registry

✅ **Wallets Table**
- Wallet types (offline/current)
- Balance tracking
- Cryptographic keys (encrypted)
- Bank account information
- Wallet status (active/inactive)

✅ **KYC Status**
- Email verification tracking
- User verification status
- Device binding records

✅ **Device Registry**
- Device fingerprint storage
- Device-user associations
- Session tracking

✅ **Transaction Tables**
- Offline transactions table
- Wallet transfers table
- Transaction status tracking
- Receipt data storage

---

### 4. Offline QR-Based Payment Workflow (BLE Skipped)

#### Complete Transaction Flow:

**Step 1: Receiver Shows Identity QR**
- Receiver opens "My QR Code" screen
- App generates PayeeQRPayload containing:
  - `payeeId`: Receiver's user ID
  - `payeeName`: Receiver's name
  - `deviceId`: Device fingerprint
  - `nonce`: Unique QR code identifier
  - `maxTransactionLimit`: Dynamic limit based on receiver's current balance
- QR code displayed on screen
- Receiver cannot generate QR if balance >= 5000 PKR

**Step 2: Sender Scans and Confirms**
- Sender opens "Send Payment" screen
- Sender clicks "Scan Payee QR Code" button
- Camera opens to scan receiver's identity QR
- App validates QR payload and extracts payee information
- **Payee Confirmation Dialog** appears showing:
  - Payee ID
  - Payee Name
  - Device ID
  - **Max Transaction Limit** (calculated dynamically)
  - "Confirm" and "Cancel" buttons
- Sender reviews and clicks "Confirm" or "Cancel"

**Step 3: Sender Generates Transaction QR**
- After confirmation, sender enters transaction amount
- App validates:
  - Amount > 0
  - Amount <= sender's balance
  - Amount <= receiver's max transaction limit
  - Sender balance > 0 (cannot send with zero balance)
- Sender generates transaction QR code containing TransactionPayloadQR:
  - `txId`: Unique transaction ID (UUID)
  - `payerId`: Sender's user ID
  - `payeeId`: Receiver's user ID
  - `amount`: Transaction amount (in paisa)
  - `timestamp`: Current timestamp (milliseconds)
  - `nonce`: Random nonce
  - `payerName`: Sender's name
  - `note`: Optional transaction note

**Step 4: Receiver Scans and Logs Transaction**
- Receiver opens "My QR Code" screen
- Receiver clicks "Scan QR" button
- Camera opens to scan sender's transaction QR
- App validates transaction payload:
  - Timestamp within ±2 minutes
  - Amount > 0
  - Payee ID matches receiver's ID
- Transaction saved to local storage as "RECEIVED"
- Receiver's wallet balance updated (added)
- Transaction receipt generated

**Step 5: Sender Manually Taps "Sent" Button**
- After receiver scans QR, sender clicks "Sent" button
- Transaction saved to local storage as "SENT"
- Sender's wallet balance updated (subtracted)
- Transaction marked as completed
- Success message displayed

**Step 6: Both Log Transaction Locally**
- Both devices store transaction in Room database:
  - `txId`: Transaction ID
  - `payerId`: Sender ID
  - `payeeId`: Receiver ID
  - `amount`: Amount in paisa
  - `timestamp`: Transaction timestamp
  - `direction`: "SENT" or "RECEIVED"
  - `rawPayload`: Full JSON payload for future syncing

---

### 5. Local Offline Ledger

✅ **Transaction Storage (Room Database)**
- Both devices store complete transaction records
- Fields stored:
  - `txId`: Unique transaction identifier (UUID)
  - `payerId`: Sender's user ID
  - `payeeId`: Receiver's user ID
  - `amount`: Transaction amount in smallest currency unit (paisa)
  - `timestamp`: Transaction timestamp (currentTimeMillis)
  - `direction`: "SENT" or "RECEIVED"
  - `rawPayload`: Complete JSON payload for future server synchronization

✅ **Transaction History**
- All transactions visible in "Transactions" screen
- Local transactions displayed with:
  - Direction (SENT/RECEIVED) with color coding
  - Transaction ID
  - Amount (converted from paisa to PKR)
  - Timestamp (formatted)
  - Payer/Payee ID
- Server transfers also displayed
- Combined view of offline and online transactions

---

### 6. Preliminary Offline Transaction Validation

✅ **Timestamp Validation**
- Transaction timestamp must be within ±2 minutes of current time
- Prevents replay attacks with old QR codes
- Validates transaction freshness

✅ **Amount Validation**
- Amount must be > 0
- Amount cannot exceed sender's balance
- Amount cannot exceed receiver's max transaction limit
- Receiver's balance cannot exceed 5000 PKR after transaction

✅ **Payee ID Validation**
- Payee ID in transaction QR must match receiver's user ID
- Prevents transaction interception
- Ensures transaction reaches intended recipient

✅ **Device Lock Requirement for Sender**
- Sender must have device security enabled (PIN/Pattern/Biometric)
- Biometric authentication required before generating transaction QR
- Prevents unauthorized transaction generation

✅ **Balance Validation**
- Sender cannot proceed if balance is 0
- Receiver cannot generate QR if balance >= 5000 PKR
- Real-time balance checks before transaction

---

## Temporary Limitations in FYP-1

These are known limitations and intentionally kept this way due to time constraints:

### ❌ NO BLE Proximity Detection
- No Bluetooth Low Energy integration
- Cannot verify physical proximity between devices
- QR codes can be scanned from photos or printed copies

### ❌ NO BLE Acknowledgment Handshake
- No automatic confirmation between devices
- No two-way communication during transaction
- Manual "Sent" button required

### ❌ Sender Must Manually Tap "Sent" Button
- Transaction not automatically finalized
- Requires manual user action
- Potential for user error or forgetfulness

### ❌ Receiver Cannot Confirm to Sender Automatically
- No automatic notification to sender
- Sender must trust that receiver scanned the QR
- No real-time transaction status

### ❌ Sender Can Theoretically Force-Close App to Avoid Logging Sent Payment
- App can be closed before clicking "Sent"
- Transaction may not be logged on sender's device
- Potential for transaction disputes

### ❌ No Cryptographic Signing or Signature Verification
- Transaction payloads are not cryptographically signed
- No digital signature verification
- Cannot prove transaction authenticity

### ❌ No Deferred Sync or Hash-Chain Ledger Yet
- Transactions stored locally but not synced automatically
- No hash-chain to prevent ledger tampering
- No server-side reconciliation yet

**These limitations will be fully addressed in FYP-2.**

---

## 7.2 Scope of FYP-2 (Final Completed Product)

FYP-2 will transform the prototype into a fully secure, production-grade offline payments system using BLE-based acknowledgment, cryptography, and a double-spending-proof ledger.

### Key Features Planned for FYP-2

#### 1. Mandatory BLE Handshake (Core Upgrade)

✅ **BLE Connection Before Scanning**
- Establish BLE connection between sender and receiver
- Verify device proximity using RSSI (Received Signal Strength Indicator)
- Device identity verification via BLE

✅ **BLE ACK Loop After Scanning**
- Automatic acknowledgment after QR scan
- Two-way confirmation between devices
- Atomic transaction commit

✅ **Sender and Receiver Both Log Only After Mutual ACK**
- Transaction logged only when both devices confirm
- Prevents one-sided transaction logging
- Ensures transaction consistency

✅ **Solves All Manual "Sent" Button Problems**
- No manual button required
- Automatic transaction finalization
- Eliminates user error

✅ **Prevents Sender Cancellation or Force-Stop Attacks**
- Transaction cannot be cancelled once BLE handshake starts
- Force-closing app does not prevent transaction logging
- Atomic commit ensures transaction completion

---

#### 2. Automatic Transaction Finalization

✅ **Sender Presses Nothing**
- No manual "Sent" button
- Fully automatic transaction flow
- Seamless user experience

✅ **Receiver Scans QR**
- Receiver scans transaction QR
- BLE handshake automatically initiated
- Transaction finalized automatically

✅ **BLE Handshake Finalizes and Logs Automatically**
- Two-phase commit system offline
- Both devices log transaction simultaneously
- No manual intervention required

**This creates a two-phase commit system offline.**

---

#### 3. Cryptographic Enhancements

✅ **Transaction Payload Signatures**
- All transaction payloads cryptographically signed
- RSA-PSS + SHA-256 digital signatures
- Tamper-proof transaction data

✅ **Device-Bound Private Keys**
- Private keys stored in Android Keystore
- Hardware-backed key storage
- Keys never leave secure hardware

✅ **Public Key Exchange**
- Public keys exchanged via QR codes
- Public key verification
- Certificate pinning

✅ **QR Signature Verification**
- QR codes include digital signatures
- Signature verification before transaction
- Prevents QR code tampering

✅ **Tamper-Proof Payload Integrity**
- Hash-based integrity checking
- Payload cannot be modified without detection
- Cryptographic proof of authenticity

---

#### 4. Hash-Chained Local Ledger

Each new offline transaction will include:

✅ **Hash of Previous Transaction**
- Links transactions in a chain
- Prevents transaction deletion
- Ensures transaction order

✅ **Sequence Increment**
- Sequential transaction numbering
- Prevents transaction reordering
- Maintains transaction history

✅ **Device ID**
- Device identifier in each transaction
- Prevents device spoofing
- Tracks transaction origin

✅ **Time Window Validation**
- Timestamp validation with tighter windows
- Prevents replay attacks
- Ensures transaction freshness

**This prevents:**
- Local ledger tampering
- Manual deletion of transactions
- Manipulating wallet balance offline
- Transaction replay attacks

---

#### 5. Deferred Synchronization Protocol

When device comes online:

✅ **Pending Transactions Uploaded**
- All local transactions synced to server
- Batch upload for efficiency
- Resume capability for interrupted syncs

✅ **Server Matches & Finalizes**
- Server matches transactions from both parties
- Conflict resolution
- Transaction reconciliation

✅ **Conflicts Resolved**
- Duplicate transaction detection
- Transaction ordering conflicts resolved
- Consensus-based conflict resolution

✅ **Double-Spending Attempts Detected**
- Server-side balance verification
- Double-spending detection algorithms
- Fraud prevention

---

#### 6. BLE Proximity Verification

For anti-fraud and anti-replay:

✅ **RSSI Threshold**
- Signal strength threshold for proximity
- Prevents remote QR scanning
- Ensures physical proximity

✅ **Device Identity Signing**
- Device identity cryptographically signed
- Prevents device spoofing
- Ensures device authenticity

✅ **Protection from Scanning Printed QR Codes or Photos**
- BLE proximity requirement
- Cannot scan from photos
- Physical presence required

---

#### 7. End-to-End Security Model

✅ **Device Lock Enforcement**
- Mandatory device security
- Biometric authentication
- PIN/Pattern protection

✅ **Key Rotation**
- Periodic key rotation
- Compromised key replacement
- Enhanced security

✅ **Nonce + Timestamp Checks**
- Unique nonce per transaction
- Timestamp validation
- Replay attack prevention

✅ **Replay-Prevention Engine**
- Nonce tracking
- Timestamp windows
- Transaction deduplication

---

## Deliverables of Final Product

### 1. Complete QR + BLE Transaction Engine
- QR code generation and scanning
- BLE proximity detection
- BLE acknowledgment handshake
- Automatic transaction finalization

### 2. Security-Grade Local Ledger with Hash-Chaining
- Hash-chained transaction records
- Tamper-proof ledger
- Transaction integrity verification
- Local transaction history

### 3. Cryptographically Signed Transaction Payloads
- Digital signatures on all transactions
- Signature verification
- Public key infrastructure
- Certificate management

### 4. Two-Way BLE ACK-Based Atomic Commit
- Two-phase commit protocol
- Atomic transaction finalization
- Mutual acknowledgment
- Transaction consistency

### 5. Full Backend Integration with Sync Engine
- Server-side transaction matching
- Conflict resolution
- Double-spending detection
- Transaction reconciliation

### 6. Server-Side Reconciliation Logic
- Transaction matching algorithms
- Balance verification
- Fraud detection
- Dispute resolution

### 7. Attack Simulations & Security Analysis Report
- Security threat analysis
- Attack vector testing
- Penetration testing results
- Security audit report

---

## Technical Implementation Details

### Android App Architecture

**Architecture Pattern**: MVVM (Model-View-ViewModel)

**Key Components**:
- **Data Layer**: Room Database, Retrofit API clients, Repositories
- **Domain Layer**: ViewModels, Use Cases, Business Logic
- **UI Layer**: Jetpack Compose screens, Navigation Component

**Local Storage**:
- Room Database for offline transactions
- DataStore for user preferences
- Encrypted storage for sensitive data

### Backend Architecture

**Framework**: FastAPI (Python)

**Database**: PostgreSQL

**Key Features**:
- RESTful API design
- JWT authentication
- Rate limiting
- Security middleware
- Email service integration (SendGrid/SMTP)

### Transaction Flow Technical Details

**QR Code Format**:
- JSON payload encoded as Base64
- Includes all transaction metadata
- Validated before processing

**Validation Rules**:
- Timestamp: ±2 minutes window
- Amount: > 0, within limits
- Payee ID: Must match receiver
- Balance: Sufficient funds available

**Balance Updates**:
- Sender: Balance subtracted immediately
- Receiver: Balance added immediately
- Both: Transaction logged locally
- Server sync: When online, transactions uploaded

---

## Testing & Validation

### Manual Testing Checklist

- [x] User registration and email verification
- [x] Two-step login flow
- [x] Wallet creation and management
- [x] QR code generation and scanning
- [x] Transaction flow (send/receive)
- [x] Balance updates
- [x] Transaction history display
- [x] Offline transaction storage
- [x] Validation rules enforcement

### Known Issues & Workarounds

1. **Manual "Sent" Button**: Required due to no BLE handshake (FYP-2 will fix)
2. **No Automatic Confirmation**: Sender must trust receiver scanned QR (FYP-2 will fix)
3. **Force-Close Vulnerability**: App can be closed before logging (FYP-2 will fix)

---

## Future Enhancements (FYP-2)

1. BLE integration for proximity and acknowledgment
2. Cryptographic signing and verification
3. Hash-chained ledger
4. Automatic server synchronization
5. Enhanced security features
6. Production-grade error handling
7. Comprehensive testing suite

---

**Document Version**: 1.0  
**Last Updated**: December 2025  
**Status**: FYP-1 Complete, FYP-2 Planning Phase

