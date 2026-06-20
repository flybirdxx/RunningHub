package com.runninghub.shared.domain.session

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SessionManagerTest {

    @Test
    fun `restore marks session authenticated when local credential exists`() = runBlocking {
        val manager = SessionManager(FakeSessionRestoreRepository(hasSession = true))

        manager.restore()

        assertEquals(SessionState.Authenticated, manager.state.value)
        assertFalse(manager.isExpired.value)
    }

    @Test
    fun `restore marks session unauthenticated when local credential is missing`() = runBlocking {
        val manager = SessionManager(FakeSessionRestoreRepository(hasSession = false))

        manager.restore()

        assertEquals(SessionState.Unauthenticated, manager.state.value)
        assertFalse(manager.isExpired.value)
    }

    @Test
    fun `restore falls back to unauthenticated when local read fails`() = runBlocking {
        val manager = SessionManager(FailingSessionRestoreRepository)

        manager.restore()

        assertEquals(SessionState.Unauthenticated, manager.state.value)
        assertFalse(manager.isExpired.value)
    }

    @Test
    fun `session can be marked expired`() {
        val manager = SessionManager()

        assertFalse(manager.isExpired.value)

        manager.expire()

        assertEquals(SessionState.Expired, manager.state.value)
        assertTrue(manager.isExpired.value)
    }

    @Test
    fun `resetExpiration clears previous expired state`() {
        val manager = SessionManager()
        manager.expire()

        manager.resetExpiration()

        assertEquals(SessionState.Unauthenticated, manager.state.value)
        assertFalse(manager.isExpired.value)
    }

    @Test
    fun `markAuthenticated clears expired state and exposes authenticated session`() {
        val manager = SessionManager()
        manager.expire()

        manager.markAuthenticated()

        assertEquals(SessionState.Authenticated, manager.state.value)
        assertFalse(manager.isExpired.value)
    }

    @Test
    fun `logout clears authenticated session state`() {
        val manager = SessionManager()
        manager.markAuthenticated()

        manager.logout()

        assertEquals(SessionState.Unauthenticated, manager.state.value)
        assertFalse(manager.isExpired.value)
    }

    private class FakeSessionRestoreRepository(
        private val hasSession: Boolean,
    ) : SessionRestoreRepository {
        override suspend fun hasRestorableSession(): Boolean = hasSession
    }

    private object FailingSessionRestoreRepository : SessionRestoreRepository {
        override suspend fun hasRestorableSession(): Boolean =
            error("credential read failed")
    }
}
