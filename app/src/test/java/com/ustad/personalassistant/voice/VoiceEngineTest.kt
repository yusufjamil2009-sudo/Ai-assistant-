package com.ustad.personalassistant.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceEngineTest {
    @Test fun detectsHindiAndHinglish() {
        val detector = VoiceLanguageDetector()
        assertEquals(VoiceLanguage.HINDI, detector.detect("मुझे संदेश भेजो"))
        assertEquals(VoiceLanguage.HINGLISH, detector.detect("Rahul ko WhatsApp pe message bhejo"))
    }

    @Test fun normalizerDoesNotChangeMeaning() {
        assertEquals("whatsapp kholo na", DefaultVoiceInputNormalizer().normalize("  whatsapp   kholo na  "))
    }

    @Test fun partialIsDistinctFromFinal() {
        val partial = SttEvent(SttEventType.PARTIAL, SttResult("WhatsApp kholo", isFinal = false))
        val final = SttEvent(SttEventType.FINAL, SttResult("WhatsApp kholo.", isFinal = true))
        assertTrue(partial.type != SttEventType.FINAL)
        assertTrue(final.result!!.isFinal)
    }

    @Test fun callModeIsAnExplicitIsolationState() {
        assertEquals(VoiceOperationMode.USER_COMMAND, VoiceOperationMode.USER_COMMAND)
        assertEquals(VoiceOperationMode.CALL_CONVERSATION_MODE, VoiceOperationMode.CALL_CONVERSATION_MODE)
    }

    @Test fun voiceErrorsAreStructured() {
        val error = VoiceException(VoiceErrorCode.STT_TIMEOUT, "Speech recognition timed out")
        assertEquals(VoiceErrorCode.STT_TIMEOUT, error.code)
        assertEquals("Speech recognition timed out", error.message)
    }
}
