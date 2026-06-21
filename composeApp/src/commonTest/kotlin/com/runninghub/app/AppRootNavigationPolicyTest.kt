package com.runninghub.app

import com.runninghub.feature.auth.domain.SessionState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppRootNavigationPolicyTest {

    @Test
    fun `restoring session does not create root screen`() {
        val decision = rootNavigationDecisionFor(SessionState.Restoring)

        assertNull(decision.target)
        assertFalse(decision.resetExpiredSession)
    }

    @Test
    fun `authenticated session replaces root with main stack`() {
        val decision = rootNavigationDecisionFor(SessionState.Authenticated)

        assertEquals(RootScreenTarget.Main, decision.target)
        assertFalse(decision.resetExpiredSession)
    }

    @Test
    fun `logout state replaces root with login stack without consuming expiration`() {
        val decision = rootNavigationDecisionFor(SessionState.Unauthenticated)

        assertEquals(RootScreenTarget.Login, decision.target)
        assertFalse(decision.resetExpiredSession)
    }

    @Test
    fun `expired session replaces root with login stack and resets expiration flag`() {
        val decision = rootNavigationDecisionFor(SessionState.Expired)

        assertEquals(RootScreenTarget.Login, decision.target)
        assertTrue(decision.resetExpiredSession)
    }
}
