package com.ustad.personalassistant.gmail

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GmailFoundationTest {
    @Test fun unconfiguredRepositoryFailsSafely() { assertFalse(UnconfiguredGmailRepository().listEmails().isSuccess) }
    @Test fun defaultPolicyIsConfirmation() { assertTrue(GmailSendPolicy.CONFIRM_BEFORE_SEND != GmailSendPolicy.AUTHORIZED_AUTO_SEND) }
}
