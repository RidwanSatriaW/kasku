package id.kasku.app.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kasku.app.data.*
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

private val PAD = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp)
private val WARN: Color @Composable get() = LocalPal.current.warn
private val AMBER: Color @Composable get() = LocalPal.current.warn

@Composable
private fun Header(title: String, vm: KaskuVM, month: Boolean = true) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        if (month) MonthSwitcher(vm.month, { vm.shift(-1) }, { vm.shift(1) })
    }
}

@Composable
private fun Hero(total: Long, hide: Boolean, onToggle: (() -> Unit)?, st: MonthStat?, ym: YearMonth, wallets: List<Wallet>, txs: List<Tx>) {
    val pal = LocalPal.current
    val brush = Brush.linearGradient(listOf(pal.g1, pal.g2, pal.g3))
    val soft = Color.White.copy(alpha = 0.8f)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(brush).padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("TOTAL SALDO", color = soft, fontSize = 12.sp)
            if (onToggle != null) {
                Text(
                    if (hide) "Tampilkan" else "Sembunyikan", color = Color.White, fontSize = 12.sp,
                    modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.12f)).clickable { onToggle() }.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
        }
        Text(money(total, hide), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        if (st != null) {
            Spacer(Modifier.height(16.dp))
            val m = MON[ym.monthValue - 1]
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Pemasukan $m", color = soft, fontSize = 12.sp)
                    Text(rp(st.income), color = Color(0xFF7FE0AB), fontWeight = FontWeight.SemiBold)
                }
                Column(Modifier.weight(1f)) {
                    Text("Pengeluaran $m", color = soft, fontSize = 12.sp)
                    Text(rp(st.expense), color = Color(0xFFFFB3BD), fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                wallets.forEach { w ->
                    Column(Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.1f)).padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Text(w.name, color = soft, fontSize = 12.sp)
                        Text(money(balance(w, txs), hide), color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(st: MonthStat, ym: YearMonth, txs: List<Tx>, wallets: List<Wallet>, onTx: (Tx) -> Unit) {
    val cs = MaterialTheme.colorScheme
    var pt by rememberSaveable { mutableStateOf("out") }
    var sel by rememberSaveable { mutableStateOf<String?>(null) }
    var showList by remember { mutableStateOf(false) }
    val by = if (pt == "out") st.byCat else st.incByCat
    val tot = if (pt == "out") st.expense else st.income
    CardBox {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KChip(selected = pt == "out", onClick = { pt = "out"; sel = null }, label = { Text("Pengeluaran") })
            KChip(selected = pt == "in", onClick = { pt = "in"; sel = null }, label = { Text("Pemasukan") })
        }
        if (by.isEmpty() || tot <= 0) {
            Text(
                "Belum ada " + (if (pt == "out") "pengeluaran" else "pemasukan") + " di " + monthLabel(ym),
                color = cs.onSurfaceVariant, modifier = Modifier.padding(vertical = 24.dp).align(Alignment.CenterHorizontally)
            )
        } else {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Donut(by, tot, sel) { catColor(it, pt == "in") }
            }
            by.forEach { (c, v) ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(if (c == sel) cs.surfaceVariant else Color.Transparent)
                        .clickable { sel = if (sel == c) null else c }.padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(catColor(c, pt == "in")))
                    Spacer(Modifier.width(10.dp))
                    Text(c, Modifier.weight(1f))
                    Text((v * 100 / tot).toString() + "%", color = cs.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(rp(v), fontWeight = FontWeight.SemiBold)
                }
            }
            val s = sel
            if (s != null) {
                val n = txs.count { it.date.startsWith(ym.toString()) && it.type == pt && !it.ref && it.category == s }
                if (n > 0) TextButton(onClick = { showList = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Lihat $n transaksi $s") }
            }
        }
    }
    val s = sel
    if (showList && s != null) {
        val l = txs.filter { it.date.startsWith(ym.toString()) && it.type == pt && !it.ref && it.category == s }
        CatTxDialog(s + " · " + rp(l.sumOf { it.amount }), l, wallets, onTx = { showList = false; onTx(it) }, onClose = { showList = false })
    }
}

@Composable
fun HomeScreen(
    vm: KaskuVM, wallets: List<Wallet>, txs: List<Tx>, budgets: List<Budget>, recs: List<Recurring>, goals: List<Goal>,
    onTx: (Tx) -> Unit, onAll: () -> Unit, onAdd: () -> Unit, onPlan: (String) -> Unit
) {
    val ym = vm.month
    val key = ym.toString()
    val st = monthStat(txs, key)
    val pst = monthStat(txs, addM(key, -1))
    val total = wallets.sumOf { balance(it, txs) }
    val cs = MaterialTheme.colorScheme
    val isCur = key == curM()
    val dd = LocalDate.now().dayOfMonth
    val dim = YearMonth.now().lengthOfMonth()
    val fc = if (isCur) st.expense * dim / dd else st.expense
    val df = if (pst.expense > 0) Math.round((st.expense - pst.expense) * 100.0 / pst.expense).toInt() else null
    val bt = budgets.filter { it.limitAmount > 0 }
    val bl = bt.sumOf { it.limitAmount }
    val bs = bt.sumOf { st.out(it.category) }
    val warn = bt.filter { st.out(it.category) * 10 >= it.limitAmount * 8 }.map { it.category + " " + (st.out(it.category) * 100 / it.limitAmount) + "%" }
    var rem: Long? = null
    var remLabel = ""
    if (isCur) {
        if (bl > 0) { rem = bl - bs; remLabel = "sisa budget" }
        else if (st.income > 0) { rem = st.income - st.spending; remLabel = "sisa pemasukan" }
    }
    val up = recs.filter { !it.paused }.map { it to dayDiff(today(), nextDue(it)) }.filter { it.second <= 14 }.sortedBy { it.second }
    val recent = txs.filter { it.date.startsWith(key) }.take(5)
    val activeGoals = goals.filter { it.closedDate.isEmpty() }

    LazyColumn(contentPadding = PAD, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Kasku", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                MonthSwitcher(ym, { vm.shift(-1) }, { vm.shift(1) })
            }
        }
        item { Hero(total, vm.hide, { vm.toggleHide() }, st, ym, wallets, txs) }
        item { SectionTitle("Menu") }
        item {
            Row(Modifier.fillMaxWidth()) {
                MenuTile("Anggaran", Icons.Filled.PieChart, catColor("Belanja"), Modifier.weight(1f)) { onPlan("bud") }
                MenuTile("Berulang", Icons.Filled.Repeat, catColor("Transport"), Modifier.weight(1f)) { onPlan("rec") }
                MenuTile("Target", Icons.Filled.TrackChanges, catColor("Hiburan"), Modifier.weight(1f)) { onPlan("goal") }
                MenuTile("Belanja", Icons.Filled.ShoppingCart, catColor("Tabungan"), Modifier.weight(1f)) { onPlan("shop") }
            }
        }
        if (txs.isEmpty()) {
            item {
                CardBox {
                    Text("Mulai dari transaksi pertama", fontWeight = FontWeight.SemiBold)
                    Text("Catat pemasukan dan pengeluaran, atau atur dulu dompet dan budget-mu.", color = cs.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("Tambah transaksi") }
                }
            }
        }
        if (warn.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = lerp(cs.surface, WARN, 0.14f))
                ) { Text("Budget hampir habis: " + warn.joinToString(", "), color = WARN, fontWeight = FontWeight.Medium, modifier = Modifier.padding(14.dp)) }
            }
        }
        val r = rem
        if (isCur && r != null) {
            val left = dim - dd + 1
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = lerp(cs.surface, cs.primary, 0.12f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Aman dibelanjakan per hari", color = cs.onSurfaceVariant, fontSize = 13.sp)
                        if (r > 0) {
                            Text(rp(r / left), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = cs.primary)
                            Text("$remLabel ${rp(r)} untuk $left hari lagi", color = cs.onSurfaceVariant, fontSize = 13.sp)
                        } else {
                            Text("Rp0", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = cs.error)
                            Text(if (bl > 0) "Budget bulan ini sudah habis" else "Pemasukan bulan ini sudah terpakai", color = cs.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CardBox(Modifier.weight(1f)) {
                    Text("Vs bulan lalu", fontSize = 12.sp, color = cs.onSurfaceVariant)
                    Text(
                        if (df == null) "-" else (if (df > 0) "+" else "") + df + "%", fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                        color = if (df == null) cs.onSurface else if (df > 0) cs.error else if (df < 0) incomeColor() else cs.onSurface
                    )
                }
                CardBox(Modifier.weight(1f)) {
                    Text(if (isCur) "Proyeksi akhir bulan" else "Total pengeluaran", fontSize = 12.sp, color = cs.onSurfaceVariant)
                    Text(rp(fc), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        if (up.isNotEmpty()) {
            item { SectionTitle("Akan datang") }
            item {
                CardBox {
                    up.forEach { (r2, k) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(cs.surfaceVariant), contentAlignment = Alignment.Center) {
                                Text(if (r2.type == "tf") "⇄" else r2.day.toString(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(recName(r2, wallets), fontWeight = FontWeight.Medium, maxLines = 1)
                                Text(
                                    (if (k <= 0) "Hari ini" else if (k == 1L) "Besok" else "$k hari lagi") + " · " + shortDate(nextDue(r2)),
                                    fontSize = 12.sp, color = cs.onSurfaceVariant
                                )
                            }
                            Text(rp(r2.amount), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        if (activeGoals.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Target")
                    TextButton(onClick = { onPlan("goal") }) { Text("Kelola") }
                }
            }
            item {
                CardBox {
                    activeGoals.forEachIndexed { i, g ->
                        if (i > 0) Spacer(Modifier.height(14.dp))
                        val p = g.saved.toFloat() / g.target.toFloat()
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(g.name, fontWeight = FontWeight.Medium)
                            Text("${Math.round(p * 100)}% · ${cp(g.saved)} / ${cp(g.target)}", fontSize = 13.sp, color = cs.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(6.dp))
                        Bar(p, goalColor(g.colorIdx))
                    }
                }
            }
        }
        item { SectionTitle("Ringkasan kategori") }
        item { CategoryCard(st, ym, txs, wallets, onTx) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Terbaru")
                TextButton(onClick = onAll) { Text("Semua") }
            }
        }
        if (recent.isEmpty()) item { Text("Belum ada transaksi bulan ini", color = cs.onSurfaceVariant) }
        else items(recent, key = { it.id }) { t -> TxRow(t, wallets) { onTx(t) } }
    }
}

@Composable
fun TxScreen(vm: KaskuVM, wallets: List<Wallet>, txs: List<Tx>, onTx: (Tx) -> Unit) {
    val ym = vm.month
    val key = ym.toString()
    val st = monthStat(txs, key)
    val cs = MaterialTheme.colorScheme
    var q by rememberSaveable { mutableStateOf("") }
    var ft by rememberSaveable { mutableStateOf("all") }
    val list = txs.filter { t ->
        t.date.startsWith(key) && (ft == "all" || t.type == ft) &&
            (q.isBlank() || t.note.contains(q, true) || (t.category ?: "").contains(q, true) || wn(wallets, t.walletId).contains(q, true))
    }
    val groups = list.groupBy { it.date }
    LazyColumn(contentPadding = PAD, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Header("Transaksi", vm) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CardBox(Modifier.weight(1f)) {
                    Text("Pemasukan", fontSize = 12.sp, color = cs.onSurfaceVariant)
                    Text(rp(st.income), color = incomeColor(), fontWeight = FontWeight.SemiBold)
                }
                CardBox(Modifier.weight(1f)) {
                    Text("Pengeluaran", fontSize = 12.sp, color = cs.onSurfaceVariant)
                    Text(rp(st.expense), color = cs.error, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            OutlinedTextField(
                value = q, onValueChange = { q = it }, singleLine = true,
                placeholder = { Text("Cari catatan, kategori, dompet") }, modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("all" to "Semua", "out" to "Keluar", "in" to "Masuk", "tf" to "Transfer").forEach { (k, l) ->
                    KChip(selected = ft == k, onClick = { ft = k }, label = { Text(l) })
                }
            }
        }
        if (list.isEmpty()) item { Text("Tidak ada transaksi", color = cs.onSurfaceVariant, modifier = Modifier.padding(24.dp)) }
        groups.forEach { (d, l) ->
            val net = l.sumOf { if (it.type == "in") it.amount else if (it.type == "out") -it.amount else 0L }
            item(key = "h$d") {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(dayLabel(d), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurfaceVariant)
                    Text(if (net != 0L) (if (net > 0) "+" else "") + cp(net) else "", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurfaceVariant)
                }
            }
            items(l, key = { it.id }) { t -> TxRow(t, wallets) { onTx(t) } }
        }
    }
}

@Composable
fun AnalysisScreen(vm: KaskuVM, wallets: List<Wallet>, txs: List<Tx>, budgets: List<Budget>, recs: List<Recurring>) {
    val key = vm.month.toString()
    val st = monthStat(txs, key)
    val cs = MaterialTheme.colorScheme
    val h = health(txs, wallets, budgets, key)
    val needs = NEED.sumOf { st.out(it) }
    val wants = WANT.sumOf { st.out(it) }
    val base = if (st.income > 0) st.income else needs + wants
    val ins = insights(txs, wallets, budgets, recs, key)
    val ml = (5 downTo 0).map { k ->
        val m = addM(key, -k)
        val s = monthStat(txs, m)
        Triple(MON[m.substring(5).toInt() - 1], s.income, s.expense)
    }
    LazyColumn(contentPadding = PAD, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Header("Analisis", vm) }
        item {
            val score = h.score
            if (score == null) {
                CardBox {
                    Text("Skor kesehatan belum bisa dihitung", fontWeight = FontWeight.SemiBold)
                    Text("Tambahkan pemasukan, budget, atau saldo agar skor muncul.", color = cs.onSurfaceVariant)
                }
            } else {
                val c = if (score >= 75) incomeColor() else if (score >= 50) AMBER else cs.error
                val lab = if (score >= 75) "Sehat" else if (score >= 50) "Cukup baik" else "Perlu perhatian"
                CardBox {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ScoreRing(score, c)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Skor kesehatan keuangan", color = cs.onSurfaceVariant, fontSize = 13.sp)
                            Text(lab, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = c)
                            Text("Gabungan tabungan, budget, dan dana darurat", color = cs.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    HealthRow("Tingkat tabungan", (h.sr?.let { Math.round(it * 100).toString() + "%" } ?: "-") + " · target 20%", h.p1, "Butuh data pemasukan bulan ini")
                    HealthRow("Kepatuhan budget", if (h.nb > 0) "${h.ok} dari ${h.nb} kategori aman" else "Belum diatur", h.p2, "Atur budget di tab Rencana")
                    HealthRow("Dana darurat", (h.mo?.let { String.format(ID, "%.1f", it) + " bulan" } ?: "-") + " · target 6 bulan", h.p3, "Butuh riwayat pengeluaran")
                }
            }
        }
        item { SectionTitle("Aturan 50/30/20") }
        item {
            if (base <= 0) {
                CardBox { Text("Belum ada data untuk dibandingkan dengan aturan 50/30/20", color = cs.onSurfaceVariant) }
            } else {
                val pn = needs.toDouble() / base
                val pw = wants.toDouble() / base
                val ps = if (st.income > 0) max(0.0, 1 - pn - pw) else 0.0
                val tt = if (pn + pw + ps > 0) pn + pw + ps else 1.0
                val rows = listOf(Triple("Kebutuhan", pn, "50%"), Triple("Keinginan", pw, "30%"), Triple("Tabungan", ps, "20%"))
                val cols = listOf(catColor("Makanan"), catColor("Belanja"), catColor("Transport"))
                CardBox {
                    Row(Modifier.fillMaxWidth().height(14.dp).clip(CircleShape), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        rows.forEachIndexed { i, r ->
                            if (r.second > 0) Box(Modifier.weight((r.second / tt).toFloat()).fillMaxHeight().background(cols[i]))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    rows.forEachIndexed { i, r ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(cols[i]))
                            Spacer(Modifier.width(8.dp))
                            Text(r.first, Modifier.weight(1f))
                            Text("target " + r.third, color = cs.onSurfaceVariant, fontSize = 13.sp)
                            Spacer(Modifier.width(10.dp))
                            Text(Math.round(r.second * 100).toString() + "%", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Kebutuhan: makanan, transport, tagihan, kesehatan. Keinginan: belanja, hiburan, lainnya. " +
                            (if (st.income > 0) "Tabungan adalah sisa pemasukan setelah keduanya." else "Persentase dihitung dari total pengeluaran karena belum ada pemasukan."),
                        color = cs.onSurfaceVariant, fontSize = 13.sp
                    )
                }
            }
        }
        item { SectionTitle("Wawasan") }
        item {
            CardBox {
                ins.forEachIndexed { i, t ->
                    if (i > 0) HorizontalDivider(color = cs.outline, modifier = Modifier.padding(vertical = 10.dp))
                    Text(t)
                }
            }
        }
        item { SectionTitle("6 bulan terakhir") }
        item {
            CardBox {
                MonthBars(ml, incomeColor(), cs.error)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(9.dp).clip(RoundedCornerShape(3.dp)).background(incomeColor())); Spacer(Modifier.width(5.dp))
                        Text("Pemasukan", fontSize = 12.sp, color = cs.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(9.dp).clip(RoundedCornerShape(3.dp)).background(cs.error)); Spacer(Modifier.width(5.dp))
                        Text("Pengeluaran", fontSize = 12.sp, color = cs.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.HealthRow(title: String, info: String, p: Double?, empty: String) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.padding(top = 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(info, color = cs.onSurfaceVariant, fontSize = 13.sp)
        }
        Spacer(Modifier.height(6.dp))
        if (p == null) Text(empty, color = cs.onSurfaceVariant, fontSize = 13.sp) else Bar(p.toFloat(), cs.primary)
    }
}

// ============================ RENCANA ============================

@Composable
fun PlanScreen(
    vm: KaskuVM, sub: String, onSub: (String) -> Unit, wallets: List<Wallet>, txs: List<Tx>, budgets: List<Budget>,
    recs: List<Recurring>, goals: List<Goal>, logs: List<GoalLog>, shop: List<ShopItem>
) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Header("Rencana", vm, month = false)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("bud" to "Budget", "rec" to "Berulang", "goal" to "Target", "shop" to "Belanja").forEach { (k, l) ->
                    KChip(selected = sub == k, onClick = { onSub(k) }, label = { Text(l) })
                }
            }
        }
        Box(Modifier.weight(1f)) {
            when (sub) {
                "bud" -> BudgetPane(vm, txs, budgets)
                "rec" -> RecPane(vm, wallets, recs)
                "goal" -> GoalPane(vm, wallets, goals, logs)
                else -> ShopPane(vm, wallets, shop)
            }
        }
    }
}

@Composable
private fun BudgetPane(vm: KaskuVM, txs: List<Tx>, budgets: List<Budget>) {
    val key = vm.month.toString()
    val st = monthStat(txs, key)
    val cs = MaterialTheme.colorScheme
    var edit by remember { mutableStateOf(false) }
    val active = OUT_CATS.filter { c -> budgets.any { it.category == c && it.limitAmount > 0L } }
    val limit = active.sumOf { c -> budgets.first { it.category == c }.limitAmount }
    val used = active.sumOf { c -> st.out(c) }
    LazyColumn(contentPadding = PAD, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                MonthSwitcher(vm.month, { vm.shift(-1) }, { vm.shift(1) })
                Button(onClick = { edit = true }) { Text("Atur budget") }
            }
        }
        if (active.isEmpty()) {
            item {
                CardBox {
                    Text("Belum ada budget", fontWeight = FontWeight.SemiBold)
                    Text("Batas belanja per kategori membantu menahan pengeluaran sebelum terlambat.", color = cs.onSurfaceVariant)
                }
            }
        } else {
            item {
                CardBox {
                    Text("Terpakai bulan ini", color = cs.onSurfaceVariant, fontSize = 13.sp)
                    Text(rp(used) + " dari " + rp(limit), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Bar(used.toFloat() / limit.toFloat(), if (used > limit) cs.error else cs.primary)
                }
            }
            item {
                CardBox {
                    active.forEachIndexed { i, c ->
                        val v = st.out(c)
                        val l = budgets.first { it.category == c }.limitAmount
                        val p = v.toFloat() / l.toFloat()
                        if (i > 0) Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconTile(catIcon(c, "out"), catColor(c))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(c, fontWeight = FontWeight.Medium)
                                    Text(rp(v) + " / " + rp(l), color = if (p > 1f) cs.error else cs.onSurfaceVariant, fontSize = 13.sp)
                                }
                                Spacer(Modifier.height(6.dp))
                                Bar(p, if (p > 1f) cs.error else if (p > 0.8f) AMBER else cs.primary)
                            }
                        }
                    }
                }
            }
        }
    }
    if (edit) {
        val avg3 = OUT_CATS.associateWith { c ->
            val v = (1..3).map { monthStat(txs, addM(curM(), -it)).out(c) }.filter { it > 0 }
            if (v.isEmpty()) 0L else (ceil(v.sum().toDouble() / v.size / 50000) * 50000).toLong()
        }
        BudgetDialog(budgets, avg3, onSave = { vm.setBudgets(it); edit = false }, onClose = { edit = false })
    }
}

@Composable
private fun RecPane(vm: KaskuVM, wallets: List<Wallet>, recs: List<Recurring>) {
    val cs = MaterialTheme.colorScheme
    var add by remember { mutableStateOf<String?>(null) }
    var del by remember { mutableStateOf<Recurring?>(null) }
    LazyColumn(contentPadding = PAD, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { add = "tf" }, modifier = Modifier.weight(1f)) { Text("+ Transfer", maxLines = 1) }
                OutlinedButton(onClick = { add = "out" }, modifier = Modifier.weight(1f)) { Text("+ Masuk/keluar", maxLines = 1) }
            }
        }
        item {
            Text(
                "Transfer berulang memindahkan saldo antar dompet otomatis tiap bulan, misalnya Bank ke Tabungan setiap gajian. Transaksi dibuat saat aplikasi dibuka pada atau setelah tanggalnya.",
                color = cs.onSurfaceVariant, fontSize = 13.sp
            )
        }
        if (recs.isEmpty()) {
            item {
                CardBox {
                    Text("Belum ada transaksi berulang", fontWeight = FontWeight.SemiBold)
                    Text("Contoh: gaji tiap tgl 1, listrik tiap tgl 5, atau transfer Bank ke Tabungan tiap tgl 2.", color = cs.onSurfaceVariant)
                }
            }
        }
        items(recs, key = { it.id }) { r ->
            CardBox(Modifier.then(if (r.paused) Modifier else Modifier)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(cs.surfaceVariant), contentAlignment = Alignment.Center) {
                        Text(if (r.type == "tf") "⇄" else r.day.toString(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(recName(r, wallets), fontWeight = FontWeight.SemiBold)
                        Text(
                            "Tiap tgl ${r.day} · " + (if (r.type == "tf") wn(wallets, r.walletId) + " → " + wn(wallets, r.toWalletId ?: 0L) else wn(wallets, r.walletId)),
                            color = cs.onSurfaceVariant, fontSize = 13.sp
                        )
                        Text(if (r.paused) "Dijeda" else "Berikutnya " + shortDate(nextDue(r)), color = cs.onSurfaceVariant, fontSize = 13.sp)
                    }
                    Text(rp(r.amount), fontWeight = FontWeight.SemiBold, color = if (r.type == "in") incomeColor() else if (r.type == "out") cs.error else cs.onSurface)
                }
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.toggleRec(r) }, modifier = Modifier.weight(1f)) { Text(if (r.paused) "Aktifkan" else "Jeda") }
                    OutlinedButton(onClick = { del = r }, modifier = Modifier.weight(1f)) { Text("Hapus") }
                }
            }
        }
    }
    add?.let { t -> RecDialog(t, wallets, onSave = { vm.addRec(it); add = null }, onClose = { add = null }) }
    del?.let { r ->
        ConfirmDialog("Hapus transaksi berulang ini? Transaksi yang sudah tercatat tidak ikut terhapus.", "Hapus", onOk = { vm.deleteRec(r); del = null }, onClose = { del = null })
    }
}

