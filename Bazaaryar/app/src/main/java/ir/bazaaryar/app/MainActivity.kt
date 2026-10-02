package ir.bazaaryar.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Event
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.bazaaryar.app.data.EVENTS
import ir.bazaaryar.app.ui.BazaaryarTheme
import ir.bazaaryar.app.ui.C
import ir.bazaaryar.app.ui.MarketScreen
import ir.bazaaryar.app.ui.NewsScreen
import ir.bazaaryar.app.ui.WatchScreen
import ir.bazaaryar.app.ui.faTime
import ir.bazaaryar.app.ui.rememberNow

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BazaaryarTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AskNotificationPermission()
                    AppRoot(vm)
                }
            }
        }
    }
}

@Composable
private fun AskNotificationPermission() {
    if (Build.VERSION.SDK_INT < 33) return
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
}

private data class Tab(val label: String, val icon: ImageVector)
private val TABS = listOf(
    Tab("اخبار", Icons.Filled.Event),
    Tab("بازار", Icons.Filled.BarChart),
    Tab("واچ‌لیست", Icons.Filled.Star),
)

@Composable
private fun AppRoot(vm: MainViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val market by vm.market.collectAsStateWithLifecycle()
    val watch by vm.watch.collectAsStateWithLifecycle()
    val alarms by vm.alarms.collectAsStateWithLifecycle()
    val now = rememberNow()

    Scaffold(
        containerColor = C.Paper,
        topBar = {
            Row(
                Modifier.fillMaxWidth().background(C.Deep).padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("بازاریار", color = C.Paper, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${faTime(now)} تهران", color = C.OnDeepMuted, fontSize = 14.sp)
            }
        },
        bottomBar = {
            NavigationBar(containerColor = C.Card) {
                TABS.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i, onClick = { tab = i },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = C.Lead, selectedTextColor = C.Lead),
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when (tab) {
                0 -> NewsScreen(EVENTS, now, alarms, vm::toggleAlarm)
                1 -> MarketScreen(market, watch, vm::toggleWatch, { vm.refresh() })
                else -> WatchScreen(market.coins, watch, EVENTS, alarms, now, vm::toggleWatch, vm::toggleAlarm) { tab = it }
            }
        }
    }
}
