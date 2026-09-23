package com.yusufjamil.aicallassistant

/**
 * Lightweight process-local bridge used only for Part 3 call controls.
 * Part 4 will replace this with the live voice/AI session controller.
 */
object AiCallServiceHolder {
    @Volatile
    var service: AiInCallService? = null
}
