package id.kasku.app.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import id.kasku.app.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

data class FormState(val tx: Tx?, val type: String)

private val NUM = KeyboardOptions(keyboardType = KeyboardType.Number)

private fun validDate(s: String): Boolean = try { LocalDate.parse(s); true } catch (e: Exception) { false }

@Composable
fun ConfirmDialog(msg: String, label: String, onOk: () -> Unit, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Konfirmasi") },
        text = { Text(msg) },
        confirmButton = { TextButton(onClick = onOk) { Text(label) } },
        dismissButton = { TextButton(onClick = onClose) { Text("Batal") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickField(label: String, date: String, onPick: (String) -> Unit, onClear: (() -> Unit)? = null) {
    var open by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { open = true }, modifier = Modifier.weight(1f)) {
                Text(if (date.isNotEmpty() && validDate(date)) shortDate(date) + " " + date.take(4) else "Belum diatur")
            }
            if (onClear != null && date.isNotEmpty()) TextButton(onClick = onClear) { Text("Hapus") }
        }
    }
    if (open) {
        val base = if (date.isNotEmpty() && validDate(date)) LocalDate.parse(date) else LocalDate.now()
        val ds = rememberDatePickerState(initialSelectedDateMillis = base.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    ds.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()) }
                    open = false
                }) { Text("Pilih") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Batal") } }
        ) { DatePicker(state = ds) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TxSheet(initial: FormState, wallets: List<Wallet>, apiKey: String, onSave: (Tx) -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val e = initial.tx
    var type by remember { mutableStateOf(initial.type) }
    var amount by remember { mutableStateOf(e?.amount?.toString() ?: "") }
    var from by remember { mutableStateOf(e?.walletId ?: wallets.first().id) }
    var to by remember { mutableStateOf(e?.toWalletId ?: (wallets.firstOrNull { it.id != from } ?: wallets.first()).id) }
    var cat by remember { mutableStateOf(e?.category ?: "") }
    var note by remember { mutableStateOf(e?.note ?: "") }
    var date by remember { mutableStateOf(e?.date ?: LocalDate.now().toString()) }
    var scanning by remember { mutableStateOf(false) }
    var chooser by remember { mutableStateOf(false) }
    val camUri = remember { mutableStateOf<Uri?>(null) }

    val cats = if (type == "in") IN_CATS else OUT_CATS
    val catSel = if (cat in cats) cat else cats.first()

    fun runScan(uri: Uri) {
        scope.launch {
            scanning = true
            try {
                val r = if (apiKey.isNotBlank()) {
                    val b64 = withContext(Dispatchers.IO) { loadJpegB64(ctx, uri) }
                    scanReceipt(b64, apiKey)
                } else scanOffline(ctx, uri)
                type = "out"
                if (r.total > 0) amount = r.total.toString()
                cat = if (r.category in OUT_CATS) r.category else "Belanja"
                note = (listOf(r.merchant) + r.items.take(3)).filter { it.isNotBlank() }.joinToString(" · ").take(80)
                val d = r.date
                if (d != null && validDate(d)) date = d
                if (r.total <= 0) Toast.makeText(ctx, "Total tidak terbaca, isi manual", Toast.LENGTH_LONG).show()
            } catch (ex: Exception) {
                Toast.makeText(ctx, "Struk belum terbaca: " + (ex.message ?: "error"), Toast.LENGTH_LONG).show()
            } finally {
                scanning = false
            }
        }
    }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) runScan(uri) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val u = camUri.value
        if (ok && u != null) runScan(u)
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(if (e == null) "Transaksi baru" else "Ubah transaksi", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            if (e == null) {
                OutlinedButton(
                    enabled = !scanning,
                    onClick = { chooser = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (scanning) "Membaca struk..." else "Scan struk") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("out" to "Keluar", "in" to "Masuk", "tf" to "Transfer").forEach { (k, l) ->
                    KChip(selected = type == k, onClick = { type = k }, label = { Text(l) })
                }
            }
            OutlinedTextField(
                value = amount, onValueChange = { amount = it.filter(Char::isDigit).take(12) },
                label = { Text("Jumlah") },
                supportingText = { if (amount.isNotEmpty()) Text(rp(amount.toLongOrNull() ?: 0L)) },
                keyboardOptions = NUM, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Picker(if (type == "tf") "Dari dompet" else "Dompet", wn(wallets, from), wallets.map { it.id to it.name }) { from = it }
            if (type == "tf") Picker("Ke dompet", wn(wallets, to), wallets.map { it.id to it.name }) { to = it }
            else Picker("Kategori", catSel, cats.map { it to it }) { cat = it }
            OutlinedTextField(
                value = note, onValueChange = { note = it }, label = { Text("Catatan (opsional)") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            DatePickField("Tanggal", date, onPick = { date = it })
            Button(
                onClick = {
                    val a = amount.toLongOrNull() ?: 0L
                    if (a <= 0L) Toast.makeText(ctx, "Isi jumlah dulu", Toast.LENGTH_SHORT).show()
                    else if (type == "tf" && from == to) Toast.makeText(ctx, "Pilih dompet tujuan yang berbeda", Toast.LENGTH_SHORT).show()
                    else onSave(
                        Tx(
                            id = e?.id ?: 0L, type = type, amount = a, walletId = from,
                            toWalletId = if (type == "tf") to else null,
                            category = if (type == "tf") null else catSel,
                            note = note.trim(), date = date,
                            auto = e?.auto ?: false, goalId = e?.goalId, ref = e?.ref ?: false
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Simpan") }
        }
    }

    if (chooser) {
        AlertDialog(
            onDismissRequest = { chooser = false },
            title = { Text("Scan struk") },
            text = { Text("Pilih sumber foto struk. Pastikan seluruh struk terlihat dan terang.") },
            confirmButton = {
                TextButton(onClick = {
                    chooser = false
                    val f = File(ctx.cacheDir, "scan_" + System.currentTimeMillis() + ".jpg")
                    val u = FileProvider.getUriForFile(ctx, "id.kasku.app.files", f)
                    camUri.value = u
                    camera.launch(u)
                }) { Text("Kamera") }
            },
            dismissButton = {
                TextButton(onClick = {
                    chooser = false
                    gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text("Galeri") }
            }
        )
    }
}

@Composable
fun TxDetail(t: Tx, wallets: List<Wallet>, onEdit: () -> Unit, onDelete: () -> Unit, onClose: () -> Unit) {
    var ask by remember { mutableStateOf(false) }
    if (ask) {
        ConfirmDialog("Hapus transaksi ini?", "Hapus", onDelete) { ask = false }
        return
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(rp(t.amount)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(t.note.ifBlank { t.category ?: "Transfer" }, fontWeight = FontWeight.Medium)
                Text("Tanggal: " + shortDate(t.date) + " " + t.date.take(4))
                if (t.category != null) Text("Kategori: " + t.category)
                Text(
                    if (t.type == "tf") "Dompet: " + wn(wallets, t.walletId) + " → " + wn(wallets, t.toWalletId ?: 0L)
                    else "Dompet: " + wn(wallets, t.walletId)
                )
                if (t.auto) Text("Sumber: transaksi berulang")
                if (t.goalId != null) Text("Terkait target tabungan. Menghapus transaksi ini ikut mengoreksi saldo target.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            if (t.goalId == null) TextButton(onClick = onEdit) { Text("Ubah") }
            else TextButton(onClick = onClose) { Text("Tutup") }
        },
        dismissButton = { TextButton(onClick = { ask = true }) { Text("Hapus") } }
    )
}

@Composable
fun CatTxDialog(title: String, list: List<Tx>, wallets: List<Wallet>, onTx: (Tx) -> Unit, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                list.forEach { t -> TxRow(t, wallets) { onTx(t) } }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Tutup") } }
    )
}

@Composable
fun WalletDialog(w: Wallet?, onSave: (String, Long) -> Unit, onDelete: (() -> Unit)?, onClose: () -> Unit) {
    var name by remember { mutableStateOf(w?.name ?: "") }
    var initTxt by remember { mutableStateOf(w?.initialBalance?.takeIf { it != 0L }?.toString() ?: "") }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (w == null) "Dompet baru" else "Ubah dompet") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = initTxt, onValueChange = { initTxt = it.filter(Char::isDigit).take(12) },
                    label = { Text("Saldo awal") }, keyboardOptions = NUM, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Hapus dompet", color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onSave(name.trim(), initTxt.toLongOrNull() ?: 0L) }) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onClose) { Text("Batal") } }
    )
}

@Composable
fun BudgetDialog(budgets: List<Budget>, avg3: Map<String, Long>, onSave: (Map<String, Long>) -> Unit, onClose: () -> Unit) {
    val cats = OUT_CATS.filter { it != "Tabungan" }
    val vals = remember {
        mutableStateMapOf<String, String>().apply {
            for (c in cats) {
                val cur = budgets.firstOrNull { it.category == c }?.limitAmount ?: 0L
                put(c, if (cur > 0L) cur.toString() else "")
            }
        }
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Budget bulanan") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedButton(
                    onClick = { avg3.forEach { (c, v) -> if (v > 0) vals[c] = v.toString() } },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) { Text("Isi dari rata-rata 3 bulan terakhir") }
                cats.forEach { c ->
                    OutlinedTextField(
                        value = vals[c] ?: "", onValueChange = { vals[c] = it.filter(Char::isDigit).take(12) },
                        label = { Text(c) }, keyboardOptions = NUM, singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(cats.associateWith { (vals[it] ?: "").toLongOrNull() ?: 0L }) }) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onClose) { Text("Batal") } }
    )
}

@Composable
fun RecDialog(initType: String, wallets: List<Wallet>, onSave: (Recurring) -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var type by remember { mutableStateOf(initType) }
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var from by remember { mutableStateOf(wallets.first().id) }
    var to by remember { mutableStateOf((wallets.firstOrNull { it.id != from } ?: wallets.first()).id) }
    var cat by remember { mutableStateOf("") }
    var day by remember { mutableStateOf("") }
    val cats = if (type == "in") IN_CATS else OUT_CATS
    val catSel = if (cat in cats) cat else cats.first()
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (type == "tf") "Transfer berulang" else "Transaksi berulang") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("out" to "Keluar", "in" to "Masuk", "tf" to "Transfer").forEach { (k, l) ->
                        KChip(selected = type == k, onClick = { type = k }, label = { Text(l) })
                    }
                }
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nama") }, placeholder = { Text(if (type == "tf") "Mis. Tabung bulanan" else "Mis. Listrik, Gaji") }
                )
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it.filter(Char::isDigit).take(12) },
                    label = { Text("Jumlah") }, keyboardOptions = NUM, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Picker(if (type == "tf") "Dari dompet" else "Dompet", wn(wallets, from), wallets.map { it.id to it.name }) { from = it }
                if (type == "tf") Picker("Ke dompet", wn(wallets, to), wallets.map { it.id to it.name }) { to = it }
                else Picker("Kategori", catSel, cats.map { it to it }) { cat = it }
                OutlinedTextField(
                    value = day, onValueChange = { day = it.filter(Char::isDigit).take(2) },
                    label = { Text("Tanggal tiap bulan (1-28)") }, keyboardOptions = NUM, singleLine = true,
                    supportingText = { Text("Jika tanggal bulan ini sudah lewat, mulai berjalan bulan depan.") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = amount.toLongOrNull() ?: 0L
                val d = day.toIntOrNull() ?: 0
                if (a <= 0L || d < 1 || d > 28) Toast.makeText(ctx, "Isi jumlah dan tanggal 1 sampai 28", Toast.LENGTH_SHORT).show()
                else if (type == "tf" && from == to) Toast.makeText(ctx, "Pilih dompet tujuan yang berbeda", Toast.LENGTH_SHORT).show()
                else onSave(
                    Recurring(
                        type = type, amount = a, walletId = from, toWalletId = if (type == "tf") to else null,
                        category = if (type == "tf") null else catSel, note = name.trim(), day = d, last = ""
                    )
                )
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Batal") } }
    )
}

