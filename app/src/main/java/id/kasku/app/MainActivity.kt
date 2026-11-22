package id.kasku.app

import android.graphics.Color as AColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.kasku.app.data.Tx
import id.kasku.app.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: KaskuVM = viewModel()
            val dark = when (vm.theme) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            LaunchedEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(AColor.TRANSPARENT) else SystemBarStyle.light(AColor.TRANSPARENT, AColor.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            KaskuTheme(vm.palette, dark) { KaskuApp(vm) }
        }
    }
}

@Composable
fun KaskuApp(vm: KaskuVM) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var planSub by rememberSaveable { mutableStateOf("bud") }
    var form by remember { mutableStateOf<FormState?>(null) }
    var detail by remember { mutableStateOf<Tx?>(null) }
    val wallets by vm.wallets.collectAsStateWithLifecycle()
    val txs by vm.txs.collectAsStateWithLifecycle()
    val budgets by vm.budgets.collectAsStateWithLifecycle()
    val recs by vm.recs.collectAsStateWithLifecycle()
    val goals by vm.goals.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val shop by vm.shop.collectAsStateWithLifecycle()
    val pal = LocalPal.current

    val items: List<Pair<String, ImageVector>> = listOf(
        "Beranda" to Icons.Filled.Home,
        "Transaksi" to Icons.AutoMirrored.Filled.List,
        "Analisis" to Icons.Filled.PieChart,
        "Rencana" to Icons.Filled.Flag,
        "Dompet" to Icons.Filled.AccountBalanceWallet
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = pal.nav) {
                items.forEachIndexed { i, (label, icon) ->
                    NavigationBarItem(
                        selected = tab == i, onClick = { tab = i },
                        icon = { Icon(icon, null) }, label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = pal.accent, selectedTextColor = pal.accent,
                            indicatorColor = pal.accent.copy(alpha = 0.14f),
                            unselectedIconColor = pal.muted, unselectedTextColor = pal.muted
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if ((tab == 0 || tab == 1) && wallets.isNotEmpty()) GradientFab { form = FormState(null, "out") }
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when (tab) {
                0 -> HomeScreen(
                    vm, wallets, txs, budgets, recs, goals,
                    onTx = { detail = it }, onAll = { tab = 1 },
                    onAdd = { form = FormState(null, "out") },
                    onPlan = { planSub = it; tab = 3 }
                )
                1 -> TxScreen(vm, wallets, txs, onTx = { detail = it })
                2 -> AnalysisScreen(vm, wallets, txs, budgets, recs)
                3 -> PlanScreen(vm, planSub, { planSub = it }, wallets, txs, budgets, recs, goals, logs, shop)
                else -> WalletScreen(vm, wallets, txs, onTransfer = { form = FormState(null, "tf") })
            }
        }
    }

    form?.let { f ->
        if (wallets.isNotEmpty()) TxSheet(f, wallets, vm.apiKey, onSave = { vm.save(it); form = null }, onClose = { form = null })
    }
    detail?.let { t ->
        TxDetail(
            t, wallets,
            onEdit = { form = FormState(t, t.type); detail = null },
            onDelete = { vm.delete(t); detail = null },
            onClose = { detail = null }
        )
    }
}
