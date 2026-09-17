# USTAD Personal AI Assistant

Native Kotlin + Jetpack Compose personal assistant foundation built incrementally across Parts 01–04.

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

## Part 04 — Voice Engine + Hindi STT/TTS
- One central `VoiceEngine` owns the application voice entry point.
- Deterministic voice session states: idle, permission, starting, listening, processing, speaking, stopping and error.
- Android microphone permission is checked through the existing `CapabilityEngine` / `PermissionManager` before listening.
- Android `SpeechRecognizer` provider with Hindi/Hinglish/English/Mixed language selection, partial/final transcripts and offline preference when the network is unavailable.
- `SpeechToTextManager` supports provider priority, preferred provider, language matching, health, cooldown and bounded retries/failover.
- Deepgram and AssemblyAI are represented as configurable provider slots with secure credential references and clean provider-specific streaming hooks; no secret is hard-coded.
- `TextToSpeechManager` supports configurable cloud TTS slots plus Android native TTS fallback.
- ElevenLabs is represented as a configurable TTS provider slot with secure API-key storage, model, voice, endpoint and priority configuration.
- Android TTS dynamically inspects available voices/locales and safely falls back when a Hindi voice is unavailable.
- Speech rate, pitch, preferred voice and auto-speak settings are persisted in the existing `ustad_settings` DataStore.
- Audio focus is requested only for assistant speech and released after interruption/completion.
- User interruption stops TTS before a new listening session; duplicate listening sessions are ignored.
- `CALL_CONVERSATION_MODE` isolation prevents caller speech from entering the phone-control action pipeline.
- Final transcripts pass through the existing Part 03 AI automation path; VoiceEngine does not bypass capability, security, protected-app or confirmation gates.
- No permanent raw-audio storage and no audio content logging.

## Safety / intentionally deferred
No lock-screen bypass, biometric/PIN interception, covert microphone operation, financial-app automation, private WhatsApp database scraping, encryption bypass, wake-word engine, real voice-authentication model, incoming-call automation, full Gmail/WhatsApp workflows, device diagnostics, or final autonomous orchestrator are implemented here. Part 04 only provides the voice authentication and call-mode interfaces needed by later parts.

Cloud credentials are never hard-coded or logged. Voice provider secrets use the existing Android Keystore-backed configuration store. Provider pricing is never invented.

## Build validation
GitHub Actions is configured to compile the Android project and run unit tests. A local Android build is not claimed unless it was actually executed successfully.
