package com.runninghub.shared.di

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global singleton signaling session expiry.
 * Repository layer emits [expire] on unrecoverable 401.
 * App.kt observes [isExpired] and navigates to login.
 *
 * NOTE: Global singletons hinder test isolation. Consider making this a Koin-scoped
 * class for future testability (e.g., resetting state between tests requires manual reset()).
 */
object SessionExpiredHandler {

    private val _isExpired = MutableStateFlow(false)
    val isExpired: StateFlow<Boolean> = _isExpired.asStateFlow()

    fun expire() {
        _isExpired.value = true
    }

    fun reset() {
        _isExpired.value = false
    }
}
