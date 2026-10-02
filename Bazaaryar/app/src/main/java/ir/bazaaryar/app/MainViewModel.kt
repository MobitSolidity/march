package ir.bazaaryar.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.bazaaryar.app.data.Coin
import ir.bazaaryar.app.data.CoinRepository
import ir.bazaaryar.app.data.EVENTS
import ir.bazaaryar.app.data.Prefs
import ir.bazaaryar.app.notify.Reminders
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class MarketState(
    val coins: List<Coin> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val updatedAt: Long = 0L,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)
    private val _market = MutableStateFlow(MarketState())
    val market: StateFlow<MarketState> = _market.asStateFlow()

    val watch = prefs.watch.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val alarms = prefs.alarms.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private var refreshJob: Job? = null

    init {
        Reminders.ensureChannel(app)
        // Restore saved reminders in case the system dropped them (reboot, force-stop, update).
        viewModelScope.launch { Reminders.rescheduleAll(app, prefs.alarms.first()) }
        viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(60_000) // free API: refresh every minute
            }
        }
    }

    /** Ignored while a request is already in flight, so the refresh button can't stack calls. */
    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _market.update { it.copy(loading = true) }
            runCatching { CoinRepository.fetchTop100() }
                .onSuccess { list -> _market.update { MarketState(list, false, null, System.currentTimeMillis()) } }
                .onFailure { e -> _market.update { it.copy(loading = false, error = e.message ?: "خطای شبکه") } }
        }
    }

    fun toggleWatch(id: String) { viewModelScope.launch { prefs.toggleWatch(id) } }

    fun toggleAlarm(id: String) { viewModelScope.launch {
        val event = EVENTS.firstOrNull { it.id == id } ?: return@launch
        val on = prefs.toggleAlarm(id)
        if (on) Reminders.schedule(getApplication(), event) else Reminders.cancel(getApplication(), event)
    } }
}
