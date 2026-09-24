package com.mcc.construction.data.fake

import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.time.Duration

/** A Wednesday morning in Johannesburg, so tests never depend on the real date. */
val FIXED_NOW: LocalDateTime = LocalDateTime.of(2026, 9, 23, 9, 30)

val fixedClock: Clock = Clock.fixed(
    FIXED_NOW.toInstant(ZoneOffset.ofHours(2)),
    ZoneId.of("Africa/Johannesburg")
)

/** A container with no simulated latency and a fixed clock. */
fun testContainer(): FakeAppContainer = FakeAppContainer(clock = fixedClock, latency = Duration.ZERO)
