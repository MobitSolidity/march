package ir.bazaaryar.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.bazaaryar.app.data.AlertSettings
import ir.bazaaryar.app.data.CoinRule
import ir.bazaaryar.app.data.EVENTS
import ir.bazaaryar.app.data.MarketState
import ir.bazaaryar.app.data.Prefs
import ir.bazaaryar.app.data.PriceHub
import ir.bazaaryar.app.notify.LiveService
import ir.bazaaryar.app.notify.PriceAlerts
import ir.bazaaryar.app.notify.Reminders
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)

    /** Live prices. The hub runs only while the activity is started or the background service is on. */
    val market: StateFlow<MarketState> = PriceHub.state
    val watch = prefs.watch.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val alarms = prefs.alarms.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val toman = prefs.toman.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val settings: StateFlow<AlertSettings?> = prefs.alertSettings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val rules = prefs.rules.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _selected = MutableStateFlow<String?>(null)
    val selected: StateFlow<String?> = _selected.asStateFlow()

    init {
        Reminders.ensureChannel(app)
        PriceAlerts.ensureChannel(app)
        // Restore saved reminders in case the system dropped them (reboot, force-stop, update).
        viewModelScope.launch { Reminders.rescheduleAll(app, prefs.alarms.first()) }
        // Background live monitor follows the user's switch.
        viewModelScope.launch {
            prefs.alertSettings.map { it.background }.distinctUntilChanged().collect { on ->
                if (on) LiveService.start(app) else LiveService.stop(app)
            }
        }
    }

    fun refresh() = PriceHub.refreshNow()

    fun select(id: String?) { _selected.value = id }

    fun toggleWatch(id: String) { viewModelScope.launch { prefs.toggleWatch(id) } }

    fun setToman(on: Boolean) { viewModelScope.launch { prefs.setToman(on) } }

    fun updateSettings(change: (AlertSettings) -> AlertSettings) { viewModelScope.launch { prefs.updateSettings(change) } }

    fun saveRule(rule: CoinRule) { viewModelScope.launch { prefs.setRule(rule) } }

    fun removeRule(id: String) { viewModelScope.launch { prefs.setRule(CoinRule(id)) } }

    fun testAlert() { PriceAlerts.test(getApplication(), settings.value ?: AlertSettings()) }

    fun toggleAlarm(id: String) { viewModelScope.launch {
        val event = EVENTS.firstOrNull { it.id == id } ?: return@launch
        val on = prefs.toggleAlarm(id)
        if (on) Reminders.schedule(getApplication(), event) else Reminders.cancel(getApplication(), event)
    } }
}
