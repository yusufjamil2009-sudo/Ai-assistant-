# AI Call Assistant — Project Status

## Planned 8-part build

1. Android foundation, profile and permissions — IMPLEMENTED
2. Native incoming-call engine and 20-second auto-answer — IMPLEMENTED IN SOURCE; PHYSICAL TEST PENDING
3. Background call handling, lock-screen controls and JOIN CALL — IMPLEMENTED IN SOURCE; PHYSICAL TEST PENDING
4. Live STT -> LLM -> TTS voice pipeline — NOT STARTED
5. API Manager for Brain/STT/TTS providers, secure keys and testing — NOT STARTED
6. Call intelligence and structured extraction — NOT STARTED
7. Centered call summary and call history — NOT STARTED
8. Full integration, device testing, hardening and release — NOT STARTED

## Part 3 delivered

- Active call controls are available from a notification-launched activity.
- The incoming-call activity is configured to appear over the lock screen and wake the screen.
- Active-call notification remains ongoing so the user can return to call controls without opening the main app.
- JOIN CALL control is wired to the active Telecom Call.
- LISTEN is represented as a call-control state for the later AI audio layer.
- MUTE/UNMUTE control is wired to InCallService microphone mute.
- END CALL disconnects the active Telecom Call.
- Call session state is cleaned when Telecom removes the call.
- SMS/message-reading functionality remains absent.

## Important scope boundary

Part 3 provides real Telecom call control and UI/background entry points. It does NOT yet provide a separate AI audio bridge that can listen to caller audio or inject synthesized speech. That belongs to Part 4 and must be implemented with supported Android call-audio APIs and tested on physical devices.

## Verification status

No physical SIM/device test has been completed in this environment.

Part 3 must be physically tested on a real supported Android phone for:
- locked screen incoming call
- screen-off incoming call
- notification tap while locked
- JOIN CALL takeover
- LISTEN control state
- MUTE/UNMUTE
- END CALL
- manual answer before timeout
- auto-answer after 20 seconds
- notification persistence/cleanup
- OEM/Android-version behavior

## Requirements that must remain

- Do not add SMS/message-reading functionality.
- Saved contacts and unknown callers must both be supported.
- Requested default: after 20 seconds without manual answer, AI attempts to handle the call.
- User must be able to take over with JOIN CALL without opening the main app.
- AI handling should continue with screen locked/screen off where Android/device/carrier rules allow it.
- Profile name controls the AI introduction.
- Male/female voice and language are selectable.
- API Manager must support provider keys, connection tests, secure storage, primary/backup selection and failure handling.
- Call summary must contain caller identity/number, purpose, what caller said, what AI said, important points, duration and date/time.
- Summary is a centered card, not a full-screen takeover, with a close button.

## Critical Android architecture note

Android's InCallService API provides call lifecycle and call-control callbacks. Current Android documentation deprecates the older audio-route API at API 34 in favor of CallEndpoint APIs. Part 3 therefore keeps the call-control layer separate from the future AI audio bridge instead of pretending that a normal microphone recorder can transparently become the cellular call audio path.

## Completion rule

When Part 8 is finished, produce a remaining-work audit that clearly separates:
- completed and tested
- completed but not physically tested
- blocked by Android/device/carrier limitations
- still remaining
