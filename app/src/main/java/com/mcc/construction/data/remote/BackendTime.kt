package com.mcc.construction.data.remote

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

object BackendTime {

    private val applicationZone =
        ZoneId.of("Africa/Johannesburg")

    fun date(value: String): LocalDate {
        return LocalDate.parse(value)
    }

    fun timestamp(value: String): LocalDateTime {
        return OffsetDateTime.parse(value)
            .atZoneSameInstant(applicationZone)
            .toLocalDateTime()
    }
}