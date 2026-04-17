package com.yourname.simplenotes.ui.auth

/**
 * Route constants for the authentication sub-graph.
 *
 * Centralised here so [AppNavigation] and any future deep-link handlers
 * share a single source of truth rather than scattered string literals.
 */
object AuthNavigation {
    const val ROUTE_AUTH = "auth"
    const val ROUTE_AUTH_SETUP = "auth_setup"
    const val ROUTE_PIN_ENTRY = "pin_entry"
}
