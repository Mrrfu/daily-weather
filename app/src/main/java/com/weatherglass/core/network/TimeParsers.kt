package com.weatherglass.core.network

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun parseIsoTimeToEpochSec(value: String): Long {
    return runCatching {
        OffsetDateTime.parse(value).toEpochSecond()
    }.getOrElse { System.currentTimeMillis() / 1000 }
}

fun parseDateToEpochSec(value: String): Long {
    return runCatching {
        val date = LocalDate.parse(value, DateTimeFormatter.ISO_DATE)
        date.atStartOfDay().toEpochSecond(ZoneOffset.UTC)
    }.getOrElse { System.currentTimeMillis() / 1000 }
}
