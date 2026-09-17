# USTAD Personal AI Assistant

Part 01 establishes the real native Android foundation for the private personal assistant.

## Stack

- Kotlin
- Jetpack Compose + Material 3
- Lifecycle-aware state with StateFlow
- Coroutines
- DataStore for non-secret preferences
- Android Keystore-backed encrypted configuration storage
- Modular permission, security, repository and service contracts

## Part 01 scope

Implemented:

- Native Android application shell
- Home, Permission Center and Settings screens
- Verified runtime permission status for microphone, camera, contacts, location and phone calls
- Android Settings verification for Notification Access and Accessibility Access
- Launchable-app capability discovery
- Future connection states for Gmail and Google Account
- Security and ProtectedAppPolicy extension points
- Guarded action-execution boundary
- Lightweight logging and common error model
- Android Keystore-backed secure config storage
- GitHub Actions build + unit-test workflow

Intentionally not implemented: AI provider routing, WhatsApp/Gmail automation, wake word, voice authentication, call automation, Accessibility automation, financial-app control, lock-screen bypass, and hidden background microphone operation.

Future parts should extend these contracts rather than rebuild the foundation.
