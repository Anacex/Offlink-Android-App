# Offline Payment System - Android App

A secure Android application for offline digital payments, enabling transactions without continuous internet connectivity while maintaining the highest standards of cybersecurity and regulatory compliance.

## 📱 Project Overview

This Android app is part of the **Offline Payment System** project, designed to enable secure offline transactions for Pakistan's financial ecosystem. The app allows users to:

- Create accounts and authenticate securely
- Manage online and offline wallets
- Transfer funds between wallets
- Perform offline transactions (via QR codes)
- Sync transactions when connectivity is restored

**Backend Server:** [https://offline-payment-system-android.onrender.com](https://offline-payment-system-android.onrender.com)

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
   - The base URL is configured in `app/build.gradle.kts`:
     ```kotlin
     buildConfigField("String", "API_BASE_URL", "\"https://offline-payment-system-android.onrender.com/\"")
     ```
   - For local development, change this to your local server URL

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
   - Visit: https://offline-payment-system-android.onrender.com/health
   - Wait 30-60 seconds for the server to fully start
   - You should see a JSON response: `{"status": "healthy"}`

2. **Wake up Supabase database**:
   - The database will automatically wake up when the server makes its first connection
   - If you see connection errors, wait a bit longer and try again

3. **Test connectivity**:
   ```bash
   curl https://offline-payment-system-android.onrender.com/health
   ```

**Note**: The first API call after inactivity may take 30-60 seconds. Subsequent calls will be fast until the next sleep period.

---

## 📁 Project Structure

```
Android-App/
├── app/
│   ├── build.gradle.kts          # App-level Gradle configuration
│   ├── proguard-rules.pro        # ProGuard rules for release builds
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml    # App permissions and components
│           ├── java/com/offlinepayment/
│           │   ├── MainActivity.kt           # Main entry point
│           │   ├── OfflinePaymentApp.kt     # Application class
│           │   │
│           │   ├── data/                     # Data layer
│           │   │   ├── AuthModels.kt         # Auth request/response DTOs
│           │   │   ├── WalletModels.kt       # Wallet request/response DTOs
│           │   │   ├── network/              # Network layer
│           │   │   │   ├── ApiClient.kt      # Retrofit client setup
│           │   │   │   ├── AuthApi.kt         # Auth endpoints interface
│           │   │   │   └── WalletApi.kt      # Wallet endpoints interface
│           │   │   ├── repository/           # Repository layer
│           │   │   │   ├── AuthRepository.kt # Auth business logic
│           │   │   │   └── WalletRepository.kt # Wallet business logic
│           │   │   └── session/              # Session management
│           │   │       ├── AuthSessionManager.kt      # Token storage
│           │   │       └── DeviceFingerprintProvider.kt # Device ID
│           │   │
│           │   ├── ui/                        # UI layer (Jetpack Compose)
│           │   │   ├── auth/                  # Authentication screens
│           │   │   │   ├── AuthViewModel.kt
│           │   │   │   ├── LoginScreen.kt
│           │   │   │   └── CreateAccountScreen.kt
│           │   │   ├── wallet/                # Wallet screens
│           │   │   │   └── WalletViewModel.kt
│           │   │   ├── WalletScreen.kt        # Main wallet UI
│           │   │   ├── SendPaymentScreen.kt
│           │   │   ├── TransactionListScreen.kt
│           │   │   ├── TopUpScreen.kt
│           │   │   ├── profile/
│           │   │   │   └── ProfileScreen.kt
│           │   │   ├── qr/
│           │   │   │   └── QRCodeScreen.kt
│           │   │   └── theme/
│           │   │       └── Theme.kt
│           │   │
│           │   └── utils/                     # Utility classes
│           │       ├── CurrencyUtils.kt       # PKR formatting
│           │       └── QRCodeHelper.kt        # QR code generation
│           │
│           └── res/                            # Resources
│               ├── values/
│               │   ├── strings.xml            # String resources
│               │   ├── colors.xml            # Color definitions
│               │   └── themes.xml             # Material theme
│               └── drawable/                  # Icons and graphics
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
| `MainActivity.kt` | Main entry point - handles navigation and app state |
| `AndroidManifest.xml` | Declares permissions (INTERNET) and app components |

### Network Layer

| File | Purpose |
|------|---------|
| `ApiClient.kt` | Configures Retrofit with Moshi, OkHttp interceptors, base URL |
| `AuthApi.kt` | Retrofit interface for `/auth/*` endpoints |
| `WalletApi.kt` | Retrofit interface for `/api/v1/wallets/*` endpoints |

### Data Models

| File | Purpose |
|------|---------|
| `AuthModels.kt` | SignupRequest, LoginRequest, LoginStep1Response, LoginConfirmResponse, etc. |
| `WalletModels.kt` | WalletDto, WalletCreateRequest, WalletTransferRequest, etc. |

### Repository Layer

| File | Purpose |
|------|---------|
| `AuthRepository.kt` | Handles signup, login, email verification, token refresh |
| `WalletRepository.kt` | Handles wallet CRUD, transfers, listing |

### UI Components

| File | Purpose |
|------|---------|
| `LoginScreen.kt` | Login UI with email/password + OTP step |
| `CreateAccountScreen.kt` | Signup UI with password validation + email verification |
| `WalletScreen.kt` | Main wallet dashboard showing balance, wallets, transfer form |
| `AuthViewModel.kt` | Manages auth state, login/signup flow |
| `WalletViewModel.kt` | Manages wallet state, refresh, transfers |

### Session Management

| File | Purpose |
|------|---------|
| `AuthSessionManager.kt` | Stores/retrieves access token, refresh token, device fingerprint |
| `DeviceFingerprintProvider.kt` | Generates and persists unique device ID for API calls |

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

- [x] **Network Integration**
  - Retrofit + Moshi for API calls
  - Automatic token injection via OkHttp interceptor
  - Error handling for HTTP errors (401, 422, etc.)
  - Device fingerprinting for security

- [x] **Wallet Management**
  - List wallets from backend
  - Create wallets (online/offline)
  - Transfer between wallets
  - Real-time balance updates

- [x] **UI/UX**
  - Material Design 3 components
  - Navigation drawer
  - Loading states and error messages
  - Responsive layouts

### 🚧 In Progress / TODO

- [ ] **Offline Transaction Features**
  - QR code scanning for payments
  - Offline transaction creation and signing
  - Local transaction storage (Room DB)
  - Sync mechanism when online

- [ ] **Security Enhancements**
  - Encrypted local storage for tokens
  - Biometric authentication
  - Certificate pinning for API calls
  - Secure key storage (Android Keystore)

- [ ] **Additional Features**
  - Transaction history from backend
  - QR code generation for receiving payments
  - Bluetooth Low Energy (BLE) for offline transfers
  - Push notifications for transaction updates

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

#### Protected Endpoints (Require Bearer Token)
- `POST /api/v1/wallets/` - Create wallet
- `GET /api/v1/wallets/` - List wallets
- `GET /api/v1/wallets/{id}` - Get wallet details
- `POST /api/v1/wallets/transfer` - Transfer between wallets
- `GET /api/v1/wallets/transfers/history` - Transfer history

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
   - Check server status: https://offline-payment-system-android.onrender.com/health

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
- **Backend Server**: https://offline-payment-system-android.onrender.com
- **Health Check**: https://offline-payment-system-android.onrender.com/health

---

## 📅 Recent Changes

### 2025-11-21
- ✅ Connected Android app to Render backend
- ✅ Implemented signup/login flow with OTP
- ✅ Added wallet listing and transfer functionality
- ✅ Added password visibility toggles
- ✅ Added real-time password requirement validation
- ✅ Fixed HTTP error handling (401, 422)
- ✅ Added INTERNET permission

---

**Last Updated**: November 21, 2025  
**Version**: 1.0.0  
**Maintainer**: Development Team

