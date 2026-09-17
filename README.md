# USTAD Personal AI Assistant

Part 02 extends the existing native Android foundation with a centralized, real-state Permission & Capability Engine.

## Stack

- Kotlin
- Jetpack Compose + Material 3
- Lifecycle-aware state with StateFlow
- Coroutines
- DataStore for non-secret preferences
- Android Keystore-backed encrypted configuration storage
- Modular permission, capability, security, repository and service contracts

## Part 02 scope

Implemented on top of Part 01:

- Centralized `CapabilityEngine` and reusable `CapabilityGate`
- Real Android runtime permission verification for microphone, camera, contacts, location and phone calls
- Real Notification Listener and Accessibility Service state detection
- Android Settings launchers with resume-time refresh
- Permanent-denial handling that can open the app's Android settings page
- Open Apps capability based on package-manager launchability, with no fake runtime permission
- Modern photo/file picker capability contracts without broad storage access
- Background assistant capability foundation with foreground-service and battery-state checks
- Secure voice-authentication enrollment/enable-state foundation without storing raw voice recordings
- Gmail and Google Account OAuth connection interfaces without hard-coded secrets or passwords
- Capability-aware action execution boundary before security/protected-app checks
- Structured permission, OAuth, capability and security error states
- Expanded unit tests for capability availability, action gating and protected-app blocking
- Lightweight futuristic Permission Center presentation while preserving mobile-first behavior

## Intentionally not implemented

AI provider routing, Gemini, OpenRouter, Groq, Mistral, SambaNova, Zhipu, voice provider integrations, ElevenLabs, Deepgram, AssemblyAI, wake word, real voice-authentication model, WhatsApp/Messenger automation, Gmail message operations, complete call assistant, automatic call answering, advanced Accessibility automation, device diagnostics, financial-app automation, lock-screen bypass, and covert background microphone operation.

Future parts must extend these contracts rather than rebuild, duplicate, migrate or replace the foundation.
