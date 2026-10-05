package com.mcc.construction.security

class IdleSessionTimer {

    private var accountId: Int? = null
    private var lastActivityMillis: Long? = null
    private var expired = false

    @Synchronized
    fun attach(userId: Int, nowMillis: Long) {
        if (accountId != userId) {
            accountId = userId
            lastActivityMillis = nowMillis
            expired = false
        }
    }

    @Synchronized
    fun clear() {
        accountId = null
        lastActivityMillis = null
        expired = false
    }

    @Synchronized
    fun isExpired(
        nowMillis: Long,
        timeoutMinutes: Int
    ): Boolean {
        require(timeoutMinutes > 0)

        if (accountId == null) return false
        if (expired) return true

        val last = lastActivityMillis ?: return false
        val elapsed = nowMillis - last
        val timeout = timeoutMinutes.toLong() * 60_000L

        if (elapsed < 0 || elapsed >= timeout) {
            expired = true
        }

        return expired
    }

    /**
     * An interaction after expiry cannot revive the session.
     */
    @Synchronized
    fun recordActivity(
        nowMillis: Long,
        timeoutMinutes: Int
    ): Boolean {
        if (accountId == null) return true
        if (isExpired(nowMillis, timeoutMinutes)) return false

        lastActivityMillis = nowMillis
        return true
    }
}