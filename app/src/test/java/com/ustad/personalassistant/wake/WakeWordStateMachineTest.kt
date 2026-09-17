package com.ustad.personalassistant.wake

import org.junit.Assert.assertTrue
import org.junit.Test

class WakeWordStateMachineTest {
    @Test fun transitionsFromWakeToCommandProcessing() {
        val machine = WakeWordStateMachine()
        assertTrue(machine.start()); assertTrue(machine.wakeDetected()); assertTrue(machine.commandListening()); assertTrue(machine.processing()); assertTrue(machine.speaking()); assertTrue(machine.idle())
        assertTrue(machine.state == WakeWordState.IDLE)
    }
}
