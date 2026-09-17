package com.ustad.personalassistant.finalagent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinalAgentModelsTest {
    @Test fun normalizerMapsHindiWhatsAppPhrase() {
        assertEquals("open whatsapp", IntentNormalizer.normalize("  WhatsApp   kholo  "))
    }

    @Test fun normalizerMapsBatteryVariants() {
        assertEquals("battery status", IntentNormalizer.normalize("Battery kitni hai?"))
    }

    @Test fun cancellationAndConfirmationAreExplicit() {
        assertTrue(IntentNormalizer.isCancellation("Cancel"))
        assertTrue(IntentNormalizer.isExplicitConfirmation("haan"))
        assertFalse(IntentNormalizer.isExplicitConfirmation("maybe"))
    }

    @Test fun registryPreventsDuplicateExecution() {
        val registry = ActionRequestRegistry()
        assertTrue(registry.putPending("r1"))
        assertFalse(registry.putPending("r1"))
        assertTrue(registry.beginExecution("r1"))
        assertFalse(registry.beginExecution("r1"))
        registry.complete("r1")
        assertEquals(ActionRequestRegistry.State.COMPLETED, registry.state("r1"))
    }

    @Test fun registryCancellationPreventsExecution() {
        val registry = ActionRequestRegistry()
        assertTrue(registry.putPending("r2"))
        assertTrue(registry.cancel("r2"))
        assertFalse(registry.beginExecution("r2"))
        assertEquals(ActionRequestRegistry.State.CANCELLED, registry.state("r2"))
    }
}
