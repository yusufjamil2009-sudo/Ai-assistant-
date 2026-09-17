package com.ustad.personalassistant.voice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceAuthenticationPolicyTest {
    @Test fun authorizedNormalSessionMayEnterControlPipeline() { assertTrue(VoiceAuthenticationPolicy.mayEnterControlPipeline(VoiceSessionType.NORMAL_ASSISTANT_SESSION, VoiceAuthenticationResult.AUTHORIZED)) }
    @Test fun unauthorizedVoiceIsBlocked() { assertFalse(VoiceAuthenticationPolicy.mayEnterControlPipeline(VoiceSessionType.NORMAL_ASSISTANT_SESSION, VoiceAuthenticationResult.UNAUTHORIZED)) }
    @Test fun callerSessionCanNeverEnterControlPipeline() { assertFalse(VoiceAuthenticationPolicy.mayEnterControlPipeline(VoiceSessionType.CALL_CONVERSATION_SESSION, VoiceAuthenticationResult.AUTHORIZED)) }
}
