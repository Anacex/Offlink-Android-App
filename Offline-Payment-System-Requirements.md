# Offline Payment System – Unified Project and Requirements Guide

---

## Introduction & Project Context

This project delivers a **secure offline digital payment system** for Pakistan’s financial ecosystem, enabling transactions **without continuous internet connectivity** while meeting the highest standards of cybersecurity and regulatory compliance. The objective is to bridge gaps in digital financial inclusion caused by frequent network outages and limited device capability, especially for rural merchants and payers.

**Primary Use Case:**  
A rural merchant, operating in a low-connectivity environment, can accept payment from a customer with a mobile Android wallet app. The transaction payload is transferred via QR code or Bluetooth offline, cryptographically signed, and locally logged. When either party later connects to the internet, all pending transactions are synchronized and finalized with the central backend, preventing double-spending and replay attacks through strong security protocols.

This guide serves as the **central reference for all developers (backend, frontend, DevOps, QA)** and project contributors. It integrates existing code, documentation, and security model, and is aligned with Pakistan’s national digital finance goals and compliance standards.

---

## Backend (FastAPI) – Technical Requirements & Features

### Technology Stack

- Python 3.10+
- FastAPI – web framework
- SQLAlchemy – ORM
- psycopg2-binary – PostgreSQL adapter
- Pydantic – data validation
- Uvicorn – ASGI server
- Docker – containerization
- Redis (optional) – caching
- `.env` – secrets/configuration

### API Endpoints

| Endpoint                                 | Purpose                             |
|-------------------------------------------|-------------------------------------|
| POST /auth/signup                        | Register account                    |
| POST /auth/verify-email                  | Email OTP verification              |
| POST /auth/login                         | Login (step 1)                      |
| POST /auth/login/confirm                 | Confirm MFA                         |
| POST /auth/token/refresh                 | Token renewal                       |
| POST /auth/logout                        | Log out                             |
| POST /api/v1/offline-transactions/verify-receipt | Verify offline TX receipt      |
| POST /api/v1/wallets/                    | Create wallet                       |
| GET /api/v1/wallets/                     | List wallets                        |
| POST /api/v1/wallets/transfer            | Wallet-to-wallet transfer           |
| GET /api/v1/wallets/transfers/history    | Transfer history                    |
| POST /api/v1/wallets/qr-code             | QR code generation                  |
| GET /api/v1/wallets/{id}/private-key     | Retrieve private key                |
| POST /api/v1/offline-transactions/create-local | Create offline transaction     |
| POST /api/v1/offline-transactions/sign-and-store | Sign & store offline tx        |
| POST /api/v1/offline-transactions/sync   | Sync all offline transactions       |
| POST /api/v1/offline-transactions/{id}/confirm | Confirm offline transaction    |

### Security & Compliance Requirements

- **RSA 2048**, RSA-PSS + SHA-256 signatures
- **bcrypt** (cost factor ≥12) password hashing
- **JWT HS256** for tokens (refresh/access)
- Strong password policies (≥10 chars, complexity)
- Multi-factor auth (email OTP)
- TLS/SSL enforced for all API calls
- Device fingerprinting and secure session management
- Unique nonce per transaction (replay prevention)
- Double-spend protection (local + server)
- Rate limiting (slowapi)
- SQL Injection/XSS/CSRF protection
- Certificate pinning for mobile integration
- Secure logging, monitoring, and backups
- **Compliance:** SBP guidelines, Electronic Transactions Ordinance 2002, PECA 2016, GDPR, PCI DSS

### DevOps & Testing

- Unit tests (`pytest -m unit -q`)
- CI/CD pipeline (secrets management)
- API documentation on Swagger/OpenAPI
- Docker, Nginx, and systemd deployment scripts
- Environment variable templates
- Database schema migrations and backup strategy

### Database Schema

| Table Name           | Purpose                      |
|----------------------|-----------------------------|
| users                | User accounts               |
| wallets              | Online & offline wallets    |
| wallet_transfers     | Wallet transfers            |
| offline_transactions | Offline transaction logs    |
| transactions         | Online transactions         |
| refresh_tokens       | Token/session management    |

---

## Frontend (Kotlin Android) – Technical Requirements & Features

### Technology Stack

- Kotlin (primary language)
- Android SDK 30+
- Jetpack Components (Room/SQLite, ViewModel, Navigation)
- Android Keystore/TEE – secure key storage
- ZXing – QR scanning
- Biometric Auth (fingerprint/face unlock)
- Encrypted Room DB (SQLCipher)

### Core Features

- **Wallet Generation & Management**
  - Ed25519 cryptographic keypair at onboarding
  - Offline balance/account storage (encrypted)
  - Top-up wallet via backend API
- **Transaction Creation & Signing**
  - Payment entry and transaction signing
  - Transactions stored in hash-chain (auditability)
  - Payload transfer via QR code or BLE (Bluetooth Low Energy)
- **Transaction Reception & Verification**
  - Receive offline payload (QR/BLE), verify signature
  - Local ledger update and transaction status/receipt
- **Synchronization**
  - Deferred sync of local transactions to backend when online
  - Real-time sync feedback and balance reconciliation
- **Security**
  - Biometric or PIN required for wallet operations
  - Encrypt all local transaction data, attestation before offline spend

### UX & Testing

- Easy status indication for offline/online usage
- Transaction history and clear sync/confirmation
- UI tests (Espresso, JUnit)
- Export/import backup for pending transactions

### Compliance

- Uninstall warnings (potential wallet loss)
- Enforcement of offline transaction limits (configurable)
- Audit logging and compliance checks per transaction

---

## Development Roles & Contribution Guidelines

- PR workflow with mandatory unit test pass
- Up-to-date documentation (START_HERE.md, DOCUMENTATION_INDEX.md)
- Small, focused commits and feature branches
- Issues reported via GitHub with reproducible logs/tests
- Archive obsolete docs in `archive_docs/` for team continuity

---

## Example Rural Merchant Offline Flow

1. Payer generates and signs a transaction via mobile app, transferring payload as QR or BLE.
2. Merchant receives and verifies signature, appends locally.
3. Both parties sync pending transaction to backend once online.
4. Backend validates, confirms, and updates wallet balances for both.
5. Double-spend/replay attempts automatically rejected and logged.

---

## Future Enhancements

- Blockchain integration for immutable ledger
- Multi-signature, post-quantum cryptography
- WebSocket for real-time sync/updates
- Cross-border payments, decentralized ID (DID)
- ML-powered fraud/anomaly detection

---

**This guide should be referenced and updated as your central source of truth for all contributors and future maintainers.**