private val GOAL_QUICK = listOf("Umroh", "Haji", "Mobil", "Rumah", "Liburan", "Pendidikan", "Nikah", "Dana darurat", "Gadget")

@Composable
fun GoalDialog(g: Goal?, onSave: (Goal, Long) -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf(g?.name ?: "") }
    var target by remember { mutableStateOf(g?.target?.toString() ?: "") }
    var saved0 by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(g?.date ?: "") }
    var color by remember { mutableStateOf(g?.colorIdx ?: 0) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (g == null) "Target baru" else "Ubah target") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pilih cepat atau ketik nama sendiri", style = MaterialTheme.typography.labelMedium)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GOAL_QUICK.forEach { k -> KChip(selected = name == k, onClick = { name = k }, label = { Text(k) }) }
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama target") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = target, onValueChange = { target = it.filter(Char::isDigit).take(13) }, label = { Text("Nominal target") },
                    supportingText = { if (target.isNotEmpty()) Text(rp(target.toLongOrNull() ?: 0L)) },
                    keyboardOptions = NUM, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                if (g == null) {
                    OutlinedTextField(
                        value = saved0, onValueChange = { saved0 = it.filter(Char::isDigit).take(13) },
                        label = { Text("Sudah terkumpul (opsional)") }, keyboardOptions = NUM, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
                DatePickField("Tenggat (opsional)", date, onPick = { date = it }, onClear = { date = "" })
                Text("Warna", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    (0..7).forEach { i ->
                        Box(
                            Modifier.size(30.dp).clip(CircleShape).background(goalColor(i))
                                .border(3.dp, if (i == color) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                                .clickable { color = i }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val t = target.toLongOrNull() ?: 0L
                if (name.isBlank() || t <= 0L) Toast.makeText(ctx, "Isi nama dan nominal target", Toast.LENGTH_SHORT).show()
                else onSave((g ?: Goal(name = "", target = 0)).copy(name = name.trim(), target = t, date = date, colorIdx = color), saved0.toLongOrNull() ?: 0L)
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Batal") } }
    )
}

/** Dialog jumlah + pilih dompet, dipakai untuk setor dan tarik dana target. */
@Composable
fun AmountDialog(title: String, info: String?, walletLabel: String, wallets: List<Wallet>, max: Long?, confirm: String, onOk: (Long, Long) -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var amount by remember { mutableStateOf("") }
    var w by remember { mutableStateOf(wallets.first().id) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (info != null) Text(info, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it.filter(Char::isDigit).take(12) }, label = { Text("Jumlah") },
                    supportingText = { if (amount.isNotEmpty()) Text(rp(amount.toLongOrNull() ?: 0L)) },
                    keyboardOptions = NUM, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Picker(walletLabel, wn(wallets, w), wallets.map { it.id to it.name }) { w = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = amount.toLongOrNull() ?: 0L
                if (a <= 0L) Toast.makeText(ctx, "Isi jumlah", Toast.LENGTH_SHORT).show()
                else if (max != null && a > max) Toast.makeText(ctx, "Melebihi dana terkumpul", Toast.LENGTH_SHORT).show()
                else onOk(a, w)
            }) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Batal") } }
    )
}

/** Dialog pilih dompet. noneLabel != null menambah opsi dengan id 0. */
@Composable
fun WalletPickDialog(title: String, text: String, label: String, wallets: List<Wallet>, noneLabel: String?, confirm: String, onOk: (Long) -> Unit, onClose: () -> Unit) {
    var w by remember { mutableStateOf(wallets.first().id) }
    val opts = wallets.map { it.id to it.name } + (if (noneLabel != null) listOf(0L to noneLabel) else emptyList())
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text)
                Picker(label, opts.firstOrNull { it.first == w }?.second ?: "?", opts) { w = it }
            }
        },
        confirmButton = { TextButton(onClick = { onOk(w) }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onClose) { Text("Batal") } }
    )
}

