package ir.bazaaryar.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.bazaaryar.app.data.EVENTS
import ir.bazaaryar.app.data.PriceHub
import ir.bazaaryar.app.notify.PriceAlerts
import ir.bazaaryar.app.ui.AlertsScreen
import ir.bazaaryar.app.ui.BazaaryarTheme
import ir.bazaaryar.app.ui.C
import ir.bazaaryar.app.ui.CoinSheet
import ir.bazaaryar.app.ui.MarketScreen
import ir.bazaaryar.app.ui.NewsScreen
import ir.bazaaryar.app.ui.WatchScreen
import ir.bazaaryar.app.ui.faTime
import ir.bazaaryar.app.ui.rememberNow

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 15+ forces edge-to-edge for targetSdk >= 35: opt in everywhere so layout is identical on all versions.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(0xFFF8FBFC.toInt(), 0xFFF8FBFC.toInt()),
        )
        super.onCreate(savedInstanceState)
        // Full-speed live data only while the app is visible; the background service keeps a lighter stream.
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { PriceHub.acquire(applicationContext, ui = true) }
            override fun onStop(owner: LifecycleOwner) { PriceHub.release(ui = true) }
        })
        if (savedInstanceState == null) openFromIntent(intent)
        setContent {
            BazaaryarTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AskNotificationPermission()
                    AppRoot(vm)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openFromIntent(intent)
    }

    /** Tapping a price alert opens that coin's sheet. */
    private fun openFromIntent(i: Intent?) {
        i?.getStringExtra(PriceAlerts.EXTRA_COIN)?.let { vm.select(it) }
    }
}

/** Asks once, and only if not already granted (no repeat prompt on every rotation). */
@Composable
private fun AskNotificationPermission() {
    if (Build.VERSION.SDK_INT < 33) return
    val context = LocalContext.current
    var asked by rememberSaveable { mutableIntStateOf(0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted && asked == 0) {
            asked = 1
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

private data class Tab(val label: String, val icon: ImageVector)
private val TABS = listOf(
    Tab("اخبار", Icons.Filled.Event),
    Tab("بازار", Icons.Filled.BarChart),
    Tab("واچ‌لیست", Icons.Filled.Star),
    Tab("هشدارها", Icons.Filled.NotificationsActive),
)

@Composable
private fun AppRoot(vm: MainViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(1) }
    val market by vm.market.collectAsStateWithLifecycle()
    val watch by vm.watch.collectAsStateWithLifecycle()
    val alarms by vm.alarms.collectAsStateWithLifecycle()
    val toman by vm.toman.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val rules by vm.rules.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val now = rememberNow()
    val rate = market.usdt?.toman

    Scaffold(
        containerColor = C.Paper,
        topBar = {
            Row(
                Modifier.fillMaxWidth().background(C.Deep).statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("بازاریار", color = C.Paper, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                // Currency toggle: show every coin in USD or in Toman (via live USDT/IRT rate).
                Text(
                    if (toman) "تومان" else "دلار",
                    color = C.Deep, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 10.dp).clip(RoundedCornerShape(50)).background(C.OnDeepMuted)
                        .clickable { vm.setToman(!toman) }.padding(horizontal = 12.dp, vertical = 4.dp),
                )
                Text("${faTime(now)} تهران", color = C.OnDeepMuted, fontSize = 14.sp)
            }
        },
        bottomBar = {
            NavigationBar(containerColor = C.Card) {
                TABS.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i, onClick = { tab = i },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = C.Lead, selectedTextColor = C.Lead),
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when (tab) {
                0 -> NewsScreen(EVENTS, now, alarms, vm::toggleAlarm)
                1 -> MarketScreen(market, watch, toman, vm::toggleWatch, vm::refresh, vm::select)
                2 -> WatchScreen(
                    market.coins, watch, EVENTS, alarms, now, toman, rate,
                    vm::toggleWatch, vm::toggleAlarm, vm::select,
                ) { tab = it }
                else -> {
                    val s = settings
                    if (s != null) AlertsScreen(s, rules, market.coins, vm::updateSettings, vm::removeRule, vm::select, vm::testAlert)
                }
            }
        }
    }

    val coin = selected?.let { id -> market.coins.firstOrNull { it.id == id } }
    if (coin != null) {
        CoinSheet(
            c = coin,
            toman = toman,
            rate = rate,
            starred = coin.id in watch,
            rule = rules[coin.id],
            defaultPct = settings?.thresholdPct ?: 5f,
            session = PriceHub.history.series(coin.id),
            onToggleStar = { vm.toggleWatch(coin.id) },
            onSaveRule = vm::saveRule,
            onDismiss = { vm.select(null) },
        )
    }
}
