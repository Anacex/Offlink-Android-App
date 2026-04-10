# Offline Payment System - Android App

A secure Android application for offline digital payments, enabling transactions without continuous internet connectivity while maintaining the highest standards of cybersecurity and regulatory compliance.

## 📱 Project Overview

This Android app is part of the **Offline Payment System** project, designed to enable secure offline transactions for Pakistan's financial ecosystem. The app allows users to:

- Create accounts and authenticate securely
- Manage online and offline wallets
- Transfer funds between wallets
- Perform offline transactions (via QR codes)
- Sync transactions when connectivity is restored

**Backend Server:** [https://offline-payment-system-android-f8hr.onrender.com](https://offline-payment-system-android-f8hr.onrender.com)

---

## 🚀 Quick Start

### Prerequisites

- **Android Studio** (Hedgehog | 2023.1.1 or later) - **Required for consistent development**
- **JDK 17** (included with Android Studio) - **Do not use JDK 11 or JDK 21**
- **Android SDK** (API 24+) - Installed automatically via Android Studio
- **Kotlin** 1.9.0+ - Managed by Gradle (no manual installation)
- Physical Android device or emulator (API 24+)

> **Note**: We use **Gradle Wrapper** to ensure everyone uses the same Gradle version. No manual Gradle installation needed!

### Setup Instructions

1. **Clone the repository** (if not already done):
   ```bash
   git clone <repository-url>
   cd Offline-Payment-System-Android/Android-App
   ```

2. **Open in Android Studio**:
   - File → Open → Select `Android-App` folder
   - Wait for Gradle sync to complete (may take a few minutes on first run)

3. **Configure API Base URL** (if needed):
   - The base URL is injected into `BuildConfig.API_BASE_URL`.
   - By default it targets the Cloud Render server.
   - For local Wi‑Fi testing, build with:
     ```bash
     ./gradlew assembleDebug -PAPI_BASE_URL="http://<PC_LAN_IP>:8000/"
     ```
   - Do not hardcode your LAN IP into committed files.

4. **Build the project**:
   ```bash
   ./gradlew build
   # Or use Android Studio: Build → Make Project
   ```

5. **Run on device/emulator**:
   - Connect your Android device via USB (enable USB debugging)
   - Or start an Android emulator
   - Click Run in Android Studio, or:
     ```bash
     ./gradlew installDebug
     ```

### ⚠️ Important: Server & Database Wake-Up

**The backend server and Supabase database are on free tier services and will sleep after periods of inactivity.**

Before testing the app, you **must wake up** the services:

1. **Wake up the Render server**:
   - Visit: https://offline-payment-system-android-f8hr.onrender.com/health
   - Wait 30-60 seconds for the server to fully start
   - You should see a JSON response: `{"status": "healthy"}`

2. **Wake up Supabase database**:
   - The database will automatically wake up when the server makes its first connection
   - If you see connection errors, wait a bit longer and try again

3. **Test connectivity**:
   ```bash
   curl https://offline-payment-system-android-f8hr.onrender.com/health
   ```

**Note**: The first API call after inactivity may take 30-60 seconds. Subsequent calls will be fast until the next sleep period.

---

## 📁 Project Structure

```
Android-App/
├── app/
│   ├── build.gradle.kts                    # App-level Gradle configuration
│   ├── proguard-rules.pro                  # ProGuard rules for release builds
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml         # App permissions and components
│           ├── java/com/offlinepayment/
│           │   ├── MainActivity.kt          # Main entry point, navigation, app state
│           │   ├── OfflinePaymentApp.kt     # Application class, device fingerprint init
│           │   │
│           │   ├── data/                    # Data layer
│           │   │   ├── AuthModels.kt        # Auth request/response DTOs
│           │   │   ├── WalletModels.kt      # Wallet request/response DTOs
│           │   │   ├── TopUpModels.kt       # Top-up request/response DTOs
│           │   │   ├── TransactionIntent.kt # Transaction intent data model
│           │   │   ├── TransactionPayloadQR.kt # Transaction QR payload (Parcelable)
│           │   │   ├── ReceiverQRPayload.kt # Payee QR payload (Parcelable)
│           │   │   ├── OfflineTransactionPayload.kt # Offline transaction payload
│           │   │   │
│           │   │   ├── network/             # Network layer
│           │   │   │   ├── ApiClient.kt     # Retrofit client setup, interceptors
│           │   │   │   ├── AuthApi.kt       # Auth endpoints interface
│           │   │   │   └── WalletApi.kt     # Wallet endpoints interface
│           │   │   │
│           │   │   ├── repository/          # Repository layer
│           │   │   │   ├── AuthRepository.kt # Auth business logic, offline login
│           │   │   │   └── WalletRepository.kt # Wallet operations, local storage
│           │   │   │
│           │   │   ├── session/             # Session management
│           │   │   │   ├── AuthSessionManager.kt # Token storage, session state
│           │   │   │   └── DeviceFingerprintProvider.kt # Device ID generation
│           │   │   │
│           │   │   └── local/               # Local database (Room)
│           │   │       ├── AppDatabase.kt   # Room database definition
│           │   │       ├── OfflineUser.kt   # Offline user entity
│           │   │       ├── OfflineUserDao.kt # User DAO
│           │   │       ├── OfflineWallet.kt # Offline wallet entity
│           │   │       ├── OfflineWalletDao.kt # Wallet DAO
│           │   │       ├── OfflineTransaction.kt # Local transaction entity
│           │   │       └── OfflineTransactionDao.kt # Transaction DAO
│           │   │
│           │   ├── ui/                       # UI layer (Jetpack Compose)
│           │   │   ├── auth/                # Authentication screens
│           │   │   │   ├── AuthViewModel.kt # Auth state management, login flow
│           │   │   │   ├── LoginScreen.kt   # Login UI with OTP step
│           │   │   │   ├── CreateAccountScreen.kt # Signup UI
│           │   │   │   └── EmailVerificationDialog.kt # Email verification dialog
│           │   │   │
│           │   │   ├── wallet/              # Wallet management
│           │   │   │   └── WalletViewModel.kt # Wallet state, transfers
│           │   │   │
│           │   │   ├── profile/             # User profile
│           │   │   │   ├── ProfileViewModel.kt # Profile state management
│           │   │   │   └── ProfileScreen.kt  # Profile display screen
│           │   │   │
│           │   │   ├── qr/                  # QR code screens
│           │   │   │   ├── QRCodeScreen.kt  # Display user's QR code
│           │   │   │   ├── QRScannerScreen.kt # Generic QR scanner
│           │   │   │   ├── TransactionQRScannerScreen.kt # Transaction QR scanner (receiver)
│           │   │   │   ├── TransactionReceivedScreen.kt # Transaction receipt (receiver)
│           │   │   │   ├── TransactionIntentReceivedScreen.kt # Transaction intent screen
│           │   │   │   └── PaymentConfirmationScreen.kt # Payment confirmation
│           │   │   │
│           │   │   ├── WalletScreen.kt      # Main wallet dashboard
│           │   │   ├── SendPaymentScreen.kt # Send payment flow
│           │   │   ├── TransactionListScreen.kt # Transaction history
│           │   │   ├── TopUpScreen.kt       # Wallet top-up screen
│           │   │   └── theme/
│           │   │       └── Theme.kt         # Material Design 3 theme
│           │   │
│           │   └── utils/                   # Utility classes
│           │       ├── CurrencyUtils.kt     # PKR formatting utilities
│           │       ├── QRCodeHelper.kt      # QR code generation/parsing
│           │       ├── WalletLimits.kt      # Wallet limits and validation
│           │       ├── NetworkUtils.kt      # Network connectivity detection
│           │       ├── BiometricAuthHelper.kt # Biometric authentication
│           │       ├── EncryptionHelper.kt  # Data encryption utilities
│           │       ├── PasswordUtils.kt     # Password validation
│           │       └── TransactionSigner.kt # Transaction signing (future)
│           │
│           └── res/                          # Resources
│               ├── values/
│               │   ├── strings.xml          # String resources
│               │   ├── colors.xml           # Color definitions
│               │   └── themes.xml           # Material theme
│               └── drawable/                # Icons and graphics
│
├── build.gradle.kts            # Project-level Gradle config
├── settings.gradle.kts          # Project settings
├── gradle.properties           # Gradle properties
├── gradlew / gradlew.bat       # Gradle wrapper scripts
└── README.md                   # This file
```

---

## 🔑 Important Files & Their Purpose

### Core Application Files

| File | Purpose |
|------|---------|
| `OfflinePaymentApp.kt` | Application class - initializes device fingerprint on app start |
| `MainActivity.kt` | Main entry point - handles navigation, shared state, app routing |
| `AndroidManifest.xml` | Declares permissions (INTERNET, CAMERA) and app components |

### Network Layer

| File | Purpose |
|------|---------|
| `ApiClient.kt` | Configures Retrofit with Moshi, OkHttp interceptors, base URL, token injection |
| `AuthApi.kt` | Retrofit interface for `/auth/*` endpoints |
| `WalletApi.kt` | Retrofit interface for `/api/v1/wallets/*` endpoints |

### Data Models

| File | Purpose |
|------|---------|
| `AuthModels.kt` | SignupRequest, LoginRequest, LoginStep1Response, LoginConfirmResponse, etc. |
| `WalletModels.kt` | WalletDto, WalletCreateRequest, WalletTransferRequest, etc. |
| `TopUpModels.kt` | TopUpRequest, TopUpResponse, TopUpVerifyRequest, etc. |
| `TransactionPayloadQR.kt` | Transaction QR payload (Parcelable) - txId, payerId, payeeId, amount, etc. |
| `ReceiverQRPayload.kt` | Payee QR payload (Parcelable) - payeeId, payeeName, deviceId, maxTransactionLimit |
| `TransactionIntent.kt` | Transaction intent data model |
| `OfflineTransactionPayload.kt` | Offline transaction payload structure |

### Repository Layer

| File | Purpose |
|------|---------|
| `AuthRepository.kt` | Handles signup, login, email verification, token refresh, offline login |
| `WalletRepository.kt` | Handles wallet CRUD, transfers, local storage, transaction management |

### Local Database (Room)

| File | Purpose |
|------|---------|
| `AppDatabase.kt` | Room database definition, DAO access |
| `OfflineUser.kt` | Offline user entity (userId, email, name, phone, etc.) |
| `OfflineUserDao.kt` | User data access operations |
| `OfflineWallet.kt` | Offline wallet entity (walletId, balance, keys, etc.) |
| `OfflineWalletDao.kt` | Wallet data access, balance updates |
| `OfflineTransaction.kt` | Local transaction entity (txId, payerId, payeeId, amount, direction) |
| `OfflineTransactionDao.kt` | Transaction queries (sent, received, all) |

### UI Components

| File | Purpose |
|------|---------|
| `LoginScreen.kt` | Login UI with email/password + OTP step, minimum delay for errors |
| `CreateAccountScreen.kt` | Signup UI with password validation + email verification |
| `WalletScreen.kt` | Main wallet dashboard showing balance, wallets, transfer form |
| `SendPaymentScreen.kt` | Complete send payment flow - scan QR, confirm payee, enter amount, generate QR, mark as sent |
| `TransactionListScreen.kt` | Transaction history - displays local and server transactions |
| `TopUpScreen.kt` | Wallet top-up with OTP verification |
| `QRCodeScreen.kt` | Display user's QR code with dynamic transaction limits |
| `TransactionQRScannerScreen.kt` | Scanner for receiver to scan sender's transaction QR |
| `TransactionReceivedScreen.kt` | Transaction receipt display after scanning |
| `AuthViewModel.kt` | Manages auth state, login/signup flow, session management |
| `WalletViewModel.kt` | Manages wallet state, refresh, transfers, history |
| `ProfileViewModel.kt` | Manages user profile data, offline balance |

### Session Management

| File | Purpose |
|------|---------|
| `AuthSessionManager.kt` | Stores/retrieves access token, refresh token, device fingerprint, user ID, email verification status |
| `DeviceFingerprintProvider.kt` | Generates and persists unique device ID for API calls |

### Utilities

| File | Purpose |
|------|---------|
| `QRCodeHelper.kt` | QR code generation/parsing, transaction payload creation, validation |
| `WalletLimits.kt` | Wallet limits (5000 PKR max), transaction limits, dynamic limit calculation |
| `CurrencyUtils.kt` | PKR currency formatting utilities |
| `NetworkUtils.kt` | Network connectivity detection (online/offline) |
| `BiometricAuthHelper.kt` | Biometric authentication for sensitive operations |
| `EncryptionHelper.kt` | Data encryption utilities for sensitive data |
| `PasswordUtils.kt` | Password validation and strength checking |

---

## ✅ Implementation Status

### ✅ Completed Features

- [x] **Authentication Flow**
  - User signup with password complexity validation
  - Email verification (OTP)
  - Two-step login (credentials → OTP → tokens)
  - Token refresh mechanism
  - Password visibility toggle
  - Real-time password requirement validation
  - Offline login support (for verified users)
  - Minimum delay before showing login errors (prevents confusion)

- [x] **Network Integration**
  - Retrofit + Moshi for API calls
  - Automatic token injection via OkHttp interceptor
  - Error handling for HTTP errors (401, 422, etc.)
  - Device fingerprinting for security
  - Network connectivity detection
  - Offline mode support

- [x] **Wallet Management**
  - List wallets from backend
  - Two-step wallet creation (request → verify with OTP)
  - Bank account verification
  - Transfer between wallets
  - Real-time balance updates
  - Wallet top-up with OTP verification
  - Offline wallet balance management

- [x] **Offline Transaction Features (FYP-1)**
  - QR code scanning for payee identity
  - QR code generation for receiving payments
  - Transaction QR code generation
  - Transaction QR code scanning (receiver side)
  - Payee confirmation dialog with transaction limits
  - Dynamic transaction limit calculation
  - Transaction amount validation
  - Local transaction storage (Room DB)
  - Transaction history display
  - Automatic wallet balance updates (sender & receiver)

- [x] **QR Code Features**
  - Payee identity QR generation with dynamic limits
  - Transaction QR generation
  - QR code scanning with ML Kit Barcode Scanning
  - Camera integration with CameraX
  - QR code validation
  - Balance-based QR generation restrictions

- [x] **Local Storage (Room Database)**
  - Offline user data storage
  - Offline wallet data storage
  - Local transaction storage
  - Transaction history queries
  - Wallet balance updates

- [x] **Security Features**
  - Device security enforcement (PIN/Pattern/Biometric)
  - Biometric authentication for transaction generation
  - Encrypted private key storage
  - Device fingerprinting
  - Session management

- [x] **UI/UX**
  - Material Design 3 components
  - Navigation drawer
  - Loading states and error messages
  - Responsive layouts
  - Scrollable screens
  - Transaction flow with step-by-step guidance
  - Real-time validation feedback

### 🚧 Planned for FYP-2

- [ ] **BLE Integration**
  - Bluetooth Low Energy proximity detection
  - BLE acknowledgment handshake
  - Automatic transaction finalization
  - Two-phase commit system

- [ ] **Cryptographic Enhancements**
  - Transaction payload signatures
  - Signature verification
  - Hash-chained local ledger
  - Tamper-proof transaction records

- [ ] **Advanced Security**
  - Certificate pinning for API calls
  - Enhanced key storage (Android Keystore)
  - Key rotation mechanism
  - Replay-prevention engine

- [ ] **Sync Features**
  - Automatic server synchronization
  - Conflict resolution
  - Transaction reconciliation
  - Deferred sync protocol

---

## 🔌 Backend Integration

### API Endpoints Used

#### Public Endpoints
- `POST /auth/signup` - Create account
- `POST /auth/verify-email` - Verify email with OTP
- `POST /auth/login` - Login step 1 (get OTP)
- `POST /auth/login/confirm` - Login step 2 (confirm OTP, get tokens)
- `POST /auth/token/refresh` - Refresh access token
- `POST /auth/logout` - Logout (revoke token)
- `GET /auth/me` - Get current user info

#### Protected Endpoints (Require Bearer Token)

**Wallet Management**:
- `POST /api/v1/wallets/create-request` - Request wallet creation (sends OTP)
- `POST /api/v1/wallets/create-verify` - Verify OTP and create wallet
- `GET /api/v1/wallets/` - List wallets
- `GET /api/v1/wallets/{id}` - Get wallet details
- `POST /api/v1/wallets/transfer` - Transfer between wallets
- `GET /api/v1/wallets/transfers/history` - Transfer history
- `GET /api/v1/wallets/{id}/private-key` - Get wallet private key
- `POST /api/v1/wallets/topup` - Request wallet top-up (sends OTP)
- `POST /api/v1/wallets/topup/verify` - Verify top-up OTP and update balance

### Authentication Flow

1. **Signup**:
   ```
   User fills form → POST /auth/signup → Server returns otp_demo
   → User enters OTP → POST /auth/verify-email → Email verified
   ```

2. **Login**:
   ```
   User enters email/password → POST /auth/login → Server returns nonce_demo + otp_demo
   → User enters OTP → POST /auth/login/confirm → Server returns access_token + refresh_token
   → Tokens stored in AuthSessionManager
   ```

3. **Authenticated Requests**:
   ```
   All protected endpoints automatically include:
   - Header: Authorization: Bearer <access_token>
   - Header: x-device-fingerprint: <device_id>
   ```

### Error Handling

The app handles HTTP errors gracefully:
- **401 Unauthorized**: Shows "Invalid credentials" or "Session expired"
- **422 Unprocessable**: Shows validation errors (e.g., password complexity)
- **Network errors**: Shows connection error messages

---

## 🧪 Testing

### Running Tests

```bash
# Unit tests
./gradlew test

# Instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest
```

### Manual Testing Checklist

- [ ] Signup with valid password (10+ chars, complexity)
- [ ] Email verification with OTP
- [ ] Login with correct credentials
- [ ] OTP confirmation step
- [ ] Wallet creation
- [ ] Wallet listing
- [ ] Wallet transfer
- [ ] Error handling (invalid credentials, network errors)

---

## 🛠️ Development Guidelines

### Code Style

- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add KDoc comments for public functions
- Keep functions focused and single-purpose

### Architecture Pattern

The app follows **MVVM (Model-View-ViewModel)** architecture:

- **Model**: Data classes, repositories, network layer
- **View**: Jetpack Compose UI screens
- **ViewModel**: State management, business logic

### Adding New Features

1. **New API Endpoint**:
   - Add interface method in `AuthApi.kt` or `WalletApi.kt`
   - Add request/response models in `AuthModels.kt` or `WalletModels.kt`
   - Add repository method in `AuthRepository.kt` or `WalletRepository.kt`
   - Add ViewModel method if needed
   - Wire up UI

2. **New Screen**:
   - Create Composable in `ui/` directory
   - Create ViewModel if state management needed
   - Add navigation route in `MainActivity.kt`

---

## 🔐 Security Notes

### Current Implementation

- ✅ HTTPS enforced (Render provides TLS)
- ✅ Device fingerprinting for session security
- ✅ JWT token-based authentication
- ✅ Password complexity validation
- ✅ Secure password input (hidden by default)

### Production Considerations

- ⚠️ **TODO**: Encrypt tokens in local storage (currently in-memory)
- ⚠️ **TODO**: Implement certificate pinning
- ⚠️ **TODO**: Add biometric authentication for sensitive operations
- ⚠️ **TODO**: Secure key storage for offline wallet keys

---

## 📝 Environment Configuration

### Build Configuration

The app uses `BuildConfig` for configuration:

```kotlin
// app/build.gradle.kts
buildConfigField("String", "API_BASE_URL", "\"https://offline-payment-system-android.onrender.com/\"")
```

### Permissions

Required permissions (declared in `AndroidManifest.xml`):
- `INTERNET` - For API calls

Future permissions (for offline features):
- `CAMERA` - For QR code scanning
- `BLUETOOTH` - For BLE transfers
- `ACCESS_FINE_LOCATION` - For BLE (Android 12+)

---

## 🐛 Troubleshooting

### Common Issues

1. **Gradle sync fails**:
   - Check internet connection
   - Clear Gradle cache: `./gradlew clean`
   - Invalidate caches in Android Studio: File → Invalidate Caches

2. **Build fails with "Unresolved reference"**:
   - Sync project: File → Sync Project with Gradle Files
   - Rebuild: Build → Rebuild Project

3. **App crashes on network request**:
   - Check `INTERNET` permission in `AndroidManifest.xml`
   - Verify device has internet connectivity
   - Check API base URL in `build.gradle.kts`

4. **401 Unauthorized errors**:
   - Verify user is logged in (check `AuthSessionManager`)
   - Check if token expired (implement refresh if needed)
   - Verify device fingerprint matches

5. **Server connection timeouts**:
   - Ensure you've woken up the Render server (see "Server & Database Wake-Up" section)
   - First request after inactivity may take 30-60 seconds
   - Check server status: https://offline-payment-system-android-f8hr.onrender.com/health

---

## 🔧 Version Management & Avoiding Mismatches

### Gradle Wrapper - Ensuring Consistent Builds

To prevent version mismatch issues across team members, we use **Gradle Wrapper** instead of system-installed Gradle.

**Current Gradle Version**: `9.0-milestone-1` (specified in `gradle/wrapper/gradle-wrapper.properties`)

#### How It Works

1. **Gradle Wrapper** (`gradlew` / `gradlew.bat`):
   - Ensures everyone uses the **exact same Gradle version**
   - Automatically downloads the correct version on first run
   - No manual Gradle installation needed
   - Version is pinned in `gradle/wrapper/gradle-wrapper.properties`

2. **Always use Gradle Wrapper** (never use system Gradle):
   ```bash
   # ✅ Correct - uses wrapper (ensures correct version)
   ./gradlew build
   ./gradlew clean
   ./gradlew installDebug
   
   # ❌ Wrong - may use different Gradle version
   gradle build
   ```

3. **On first clone**:
   - Android Studio will automatically use the wrapper
   - Gradle will download the correct version automatically
   - Wait for "Gradle sync finished" message

#### Version Requirements

| Component | Version | Location |
|-----------|---------|----------|
| Gradle | `9.0-milestone-1` | `gradle/wrapper/gradle-wrapper.properties` |
| Android Gradle Plugin | See `build.gradle.kts` | `app/build.gradle.kts` |
| Kotlin | See `build.gradle.kts` | `app/build.gradle.kts` |
| JDK | 17 | Android Studio default (verify in Project Structure) |
| Android SDK | API 24+ | Installed via Android Studio SDK Manager |

#### Checking Your Gradle Version

```bash
./gradlew --version
```

This will show the Gradle version being used. It should match the version in `gradle/wrapper/gradle-wrapper.properties`.

#### If You Encounter Version Issues

1. **Clear Gradle cache**:
   ```bash
   ./gradlew clean
   ```

2. **Invalidate Android Studio caches**:
   - File → Invalidate Caches → Invalidate and Restart

3. **Re-sync project**:
   - File → Sync Project with Gradle Files

4. **Verify wrapper is being used**:
   - Check that `gradlew` (or `gradlew.bat` on Windows) exists in project root
   - Always use `./gradlew` commands, never `gradle` directly

---

## 📚 Dependencies

Key dependencies (see `app/build.gradle.kts` for full list):

- **Jetpack Compose**: UI framework
- **Retrofit 2.11.0**: HTTP client
- **Moshi 1.15.1**: JSON serialization
- **OkHttp 5.0.0**: HTTP client with interceptors
- **Coroutines 1.9.0**: Async operations
- **DataStore 1.1.1**: Preferences storage

---

## 👥 Team Collaboration

### Git Workflow

- Create feature branches: `git checkout -b feature/your-feature-name`
- Commit frequently with clear messages
- Push to your branch and create PR for review
- Ensure all tests pass before merging

### Code Review Checklist

- [ ] Code follows Kotlin conventions
- [ ] No hardcoded credentials or secrets
- [ ] Error handling implemented
- [ ] UI is responsive and handles edge cases
- [ ] Network calls are properly handled
- [ ] No memory leaks (check ViewModel scopes)

---

## 📞 Support & Resources

- **Backend API Docs**: See `../API_DOCUMENTATION.md` in backend directory
- **Requirements**: See `Offline-Payment-System-Requirements.md`
- **Backend Server**: https://offline-payment-system-android-f8hr.onrender.com
- **Health Check**: https://offline-payment-system-android-f8hr.onrender.com/health

---

## 🎯 Current Features (FYP-1)

### Authentication & User Management
- ✅ User registration with email verification
- ✅ Two-step login (credentials → OTP → tokens)
- ✅ Offline login for verified users
- ✅ Password complexity validation
- ✅ Token refresh mechanism
- ✅ Session management with user ID tracking
- ✅ Minimum delay before showing login errors

### Wallet Management
- ✅ Two-step wallet creation (request → verify with OTP)
- ✅ Bank account verification
- ✅ Wallet listing and details
- ✅ Transfer between wallets
- ✅ Wallet top-up with OTP verification
- ✅ Real-time balance updates
- ✅ Offline wallet balance management

### Offline Transaction Flow (QR-Based)
- ✅ **Step 1**: Receiver generates identity QR with dynamic transaction limits
- ✅ **Step 2**: Sender scans payee QR and confirms payee information
- ✅ **Step 3**: Sender enters transaction amount with validation
- ✅ **Step 4**: Sender generates transaction QR code
- ✅ **Step 5**: Receiver scans transaction QR and accepts payment
- ✅ **Step 6**: Sender clicks "Sent" button to finalize transaction
- ✅ **Step 7**: Both devices log transaction locally and update balances

### QR Code Features
- ✅ Payee identity QR generation with dynamic limits
- ✅ Transaction QR generation
- ✅ QR code scanning with ML Kit Barcode Scanning
- ✅ Camera integration with CameraX
- ✅ QR code validation (timestamp, amount, payee ID)
- ✅ Balance-based QR generation restrictions

### Local Storage & Offline Support
- ✅ Room Database for offline data storage
- ✅ Offline user data caching
- ✅ Offline wallet data caching
- ✅ Local transaction storage
- ✅ Transaction history queries
- ✅ Automatic wallet balance updates

### Security Features
- ✅ Device security enforcement (PIN/Pattern/Biometric)
- ✅ Biometric authentication for transaction generation
- ✅ Encrypted private key storage
- ✅ Device fingerprinting
- ✅ Session management with JWT tokens

### Transaction Management
- ✅ Transaction validation (timestamp, amount, payee ID)
- ✅ Dynamic transaction limit calculation
- ✅ Balance validation (sender & receiver)
- ✅ Transaction history display
- ✅ Local transaction storage (SENT/RECEIVED)
- ✅ Automatic balance updates after transactions

---

## 📅 Recent Changes

### December 2025 (FYP-1 Completion)
- ✅ Implemented complete offline transaction flow
- ✅ Added QR code scanning and generation
- ✅ Implemented local transaction storage (Room DB)
- ✅ Added transaction history screen
- ✅ Implemented automatic wallet balance updates
- ✅ Added payee confirmation dialog with transaction limits
- ✅ Added dynamic transaction limit calculation
- ✅ Fixed login race condition issues
- ✅ Added minimum delay before showing login errors
- ✅ Fixed blank screen issues after QR scanning
- ✅ Implemented shared state for reliable data passing
- ✅ Added scrollable screens for better UX
- ✅ Added "Sent" button for transaction finalization

### November 2025
- ✅ Connected Android app to Render backend
- ✅ Implemented signup/login flow with OTP
- ✅ Added wallet listing and transfer functionality
- ✅ Added password visibility toggles
- ✅ Added real-time password requirement validation
- ✅ Fixed HTTP error handling (401, 422)
- ✅ Added INTERNET and CAMERA permissions

---

## 🏗️ Architecture

### MVVM Pattern

The app follows **MVVM (Model-View-ViewModel)** architecture:

- **Model**: Data classes, Room entities, network models
- **View**: Jetpack Compose UI screens
- **ViewModel**: State management, business logic, repository calls

### Data Flow

```
UI (Compose) → ViewModel → Repository → Network/Local DB
                ↓
            State Flow
                ↓
            UI Updates
```

### Key Components

1. **Data Layer**:
   - Room Database for local storage
   - Retrofit for API calls
   - Repositories for business logic

2. **Domain Layer**:
   - ViewModels for state management
   - Use cases for business operations
   - Validation logic

3. **UI Layer**:
   - Jetpack Compose screens
   - Navigation Component
   - Material Design 3 components

---

## 🔐 Security Implementation

### Current Security Features

1. **Device Security**:
   - Mandatory device lock (PIN/Pattern/Biometric)
   - Biometric authentication for sensitive operations
   - Device fingerprinting

2. **Authentication**:
   - JWT token-based authentication
   - Refresh token mechanism
   - Device-bound sessions

3. **Data Protection**:
   - Encrypted private key storage
   - Secure session management
   - Local database encryption (Room)

4. **Transaction Security**:
   - Timestamp validation (±2 minutes)
   - Amount validation
   - Payee ID verification
   - Balance checks

### Future Security Enhancements (FYP-2)

- Certificate pinning
- Enhanced key storage (Android Keystore)
- Cryptographic transaction signing
- Hash-chained ledger
- BLE proximity verification

---

**Last Updated**: December 2025  
**Version**: 1.0.0 (FYP-1)  
**Maintainer**: Development Team

