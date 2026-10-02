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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.bazaaryar.app.data.EVENTS
import ir.bazaaryar.app.data.PriceHub
import ir.bazaaryar.app.notify.ClockRing
import ir.bazaaryar.app.notify.PriceAlerts
import ir.bazaaryar.app.ui.AlertsScreen
import ir.bazaaryar.app.ui.BazaaryarTheme
import ir.bazaaryar.app.ui.C
import ir.bazaaryar.app.ui.CoinSheet
import ir.bazaaryar.app.ui.L
import ir.bazaaryar.app.ui.Lang
import ir.bazaaryar.app.ui.MarketScreen
import ir.bazaaryar.app.ui.NewsScreen
import ir.bazaaryar.app.ui.ThemeMode
import ir.bazaaryar.app.ui.UpdatePrompt
import ir.bazaaryar.app.ui.WatchScreen
import ir.bazaaryar.app.ui.faTime
import ir.bazaaryar.app.ui.rememberNow
import ir.bazaaryar.app.ui.resolveDark
import ir.bazaaryar.app.ui.tr
import ir.bazaaryar.app.update.Updater

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                ClockRing.appVisible = true
                PriceHub.acquire(applicationContext, ui = true)
            }
            override fun onStop(owner: LifecycleOwner) {
                ClockRing.appVisible = false
                PriceHub.release(ui = true)
            }
        })
        if (savedInstanceState == null) openFromIntent(intent)
        setContent {
            val theme by vm.theme.collectAsStateWithLifecycle()
            val dark = theme.resolveDark()
            LaunchedEffect(dark) {
                this@MainActivity.enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                    navigationBarStyle = if (dark) SystemBarStyle.dark(0xFF141A2E.toInt()) else SystemBarStyle.light(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt()),
                )
            }
            BazaaryarTheme(theme) {
                CompositionLocalProvider(LocalLayoutDirection provides L.dir) {
                    AskNotificationPermission()
                    AppRoot(vm, theme)
                    UpdatePrompt()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); openFromIntent(intent) }
    private fun openFromIntent(i: Intent?) { i?.getStringExtra(PriceAlerts.EXTRA_COIN)?.let { vm.select(it) } }
}

@Composable
private fun AskNotificationPermission() {
    if (Build.VERSION.SDK_INT < 33) return
    val context = LocalContext.current
    var asked by rememberSaveable { mutableIntStateOf(0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!granted && asked == 0) { asked = 1; launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }
}

private class Tab(private val fa: String, private val en: String, val icon: ImageVector) { val label: String get() = tr(fa, en) }
private val TABS = listOf(
    Tab("اخبار", "News", Icons.Filled.Event), Tab("بازار", "Market", Icons.Filled.BarChart),
    Tab("واچ‌لیست", "Watchlist", Icons.Filled.Star), Tab("هشدارها", "Alerts", Icons.Filled.NotificationsActive),
)
private fun ThemeMode.next(): ThemeMode = when (this) { ThemeMode.SYSTEM -> ThemeMode.LIGHT; ThemeMode.LIGHT -> ThemeMode.DARK; ThemeMode.DARK -> ThemeMode.SYSTEM }
private fun ThemeMode.icon(): ImageVector = when (this) { ThemeMode.SYSTEM -> Icons.Filled.BrightnessAuto; ThemeMode.LIGHT -> Icons.Filled.LightMode; ThemeMode.DARK -> Icons.Filled.DarkMode }
private fun ThemeMode.label(): String = when (this) { ThemeMode.SYSTEM -> tr("تم: خودکار", "Theme: system"); ThemeMode.LIGHT -> tr("تم: روشن", "Theme: light"); ThemeMode.DARK -> tr("تم: تیره", "Theme: dark") }

@Composable
private fun AppRoot(vm: MainViewModel, theme: ThemeMode) {
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
    val chip = RoundedCornerShape(50)
    Scaffold(
        containerColor = C.Paper,
        topBar = {
            Row(Modifier.fillMaxWidth().background(C.headerBrush).statusBarsPadding().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("March", "March"), color = C.OnDeep, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(faTime(now), color = C.OnDeepMuted, fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp))
                Text(if (toman) tr("تومان", "Toman") else tr("دلار", "USD"), color = C.Deep, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 6.dp).clip(chip).background(C.OnDeepMuted).clickable { vm.setToman(!toman) }.padding(horizontal = 12.dp, vertical = 4.dp))
                Text(if (L.en) "فا" else "EN", color = C.OnDeep, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(chip).background(C.OnDeep.copy(alpha = 0.16f)).clickable { vm.setLang(if (L.en) Lang.FA else Lang.EN) }.padding(horizontal = 10.dp, vertical = 4.dp))
                IconButton(onClick = { vm.setTheme(theme.next()) }) { Icon(theme.icon(), contentDescription = theme.label(), tint = C.OnDeep, modifier = Modifier.size(22.dp)) }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = C.Card) { TABS.forEachIndexed { i, t -> NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Icon(t.icon, contentDescription = t.label) }, label = { Text(t.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = C.Lead, selectedTextColor = C.Lead, unselectedIconColor = C.Muted, unselectedTextColor = C.Muted, indicatorColor = C.Soft)) } }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when (tab) {
                0 -> NewsScreen(EVENTS, now, alarms, vm::toggleAlarm)
                1 -> MarketScreen(market, watch, toman, vm::toggleWatch, vm::refresh, vm::select)
                2 -> WatchScreen(market.coins, watch, EVENTS, alarms, now, toman, rate, vm::toggleWatch, vm::toggleAlarm, vm::select) { tab = it }
                else -> settings?.let { AlertsScreen(it, rules, market.coins, vm::updateSettings, vm::removeRule, vm::select, vm::testAlert, vm::testAlarm) }
            }
        }
    }
    val coin = selected?.let { id -> market.coins.firstOrNull { it.id == id } }
    if (coin != null) CoinSheet(c = coin, toman = toman, rate = rate, starred = coin.id in watch, rule = rules[coin.id], defaultPct = settings?.thresholdPct ?: 5f, session = PriceHub.history.series(coin.id), onToggleStar = { vm.toggleWatch(coin.id) }, onSaveRule = vm::saveRule, onDismiss = { vm.select(null) })
}