@Composable
private fun GoalPane(vm: KaskuVM, wallets: List<Wallet>, goals: List<Goal>, logs: List<GoalLog>) {
    val cs = MaterialTheme.colorScheme
    var form by remember { mutableStateOf<Goal?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var dep by remember { mutableStateOf<Goal?>(null) }
    var wd by remember { mutableStateOf<Goal?>(null) }
    var hist by remember { mutableStateOf<Goal?>(null) }
    var close by remember { mutableStateOf<Goal?>(null) }
    val act = goals.filter { it.closedDate.isEmpty() }
    val arc = goals.filter { it.closedDate.isNotEmpty() }.sortedByDescending { it.closedDate }
    val sv = act.sumOf { it.saved }
    val tg = act.sumOf { it.target }
    LazyColumn(contentPadding = PAD, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Button(onClick = { form = null; showForm = true }, modifier = Modifier.fillMaxWidth()) { Text("+ Target baru") } }
        if (act.isNotEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = cs.primaryContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Total terkumpul untuk ${act.size} target", color = cs.onSurfaceVariant, fontSize = 13.sp)
                        Text(rp(sv), fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text("dari ${rp(tg)} · ${Math.round(sv * 100.0 / tg)}%", color = cs.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
            }
        } else {
            item {
                CardBox {
                    Text("Belum ada target aktif", fontWeight = FontWeight.SemiBold)
                    Text("Umroh, mobil, rumah, atau apa pun impianmu. Beri tenggat agar tahu berapa yang perlu disisihkan tiap bulan.", color = cs.onSurfaceVariant)
                }
            }
        }
        items(act, key = { it.id }) { g ->
            val p = g.saved.toFloat() / g.target.toFloat()
            val done = g.saved >= g.target
            val gc = goalColor(g.colorIdx)
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = lerp(cs.surface, gc, 0.2f))) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Ring(p, gc, 58.dp, 7.dp, (g.name.take(1)).uppercase(), 20)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(g.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(rp(g.saved) + " dari " + rp(g.target), color = cs.onSurfaceVariant, fontSize = 13.sp)
                            Text(if (done) "Tercapai" else "Kurang " + rp(g.target - g.saved), color = cs.onSurfaceVariant, fontSize = 13.sp)
                        }
                        Text(Math.round(p * 100).toString() + "%", fontWeight = FontWeight.Bold)
                    }
                    if (g.date.isNotEmpty() && !done) {
                        val dl = dayDiff(today(), g.date)
                        val mo = max(1L, ceil(dl / 30.4).toLong())
                        Spacer(Modifier.height(8.dp))
                        if (dl < 0) Text("Lewat tenggat " + shortDate(g.date), color = WARN, fontSize = 13.sp)
                        else Text("Sisihkan " + rp((g.target - g.saved) / mo) + " per bulan sampai " + shortDate(g.date) + " ($mo bulan)", color = cs.onSurfaceVariant, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { dep = g }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) { Text("Setor", maxLines = 1) }
                        OutlinedButton(onClick = { wd = g }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) { Text("Tarik", maxLines = 1) }
                        OutlinedButton(onClick = { hist = g }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) { Text("Riwayat", maxLines = 1) }
                    }
                    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { form = g; showForm = true }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) { Text("Ubah", maxLines = 1) }
                        OutlinedButton(onClick = { close = g }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) { Text("Tutup", maxLines = 1) }
                    }
                }
            }
        }
        if (arc.isNotEmpty()) {
            item { SectionTitle("Arsip target") }
            items(arc, key = { "a" + it.id }) { g ->
                CardBox {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(g.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Ditutup " + shortDate(g.closedDate) + " · " + (
                                    if (g.closedWallet != 0L) "dana " + rp(g.closedAmount) + " dikembalikan ke " + wn(wallets, g.closedWallet)
                                    else if (g.closedKept > 0) "tanpa pengembalian (" + rp(g.closedKept) + ")" else "tanpa dana tersisa"
                                    ),
                                color = cs.onSurfaceVariant, fontSize = 13.sp
                            )
                        }
                        TextButton(onClick = { hist = g }) { Text("Riwayat") }
                    }
                }
            }
        }
    }
    if (showForm) GoalDialog(form, onSave = { g, s -> vm.saveGoal(g, s); showForm = false }, onClose = { showForm = false })
    dep?.let { g ->
        AmountDialog("Setor ke " + g.name, null, "Diambil dari dompet", wallets, null, "Setor", onOk = { a, w -> vm.deposit(g.id, a, w); dep = null }, onClose = { dep = null })
    }
    wd?.let { g ->
        AmountDialog("Tarik dari " + g.name, "Dana tersedia " + rp(g.saved), "Masuk ke dompet", wallets, g.saved, "Tarik dana", onOk = { a, w -> vm.withdraw(g.id, a, w); wd = null }, onClose = { wd = null })
    }
    hist?.let { g -> GoalHistoryDialog(g, logs, wallets, onClose = { hist = null }) }
    close?.let { g ->
        WalletPickDialog(
            "Tutup target",
            g.name + " punya dana terkumpul " + rp(g.saved) + ". Target dipindah ke arsip dan riwayatnya tetap tersimpan.",
            "Kembalikan dana ke", wallets, if (g.saved > 0) "Tidak dikembalikan (sudah terpakai)" else null, "Tutup target",
            onOk = { w -> vm.closeGoal(g.id, if (g.saved > 0) w else 0L); close = null }, onClose = { close = null }
        )
    }
}

