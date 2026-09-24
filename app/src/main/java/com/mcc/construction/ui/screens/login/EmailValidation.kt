package com.mcc.construction.ui.screens.login

/**
 * A light check that the text is shaped like an email address — something@
 * something.something. It catches typos before a round trip to the server; the
 * server remains the judge of whether the account exists.
 */
internal fun isPlausibleEmail(text: String): Boolean = EMAIL_SHAPE.matches(text.trim())

private val EMAIL_SHAPE = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
