# AI Call Assistant — Project Status

## Planned 8-part build

1. Android foundation, profile and permissions — IMPLEMENTED
2. Native incoming-call engine and 20-second auto-answer — NOT STARTED
3. Background call handling, lock-screen controls and JOIN CALL — NOT STARTED
4. Live STT -> LLM -> TTS voice pipeline — NOT STARTED
5. API Manager for Brain/STT/TTS providers, secure keys and testing — NOT STARTED
6. Call intelligence and structured extraction — NOT STARTED
7. Centered call summary and call history — NOT STARTED
8. Full integration, device testing, hardening and release — NOT STARTED

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

## Part 1 verification status

Source files have been added to the repository.

A physical Android device test has NOT been claimed yet. Later testing must explicitly cover:
- locked screen
- screen off
- saved contact
- unknown caller
- 20-second timeout
- AI answer
- JOIN CALL takeover
- call termination
- provider failure/fallback
- different Android versions/device vendors

## Critical Android architecture note

The production call engine must use Android Telecom APIs. Android documentation states that an app acting as the default phone app must handle ACTION_DIAL and fully implement InCallService for incoming and ongoing call UI. This will be addressed in Parts 2 and 3.

## Completion rule

When Part 8 is finished, produce a remaining-work audit that clearly separates:
- completed and tested
- completed but not physically tested
- blocked by Android/device/carrier limitations
- still remaining
