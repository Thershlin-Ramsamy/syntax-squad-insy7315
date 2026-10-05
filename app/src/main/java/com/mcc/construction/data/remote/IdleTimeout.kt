package com.mcc.construction.data.remote

object IdleTimeout {
    fun expired(
        now: Long,
        lastActivity: Long,
        minutes: Int
    ): Boolean {
        require(minutes > 0)

        return now < lastActivity ||
                now - lastActivity >= minutes * 60_000L
    }
}