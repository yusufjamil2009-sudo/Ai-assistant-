package com.ustad.personalassistant.background

import org.junit.Assert.assertEquals
import org.junit.Test

class BackgroundAssistantStateTest {
    @Test fun exposesExpectedStates() { assertEquals(8, BackgroundAssistantState.entries.size); assertEquals(BackgroundAssistantState.DISABLED, BackgroundAssistantState.entries.first()) }
}
