package com.mcc.construction.data.repository

/**
 * Application-authored feedback that is safe to show to a user.
 *
 * Never construct this exception from raw server exception text.
 */
class UserFacingException(
    val userMessage: String
) : IllegalStateException(userMessage)