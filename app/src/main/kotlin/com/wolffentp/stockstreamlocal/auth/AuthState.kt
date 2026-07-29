package com.wolffentp.stockstreamlocal.auth

sealed class AuthState {
    /** App requires lock screen before navigation. */
    object Locked : AuthState()
    /** User has been authenticated for this session. */
    object Authenticated : AuthState()
    /** No PIN has been configured; lock screen is skipped on first launch. */
    object NoPinConfigured : AuthState()
    /** PIN entry in progress. */
    data class PinEntry(val attemptCount: Int = 0, val errorMessage: String? = null) : AuthState()
}
