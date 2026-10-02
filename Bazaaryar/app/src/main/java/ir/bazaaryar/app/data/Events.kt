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
    EconEvent("nfp", t("2026-10-02T16:00:00"), "US", "اشتغال بخش غیرکشاورزی (NFP)", 3),
    EconEvent("unemp", t("2026-10-02T16:00:00"), "US", "نرخ بیکاری", 3),
    EconEvent("ism-s", t("2026-10-05T17:30:00"), "US", "شاخص مدیران خرید خدماتی ISM", 3),
    EconEvent("trade", t("2026-10-06T16:00:00"), "US", "تراز تجاری", 1),
    EconEvent("adp", t("2026-10-07T15:45:00"), "US", "اشتغال بخش خصوصی ADP", 2),
    EconEvent("fomc-min", t("2026-10-07T21:30:00"), "US", "صورتجلسه فدرال رزرو (FOMC Minutes)", 3),
    EconEvent("claims", t("2026-10-08T16:00:00"), "US", "مدعیان بیمه بیکاری", 2, previous = "197K"),
    EconEvent("umich", t("2026-10-09T17:30:00"), "US", "اطمینان مصرف‌کننده میشیگان (اولیه)", 2, previous = "51.7", forecast = "54.1"),
    EconEvent("cpi", t("2026-10-14T16:00:00"), "US", "شاخص قیمت مصرف‌کننده (CPI) سپتامبر", 3),
    EconEvent("beige", t("2026-10-14T21:30:00"), "US", "گزارش کتاب بژ فدرال رزرو", 2),
    EconEvent("ppi", t("2026-10-15T16:00:00"), "US", "شاخص قیمت تولیدکننده (PPI)", 2),
    EconEvent("indpro", t("2026-10-16T16:45:00"), "US", "تولید صنعتی", 1),
    EconEvent("fomc", t("2026-10-28T21:30:00"), "US", "تصمیم نرخ بهره فدرال رزرو", 3, previous = "3.75–4.00%"),
    EconEvent("presser", t("2026-10-28T22:00:00"), "US", "کنفرانس خبری رئیس فدرال رزرو", 3),
    EconEvent("pce", t("2026-10-29T16:00:00"), "US", "شاخص هسته PCE", 3),
).sortedBy { it.at }

val FLAGS = mapOf("US" to "🇺🇸", "EU" to "🇪🇺", "CN" to "🇨🇳", "JP" to "🇯🇵", "GB" to "🇬🇧")
