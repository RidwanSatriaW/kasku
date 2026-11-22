package id.kasku.app.ui

import id.kasku.app.data.Recurring
import id.kasku.app.data.Tx
import id.kasku.app.data.Wallet
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToLong

val ID: Locale = Locale("id", "ID")
private val nf: NumberFormat = NumberFormat.getInstance(ID)

val MON = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
val MONTH_FULL = listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")

val OUT_CATS = listOf("Makanan", "Transport", "Belanja", "Tagihan", "Hiburan", "Kesehatan", "Tabungan", "Lainnya")
val IN_CATS = listOf("Gaji", "Bonus", "Usaha", "Lainnya")
val NEED = listOf("Makanan", "Transport", "Tagihan", "Kesehatan")
val WANT = listOf("Belanja", "Hiburan", "Lainnya")

fun rp(n: Long): String = (if (n < 0) "-" else "") + "Rp" + nf.format(abs(n))
fun money(n: Long, hide: Boolean): String = if (hide) "Rp••••••" else rp(n)

fun cp(n: Long): String {
    val a = abs(n)
    val s = if (n < 0) "-" else ""
    return s + when {
        a >= 1_000_000_000L -> String.format(ID, "%.1f", a / 1e9) + " M"
        a >= 10_000_000L -> String.format(ID, "%.0f", a / 1e6) + " jt"
        a >= 1_000_000L -> String.format(ID, "%.1f", a / 1e6) + " jt"
        a >= 1000L -> (a / 1000.0).roundToLong().toString() + " rb"
        else -> a.toString()
    }
}

fun monthLabel(ym: YearMonth): String = MONTH_FULL[ym.monthValue - 1] + " " + ym.year

fun shortDate(d: String): String {
    val l = LocalDate.parse(d)
    return "${l.dayOfMonth} ${MON[l.monthValue - 1]}"
}

fun dayLabel(d: String): String {
    val l = LocalDate.parse(d)
    val t = LocalDate.now()
    return when (l) {
        t -> "Hari ini"
        t.minusDays(1) -> "Kemarin"
        else -> "${l.dayOfMonth} ${MONTH_FULL[l.monthValue - 1]} ${l.year}"
    }
}

fun pad(n: Int): String = n.toString().padStart(2, '0')
fun today(): String = LocalDate.now().toString()
fun curM(): String = YearMonth.now().toString()
fun addM(m: String, k: Int): String = YearMonth.parse(m).plusMonths(k.toLong()).toString()
fun nextDue(r: Recurring): String = addM(r.last, 1) + "-" + pad(r.day)
fun initLast(day: Int): String = if (day <= LocalDate.now().dayOfMonth) curM() else addM(curM(), -1)
fun dayDiff(a: String, b: String): Long = ChronoUnit.DAYS.between(LocalDate.parse(a), LocalDate.parse(b))

fun wn(wallets: List<Wallet>, id: Long): String = wallets.firstOrNull { it.id == id }?.name ?: "?"

fun recName(r: Recurring, wallets: List<Wallet>): String =
    r.note.ifBlank { if (r.type == "tf") wn(wallets, r.walletId) + " → " + wn(wallets, r.toWalletId ?: 0L) else r.category ?: "Berulang" }

/** Saldo dompet = saldo awal + masuk - keluar - transfer keluar + transfer masuk. */
fun balance(w: Wallet, txs: List<Tx>): Long {
    var b = w.initialBalance
    for (t in txs) {
        when (t.type) {
            "in" -> if (t.walletId == w.id) b += t.amount
            "out" -> if (t.walletId == w.id) b -= t.amount
            "tf" -> {
                if (t.walletId == w.id) b -= t.amount
                if (t.toWalletId == w.id) b += t.amount
            }
        }
    }
    return b
}

class MonthStat(
    val income: Long,
    val expense: Long,
    val byCat: List<Pair<String, Long>>,
    val incByCat: List<Pair<String, Long>>
) {
    val saving: Long get() = out("Tabungan")
    val spending: Long get() = expense - saving
    fun out(c: String): Long = byCat.firstOrNull { it.first == c }?.second ?: 0L
}

fun monthStat(txs: List<Tx>, ym: String): MonthStat {
    var i = 0L
    var o = 0L
    val oc = HashMap<String, Long>()
    val ic = HashMap<String, Long>()
    val l = txs.filter { it.date.startsWith(ym) }
    for (t in l) {
        if (t.ref) continue
        val c = t.category ?: "Lainnya"
        if (t.type == "in") { i += t.amount; ic[c] = (ic[c] ?: 0L) + t.amount }
        if (t.type == "out") { o += t.amount; oc[c] = (oc[c] ?: 0L) + t.amount }
    }
    for (t in l) {
        if (t.ref) {
            oc["Tabungan"] = max(0L, (oc["Tabungan"] ?: 0L) - t.amount)
            o = max(0L, o - t.amount)
        }
    }
    return MonthStat(
        i, o,
        oc.toList().filter { it.second > 0 }.sortedByDescending { it.second },
        ic.toList().filter { it.second > 0 }.sortedByDescending { it.second }
    )
}