@Composable
fun GoalHistoryDialog(g: Goal, logs: List<GoalLog>, wallets: List<Wallet>, onClose: () -> Unit) {
    val l = logs.filter { it.goalId == g.id }.sortedWith(compareByDescending<GoalLog> { it.date }.thenByDescending { it.id })
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Riwayat · " + g.name) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Terkumpul " + rp(g.saved) + " dari " + rp(g.target) + (if (g.closedDate.isNotEmpty()) " · ditutup " + shortDate(g.closedDate) else ""),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp
                )
                if (l.isEmpty()) Text("Belum ada riwayat", modifier = Modifier.padding(top = 12.dp))
                l.forEach { e ->
                    val inn = e.type == "in"
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(e.note, fontWeight = FontWeight.Medium)
                            Text(
                                shortDate(e.date) + (if (e.walletId != 0L) " · " + wn(wallets, e.walletId) else ""),
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text((if (inn) "+" else "−") + rp(e.amount), fontWeight = FontWeight.SemiBold, color = if (inn) incomeColor() else MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Tutup") } }
    )
}

@Composable
fun ApiKeyDialog(current: String, onSave: (String) -> Unit, onClose: () -> Unit) {
    var k by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("API key Claude (opsional)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Tanpa key, scan struk memakai pengenal teks di HP: gratis, offline, tapi kurang akurat. Dengan API key Claude (console.anthropic.com) hasilnya lebih akurat, dan foto struk dikirim ke Anthropic hanya saat kamu menekan Scan struk. Key hanya disimpan di HP ini.",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(value = k, onValueChange = { k = it }, label = { Text("API key") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { onSave(k) }) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onClose) { Text("Batal") } }
    )
}
