package ir.bazaaryar.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore("bazaaryar")
private val WATCH = stringSetPreferencesKey("watch")
private val ALARMS = stringSetPreferencesKey("alarms")

class Prefs(private val context: Context) {
    val watch: Flow<Set<String>> = context.store.data.map { it[WATCH] ?: setOf("bitcoin", "ethereum", "solana", "ripple") }
    val alarms: Flow<Set<String>> = context.store.data.map { it[ALARMS] ?: emptySet() }

    suspend fun toggleWatch(id: String) = context.store.edit {
        val cur = it[WATCH] ?: setOf("bitcoin", "ethereum", "solana", "ripple")
        it[WATCH] = if (id in cur) cur - id else cur + id
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
}
