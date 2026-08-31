package com.ryuken.obsidianledger

import com.ryuken.obsidianledger.core.domain.repository.AuthSessionState
import kotlin.test.Test
import kotlin.test.assertEquals

class StartupRouteTest {

    @Test
    fun authenticated_routesToMain() {
        assertEquals(StartupRoute.Main, startupRoute(AuthSessionState.Authenticated("u1")))
    }

    @Test
    fun notAuthenticated_routesToAuth() {
        assertEquals(StartupRoute.Auth, startupRoute(AuthSessionState.NotAuthenticated))
    }

    @Test
    fun initialization_routesToAuth_defensively() {
        assertEquals(StartupRoute.Auth, startupRoute(AuthSessionState.Initializing))
    }

    @Test
    fun timeoutNull_routesToAuth() {
        assertEquals(StartupRoute.Auth, startupRoute(null))
    }
}
