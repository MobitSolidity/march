package ir.bazaaryar.app.data

import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Upcoming macro releases, Tehran time (UTC+03:30, no DST).
 * Sources: Federal Reserve calendar, BLS release schedule. Update this list monthly,
 * or replace it with a calendar API of your choice (see README).
 */
private val TEHRAN = ZoneOffset.ofHoursMinutes(3, 30)
private fun t(s: String): Long = LocalDateTime.parse(s).atOffset(TEHRAN).toInstant().toEpochMilli()

val EVENTS: List<EconEvent> = listOf(
    EconEvent("nfp", t("2026-10-02T16:00:00"), "US", "اشتغال بخش غیرکشاورزی (NFP)", 3, en = "Nonfarm Payrolls (NFP)"),
    EconEvent("unemp", t("2026-10-02T16:00:00"), "US", "نرخ بیکاری", 3, en = "Unemployment Rate"),
    EconEvent("ism-s", t("2026-10-05T17:30:00"), "US", "شاخص مدیران خرید خدماتی ISM", 3, en = "ISM Services PMI"),
    EconEvent("trade", t("2026-10-06T16:00:00"), "US", "تراز تجاری", 1, en = "Trade Balance"),
    EconEvent("adp", t("2026-10-07T15:45:00"), "US", "اشتغال بخش خصوصی ADP", 2, en = "ADP Employment Change"),
    EconEvent("fomc-min", t("2026-10-07T21:30:00"), "US", "صورتجلسه فدرال رزرو (FOMC Minutes)", 3, en = "FOMC Meeting Minutes"),
    EconEvent("claims", t("2026-10-08T16:00:00"), "US", "مدعیان بیمه بیکاری", 2, previous = "197K", en = "Initial Jobless Claims"),
    EconEvent("umich", t("2026-10-09T17:30:00"), "US", "اطمینان مصرف‌کننده میشیگان (اولیه)", 2, previous = "51.7", forecast = "54.1", en = "UMich Consumer Sentiment (Prelim)"),
    EconEvent("cpi", t("2026-10-14T16:00:00"), "US", "شاخص قیمت مصرف‌کننده (CPI) سپتامبر", 3, en = "CPI (September)"),
    EconEvent("beige", t("2026-10-14T21:30:00"), "US", "گزارش کتاب بژ فدرال رزرو", 2, en = "Fed Beige Book"),
    EconEvent("ppi", t("2026-10-15T16:00:00"), "US", "شاخص قیمت تولیدکننده (PPI)", 2, en = "Producer Price Index (PPI)"),
    EconEvent("indpro", t("2026-10-16T16:45:00"), "US", "تولید صنعتی", 1, en = "Industrial Production"),
    EconEvent("fomc", t("2026-10-28T21:30:00"), "US", "تصمیم نرخ بهره فدرال رزرو", 3, previous = "3.75–4.00%", en = "Fed Interest Rate Decision"),
    EconEvent("presser", t("2026-10-28T22:00:00"), "US", "کنفرانس خبری رئیس فدرال رزرو", 3, en = "Fed Chair Press Conference"),
    EconEvent("pce", t("2026-10-29T16:00:00"), "US", "شاخص هسته PCE", 3, en = "Core PCE Price Index"),
).sortedBy { it.at }

val FLAGS = mapOf("US" to "🇺🇸", "EU" to "🇪🇺", "CN" to "🇨🇳", "JP" to "🇯🇵", "GB" to "🇬🇧")
