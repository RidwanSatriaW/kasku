package id.kasku.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kasku.app.data.Tx
import id.kasku.app.data.Wallet
import java.time.YearMonth

fun catIcon(cat: String?, type: String): ImageVector = when (type) {
    "tf" -> Icons.Filled.SwapHoriz
    "in" -> when (cat) {
        "Gaji" -> Icons.Filled.Payments
        "Bonus" -> Icons.Filled.CardGiftcard
        "Usaha" -> Icons.Filled.Storefront
        else -> Icons.Filled.AttachMoney
    }
    else -> when (cat) {
        "Makanan" -> Icons.Filled.Restaurant
        "Transport" -> Icons.Filled.DirectionsCar
        "Belanja" -> Icons.Filled.ShoppingBag
        "Tagihan" -> Icons.Filled.Receipt
        "Hiburan" -> Icons.Filled.Movie
        "Kesehatan" -> Icons.Filled.Favorite
        "Tabungan" -> Icons.Filled.Savings
        else -> Icons.Filled.MoreHoriz
    }
}

fun walletIcon(name: String): ImageVector {
    val n = name.lowercase()
    return when {
        n.contains("tunai") || n.contains("cash") -> Icons.Filled.Payments
        n.contains("bank") -> Icons.Filled.AccountBalance
        n.contains("wallet") || n.contains("dana") || n.contains("gopay") || n.contains("ovo") -> Icons.Filled.PhoneAndroid
        n.contains("tabung") -> Icons.Filled.Savings
        else -> Icons.Filled.AccountBalanceWallet
    }
}

fun walletColor(name: String): Color {
    val n = name.lowercase()
    return when {
        n.contains("tunai") || n.contains("cash") -> CAT_COLORS[6]
        n.contains("bank") -> CAT_COLORS[1]
        n.contains("wallet") || n.contains("dana") || n.contains("gopay") || n.contains("ovo") -> CAT_COLORS[2]
        n.contains("tabung") -> CAT_COLORS[5]
        else -> CAT_COLORS[4]
    }
}

@Composable
fun IconTile(icon: ImageVector, tint: Color, size: Dp = 40.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.55f)) }
}

@Composable
fun MenuTile(label: String, icon: ImageVector, tint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        IconTile(icon, tint, 56.dp)
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
fun KChip(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit) {
    val p = LocalPal.current
    FilterChip(
        selected = selected, onClick = onClick, label = label, shape = RoundedCornerShape(99.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = p.card, labelColor = p.muted,
            selectedContainerColor = p.text, selectedLabelColor = p.bg
        ),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = p.line, selectedBorderColor = p.text)
    )
}

@Composable
fun GradientFab(onClick: () -> Unit) {
    val p = LocalPal.current
    Box(
        Modifier.size(56.dp).clip(CircleShape).background(Brush.linearGradient(listOf(p.fabA, p.fabB))).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(Icons.Filled.Add, "Tambah transaksi", tint = p.onFab) }
}

@Composable
fun PaletteChooser(selected: String, onPick: (String) -> Unit) {
    val dark = LocalDark.current
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PALETTES.forEach { (k, label) ->
            val p = palette(k, dark)
            Column(Modifier.weight(1f).clickable { onPick(k) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(p.g1, p.g2, p.g3)))
                        .border(if (k == selected) 3.dp else 0.5.dp, if (k == selected) cs.onSurface else cs.outline, RoundedCornerShape(16.dp))
                )
                Spacer(Modifier.height(6.dp))
                Text(label, fontSize = 13.sp, fontWeight = if (k == selected) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}

@Composable
fun CardBox(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun MonthSwitcher(ym: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.clip(CircleShape).background(cs.surface).border(0.5.dp, cs.outline, CircleShape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.ChevronLeft, "Bulan sebelumnya", tint = cs.onSurfaceVariant) }
        Text(monthLabel(ym), fontWeight = FontWeight.Medium, fontSize = 13.sp)
        IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.ChevronRight, "Bulan berikutnya", tint = cs.onSurfaceVariant) }
    }
}

@Composable
fun Bar(p: Float, color: Color) {
    LinearProgressIndicator(
        progress = { p.coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(7.dp)),
        color = color,
        trackColor = MaterialTheme.colorScheme.outline
    )
}

@Composable
fun SectionTitle(t: String) {
    Text(t, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun TxRow(t: Tx, wallets: List<Wallet>, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val isTf = t.type == "tf"
    val tint = if (isTf) cs.onSurfaceVariant else catColor(t.category ?: "Lainnya", t.type == "in")
    val col = when (t.type) {
        "in" -> incomeColor()
        "out" -> cs.error
        else -> cs.onSurface
    }
    val sign = when (t.type) {
        "in" -> "+"
        "out" -> "−"
        else -> ""
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(catIcon(t.category, t.type), tint)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t.note.ifBlank { t.category ?: "Transfer" }, fontWeight = FontWeight.Medium, maxLines = 1)
            val sub = if (isTf) wn(wallets, t.walletId) + " → " + wn(wallets, t.toWalletId ?: 0L)
            else (t.category ?: "") + " · " + wn(wallets, t.walletId)
            Text(
                sub + " · " + shortDate(t.date) + (if (t.auto) " · auto" else ""),
                style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(sign + rp(t.amount), color = col, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun <T> Picker(label: String, selected: String, options: List<Pair<T, String>>, onPick: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selected, Modifier.weight(1f))
                Text("▾")
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { (v, n) ->
                    DropdownMenuItem(text = { Text(n) }, onClick = { onPick(v); open = false })
                }
            }
        }
    }
}
