# AI Call Assistant — Project Status

## Planned 8-part build

1. Android foundation, profile and permissions — IMPLEMENTED
2. Native incoming-call engine and 20-second auto-answer — IMPLEMENTED IN SOURCE; PHYSICAL TEST PENDING
3. Background call handling, lock-screen controls and JOIN CALL — NOT STARTED
4. Live STT -> LLM -> TTS voice pipeline — NOT STARTED
5. API Manager for Brain/STT/TTS providers, secure keys and testing — NOT STARTED
6. Call intelligence and structured extraction — NOT STARTED
7. Centered call summary and call history — NOT STARTED
8. Full integration, device testing, hardening and release — NOT STARTED

## Part 2 delivered

- Native Android InCallService is registered for managed cellular calls.
- The app can request the Android ROLE_DIALER role.
- ACTION_DIAL is declared for the default-phone-app requirement.
- Incoming ringing calls are captured by onCallAdded.
- A 20-second timer is started for each ringing call.
- If the user does not answer before the timer expires, the call is answered with audio-only state.
- Manual Answer and Decline controls are provided.
- Incoming-call status notification is posted.
- Call state is cleared when the call is removed.
- SMS/message-reading functionality remains absent.

## Important scope boundary

Part 2 does NOT claim that the AI can hear or speak to the caller. It only establishes the Android Telecom call-control layer. STT, LLM, TTS and live call audio belong to Part 4.

## Verification status

No physical SIM/device test has been completed in this environment.

Part 2 must be physically tested on a real supported Android phone for:
- saved contact caller
- unknown caller
- incoming ringing event
- manual answer
- manual decline
- 20-second auto-answer
- call removal/cleanup
- locked screen behavior
- OEM/Android-version differences

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

Android documentation requires a default phone app to handle ACTION_DIAL and fully implement InCallService for incoming and ongoing call UI. Part 2 adds the incoming-call side; Part 3 will extend this for lock-screen/background controls and user takeover. Emergency calls continue to use the preloaded dialer.

## Completion rule

When Part 8 is finished, produce a remaining-work audit that clearly separates:
- completed and tested
- completed but not physically tested
- blocked by Android/device/carrier limitations
- still remaining
