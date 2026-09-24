package com.mcc.construction.data.model

/**
 * The weather choices on a daily report. [storedValue] is what goes into
 * `DAILY_REPORTS.Weather` — a fixed value for the database, not display text;
 * the UI shows its own translated label.
 */
enum class Weather(val storedValue: String) {
    SUNNY("Sunny"),
    CLOUDY("Cloudy"),
    RAINY("Rainy"),
    WINDY("Windy"),
    HOT("Hot"),
    COLD("Cold")
}
