package ir.bazaaryar.app.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import ir.bazaaryar.app.ui.Lang
import ir.bazaaryar.app.ui.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

// Store name kept as-is so existing users keep their settings after the rename to March.
private val Context.store by preferencesDataStore("bazaaryar")
private val WATCH = stringSetPreferencesKey("watch")
private val ALARMS = stringSetPreferencesKey("alarms")
private val TOMAN = booleanPreferencesKey("toman")
private val A_ENABLED = booleanPreferencesKey("alert_enabled")
private val A_PCT = floatPreferencesKey("alert_pct")
private val A_WINDOW = stringPreferencesKey("alert_window")
private val A_SCOPE = stringPreferencesKey("alert_scope")
private val A_DIR = stringPreferencesKey("alert_dir")
private val A_COOLDOWN = intPreferencesKey("alert_cooldown")
private val A_BG = booleanPreferencesKey("alert_bg")
private val A_USDT = booleanPreferencesKey("alert_usdt")
private val A_RING = stringPreferencesKey("alert_ring")
private val A_CLOCK = booleanPreferencesKey("alert_clock_app")
private val NAME = stringPreferencesKey("user_name")
private val RULES = stringPreferencesKey("coin_rules")
private val THEME = stringPreferencesKey("theme")
private val LANG = stringPreferencesKey("lang")

private val DEFAULT_WATCH = setOf("bitcoin", "ethereum", "solana", "ripple", USDT_IRT_ID)

/** Appearance: theme mode and app language. */
data class Look(val theme: ThemeMode = ThemeMode.SYSTEM, val lang: Lang = Lang.FA)

private inline fun <reified T : Enum<T>> enumOr(name: String?, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: fallback

private fun readSettings(p: Preferences) = AlertSettings(
    enabled = p[A_ENABLED] ?: true,
    thresholdPct = p[A_PCT] ?: 5f,
    window = enumOr(p[A_WINDOW], AlertWindow.M15),
    scope = enumOr(p[A_SCOPE], AlertScope.WATCHLIST),
    direction = enumOr(p[A_DIR], AlertDirection.BOTH),
    cooldownMin = p[A_COOLDOWN] ?: 30,
    background = p[A_BG] ?: true,
    includeUsdt = p[A_USDT] ?: true,
    name = p[NAME] ?: "",
    ring = enumOr(p[A_RING], RingMode.ALL),
    clockApp = p[A_CLOCK] ?: true,
)

private fun MutablePreferences.write(s: AlertSettings) {
    this[A_ENABLED] = s.enabled
    this[A_PCT] = s.thresholdPct
    this[A_WINDOW] = s.window.name
    this[A_SCOPE] = s.scope.name
    this[A_DIR] = s.direction.name
    this[A_COOLDOWN] = s.cooldownMin
    this[A_BG] = s.background
    this[A_USDT] = s.includeUsdt
    this[NAME] = s.name
    this[A_RING] = s.ring.name
    this[A_CLOCK] = s.clockApp
}

private fun JSONObject.optNum(k: String): Double? =
    if (has(k) && !isNull(k)) optDouble(k).takeIf { !it.isNaN() } else null

private fun decodeRules(raw: String?): Map<String, CoinRule> {
    if (raw.isNullOrBlank()) return emptyMap()
    return try {
        val o = JSONObject(raw)
        val out = LinkedHashMap<String, CoinRule>()
        for (id in o.keys()) {
            val r = o.optJSONObject(id) ?: continue
            val rule = CoinRule(id, r.optNum("pct")?.toFloat(), r.optNum("above"), r.optNum("below"))
            if (!rule.isEmpty) out[id] = rule
        }
        out
    } catch (_: Exception) {
        emptyMap()
    }
}

private fun encodeRules(m: Map<String, CoinRule>): String {
    val o = JSONObject()
    for (r in m.values) {
        val j = JSONObject()
        r.pct?.let { j.put("pct", it.toDouble()) }
        r.above?.let { j.put("above", it) }
        r.below?.let { j.put("below", it) }
        o.put(r.id, j)
    }
    return o.toString()
}

class Prefs(private val context: Context) {
    val watch: Flow<Set<String>> = context.store.data.map { it[WATCH] ?: DEFAULT_WATCH }
    val alarms: Flow<Set<String>> = context.store.data.map { it[ALARMS] ?: emptySet() }
    val toman: Flow<Boolean> = context.store.data.map { it[TOMAN] ?: false }
    val alertSettings: Flow<AlertSettings> = context.store.data.map { readSettings(it) }
    val rules: Flow<Map<String, CoinRule>> = context.store.data.map { decodeRules(it[RULES]) }
    val look: Flow<Look> = context.store.data.map {
        Look(enumOr(it[THEME], ThemeMode.SYSTEM), enumOr(it[LANG], Lang.FA))
    }

    suspend fun toggleWatch(id: String) {
        context.store.edit {
            val cur = it[WATCH] ?: DEFAULT_WATCH
            it[WATCH] = if (id in cur) cur - id else cur + id
        }
    }

    /** Returns true if the alarm is now on. */
    suspend fun toggleAlarm(id: String): Boolean {
        var on = false
        context.store.edit {
            val cur = it[ALARMS] ?: emptySet()
            on = id !in cur
            it[ALARMS] = if (on) cur + id else cur - id
        }
        return on
    }

    suspend fun setToman(on: Boolean) {
        context.store.edit { it[TOMAN] = on }
    }

    suspend fun setTheme(mode: ThemeMode) {
        context.store.edit { it[THEME] = mode.name }
    }

    suspend fun setLang(lang: Lang) {
        context.store.edit { it[LANG] = lang.name }
    }

    suspend fun updateSettings(change: (AlertSettings) -> AlertSettings) {
        context.store.edit { p -> p.write(change(readSettings(p))) }
    }

    /** Saves a per-coin rule; an empty rule removes it. */
    suspend fun setRule(rule: CoinRule) {
        context.store.edit { p ->
            val m = decodeRules(p[RULES]).toMutableMap()
            if (rule.isEmpty) {
                m.remove(rule.id)
            } else {
                m[rule.id] = rule
            }
            p[RULES] = encodeRules(m)
        }
    }

    /** Target alerts are one-shot: drop the target once it has fired. */
    suspend fun clearTarget(id: String, above: Boolean) {
        context.store.edit { p ->
            val m = decodeRules(p[RULES]).toMutableMap()
            val r = m[id] ?: return@edit
            val next = if (above) r.copy(above = null) else r.copy(below = null)
            if (next.isEmpty) {
                m.remove(id)
            } else {
                m[id] = next
            }
            p[RULES] = encodeRules(m)
        }
    }
}
