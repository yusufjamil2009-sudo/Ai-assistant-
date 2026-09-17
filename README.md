# USTAD Personal AI Assistant

Native Kotlin + Jetpack Compose personal assistant foundation built incrementally across Parts 01–03.

## Part 01 — Core foundation
- Modular AI, voice, command, permission, action, security and protected-app contracts.
- Persistent settings and Android Keystore-backed secure configuration.
- Real Android capability/permission state; no fake permission toggles.
- Security-first action boundary and Protected Apps policy.

## Part 02 — Capability + safe automation foundation
- Central `CapabilityEngine` / `CapabilityGate`.
- Android Accessibility action engine with semantic-node operations, verification and timeouts.
- Automation policy and protected-app firewall.
- WhatsApp notification/intent/accessibility integration hooks without private database scraping or encryption bypass.
- Gmail/Google OAuth contracts and safe confirmation policies.
- Financial-app automation, authentication interception and lock-screen bypass remain blocked.

## Part 03 — AI brain + central API manager
- Central `AiBrain`, structured `AiRequest` / `AiResponse`, intent/entity/action-plan models.
- On-device AI state contract with safe unavailable fallback when no local runtime is present.
- Configurable Gemini, OpenRouter, Groq, Mistral, SambaNova and Zhipu provider slots.
- Central `ApiManager` with priority routing, capability matching, retry/backoff/jitter, cooldown, timeout isolation, failover, offline handling and usage/health tracking.
- Keystore-backed API-key storage with masked UI-ready credential access.
- Persistent provider configuration and routing policy (`PRIVACY_FIRST`, `BALANCED`, `CLOUD_FIRST`).
- Generic HTTP provider adapter; provider-specific API mapping can be supplied without changing the router.
- Validated action plans never execute directly from model output; existing capability/security/protected-app pipeline remains the execution boundary.
- Conversation context is bounded and kept lightweight.

## Safety / intentionally deferred
No lock-screen bypass, biometric/PIN interception, covert microphone operation, financial-app automation, private WhatsApp database scraping, encryption bypass, wake-word engine, real voice-authentication model, Deepgram/AssemblyAI/ElevenLabs integration, incoming-call automation, or full Gmail/WhatsApp workflows are implemented by these foundations.

Cloud provider credentials are never hard-coded or logged. Costs are only recorded when a provider explicitly supplies usage/cost metadata; the app does not invent provider pricing.

Build validation is performed by GitHub Actions where available.