@Composable
private fun ShopPane(vm: KaskuVM, wallets: List<Wallet>, shop: List<ShopItem>) {
    val cs = MaterialTheme.colorScheme
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var buy by remember { mutableStateOf(false) }
    val total = shop.filter { !it.done }.sumOf { it.price }
    LazyColumn(contentPadding = PAD, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            CardBox {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Barang") }, placeholder = { Text("Mis. Beras 5 kg") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = price, onValueChange = { price = it.filter(Char::isDigit).take(12) }, label = { Text("Perkiraan harga") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { if (name.isNotBlank()) { vm.addShop(name.trim(), price.toLongOrNull() ?: 0L); name = ""; price = "" } },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Tambah") }
            }
        }
        item {
            CardBox {
                if (shop.isEmpty()) Text("Daftar kosong", color = cs.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
                shop.forEach { x ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = x.done, onCheckedChange = { vm.toggleShop(x) })
                        Text(
                            x.name, Modifier.weight(1f),
                            color = if (x.done) cs.onSurfaceVariant else cs.onSurface,
                            textDecoration = if (x.done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                        )
                        if (x.price > 0) Text(rp(x.price), color = cs.onSurfaceVariant, fontSize = 13.sp)
                        TextButton(onClick = { vm.deleteShop(x) }) { Text("×", fontSize = 18.sp) }
                    }
                }
                if (shop.isNotEmpty()) {
                    HorizontalDivider(color = cs.outline, modifier = Modifier.padding(vertical = 8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Sisa estimasi", fontWeight = FontWeight.SemiBold)
                        Text(rp(total), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        if (shop.any { it.done }) {
            item { OutlinedButton(onClick = { buy = true }, modifier = Modifier.fillMaxWidth()) { Text("Catat yang dicentang sebagai pengeluaran") } }
        }
    }
    if (buy) {
        val d = shop.filter { it.done }
        val t = d.sumOf { it.price }
        WalletPickDialog(
            "Catat belanja", "${d.size} barang, total ${rp(t)}" + (if (t == 0L) " (tanpa harga, hanya dibersihkan dari daftar)" else ""),
            "Dibayar dari dompet", wallets, null, "Catat", onOk = { w -> vm.buyShop(w); buy = false }, onClose = { buy = false }
        )
    }
}

// ============================ DOMPET ============================

private fun writeText(ctx: Context, uri: Uri, text: String) {
    try {
        ctx.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
        Toast.makeText(ctx, "File tersimpan", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(ctx, "Gagal menyimpan file", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun WalletScreen(vm: KaskuVM, wallets: List<Wallet>, txs: List<Tx>, onTransfer: () -> Unit) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    var editing by remember { mutableStateOf<Wallet?>(null) }
    var adding by remember { mutableStateOf(false) }
    var keyDlg by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<String?>(null) }
    val total = wallets.sumOf { balance(it, txs) }

    val csvL = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { u -> if (u != null) writeText(ctx, u, vm.exportCsv()) }
    val jsonL = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { u -> if (u != null) writeText(ctx, u, vm.exportJson()) }
    val impL = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) {
            try {
                val t = ctx.contentResolver.openInputStream(u)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
                parseSnapshot(t)
                pending = t
            } catch (e: Exception) {
                Toast.makeText(ctx, "File backup tidak valid", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(contentPadding = PAD, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Dompet", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Button(onClick = { adding = true }) { Text("+ Dompet") }
            }
        }
        item { Hero(total, vm.hide, null, null, vm.month, wallets, txs) }
        item {
            CardBox {
                wallets.forEach { w ->
                    Row(Modifier.fillMaxWidth().clickable { editing = w }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconTile(walletIcon(w.name), walletColor(w.name))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(w.name, fontWeight = FontWeight.Medium)
                            Text("Saldo awal " + rp(w.initialBalance), fontSize = 12.sp, color = cs.onSurfaceVariant)
                        }
                        Text(money(balance(w, txs), vm.hide), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        item { OutlinedButton(onClick = onTransfer, modifier = Modifier.fillMaxWidth()) { Text("Transfer antar dompet") } }
        item { SectionTitle("Tema warna") }
        item { PaletteChooser(vm.palette) { vm.choosePalette(it) } }
        item { SectionTitle("Mode") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("auto" to "Otomatis", "light" to "Terang", "dark" to "Gelap").forEach { (k, l) ->
                    KChip(selected = vm.theme == k, onClick = { vm.changeTheme(k) }, label = { Text(l) })
                }
            }
        }
        item { SectionTitle("Scan struk") }
        item {
            OutlinedButton(onClick = { keyDlg = true }, modifier = Modifier.fillMaxWidth()) {
                Text(if (vm.apiKey.isBlank()) "API key Claude (opsional, hasil lebih akurat)" else "API key Claude tersimpan · ubah")
            }
        }
        item { SectionTitle("Data") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { csvL.launch("kasku-transaksi.csv") }, modifier = Modifier.weight(1f)) { Text("Ekspor CSV", maxLines = 1) }
                OutlinedButton(onClick = { jsonL.launch("kasku-backup.json") }, modifier = Modifier.weight(1f)) { Text("Backup JSON", maxLines = 1) }
            }
        }
        item { OutlinedButton(onClick = { impL.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("Pulihkan dari backup") } }
        item {
            Text(
                "Data tersimpan di database SQLite lokal di HP ini (kasku.db). Backup JSON memakai format yang sama dengan versi web, jadi bisa dipulihkan di keduanya. Buat backup berkala.",
                color = cs.onSurfaceVariant, fontSize = 13.sp
            )
        }
    }
    if (adding) WalletDialog(null, onSave = { n, i -> vm.saveWallet(Wallet(name = n, initialBalance = i)); adding = false }, onDelete = null, onClose = { adding = false })
    editing?.let { w ->
        WalletDialog(
            w, onSave = { n, i -> vm.saveWallet(w.copy(name = n, initialBalance = i)); editing = null },
            onDelete = {
                vm.deleteWallet(w) { err ->
                    if (err != null) Toast.makeText(ctx, err, Toast.LENGTH_SHORT).show()
                    editing = null
                }
            },
            onClose = { editing = null }
        )
    }
    if (keyDlg) ApiKeyDialog(vm.apiKey, onSave = { vm.saveApiKey(it); keyDlg = false }, onClose = { keyDlg = false })
    pending?.let { t ->
        val s = parseSnapshot(t)
        ConfirmDialog(
            "Backup berisi ${s.txs.size} transaksi dan ${s.wallets.size} dompet. Data saat ini akan diganti.", "Ganti data",
            onOk = {
                pending = null
                vm.importJson(t) { ok -> Toast.makeText(ctx, if (ok) "Data dipulihkan" else "Gagal memulihkan data", Toast.LENGTH_SHORT).show() }
            },
            onClose = { pending = null }
        )
    }
}
