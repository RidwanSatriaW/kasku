package id.kasku.app.ui

import id.kasku.app.data.Budget
import id.kasku.app.data.Recurring
import id.kasku.app.data.Tx
import id.kasku.app.data.Wallet
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class Health(
    val score: Double?, val sr: Double?, val nb: Int, val ok: Int,
    val mo: Double?, val p1: Double?, val p2: Double?, val p3: Double?
)

fun health(txs: List<Tx>, wallets: List<Wallet>, budgets: List<Budget>, m: String): Health {
    val st = monthStat(txs, m)
    val sr = if (st.income > 0) (st.income - st.spending).toDouble() / st.income else null
    val bt = budgets.filter { it.limitAmount > 0 }
    val ok = bt.count { st.out(it.category) <= it.limitAmount }
    val a3 = (0..2).map { monthStat(txs, addM(m, -it)).spending }.filter { it > 0 }
    val avg = if (a3.isNotEmpty()) a3.sum().toDouble() / a3.size else 0.0
    val total = wallets.sumOf { balance(it, txs) }
    val mo = if (avg > 0) max(0L, total) / avg else null
    val p1 = sr?.let { min(1.0, max(0.0, it / 0.2)) }
    val p2 = if (bt.isNotEmpty()) ok.toDouble() / bt.size else null
    val p3 = mo?.let { min(1.0, it / 6) }
    var sw = 0.0
    var sc = 0.0
    listOf(p1 to 40.0, p2 to 30.0, p3 to 30.0).forEach { (p, w) ->
        if (p != null) { sw += w; sc += p * w }
    }
    return Health(if (sw > 0) sc / sw * 100 else null, sr, bt.size, ok, mo, p1, p2, p3)
}

private fun f1(x: Double): String = String.format(ID, "%.1f", x)

fun insights(txs: List<Tx>, wallets: List<Wallet>, budgets: List<Budget>, recs: List<Recurring>, m: String): List<String> {
    if (txs.none { it.date.startsWith(m) }) return listOf("Catat transaksi pertamamu supaya analisis muncul di sini.")
    val st = monthStat(txs, m)
    val pst = monthStat(txs, addM(m, -1))
    val h = health(txs, wallets, budgets, m)
    val isCur = m == curM()
    val l = ArrayList<String>()
    val o = st.byCat.filter { it.first != "Tabungan" }
    if (o.isNotEmpty() && st.spending > 0) {
        l.add("Pengeluaran terbesar adalah ${o[0].first}, ${o[0].second * 100 / st.spending}% dari total belanja bulan ini.")
    }
    if (pst.spending > 0 && st.spending > 0) {
        if (isCur) {
            l.add("Belanja bulan ini sudah ${st.spending * 100 / pst.spending}% dari total belanja bulan lalu.")
        } else {
            val d = Math.round((st.spending - pst.spending) * 100.0 / pst.spending).toInt()
            if (abs(d) >= 5) l.add("Belanja ${if (d > 0) "naik" else "turun"} ${abs(d)}% dibanding bulan sebelumnya.")
        }
    }
    val over = budgets.filter { it.limitAmount > 0 && st.out(it.category) > it.limitAmount }.map { it.category }
    if (over.isNotEmpty()) l.add("Melewati budget: ${over.joinToString(", ")}. Pertimbangkan menaikkan budget atau menahan belanja kategori ini.")
    val sr = h.sr
    if (sr != null) {
        if (sr < 0) l.add("Pengeluaran lebih besar dari pemasukan bulan ini. Tutup selisihnya dari pos yang paling fleksibel dulu, yaitu keinginan.")
        else if (sr < 0.1) l.add("Tingkat tabungan ${Math.round(sr * 100)}%. Targetkan minimal 10% dulu, lalu naik bertahap ke 20%.")
        else if (sr >= 0.2) l.add("Tingkat tabungan ${Math.round(sr * 100)}% sudah mencapai target 20%. Pertahankan.")
    }
    val au = txs.filter { it.date.startsWith(m) && it.auto && it.type == "out" }.sumOf { it.amount }
    if (au > 0 && st.spending > 0) {
        l.add("Tagihan dan cicilan otomatis mengambil ${rp(au)} (${au * 100 / st.spending}% dari belanja). Tinjau langganan yang jarang dipakai.")
    }
    val mo = h.mo
    if (mo != null) {
        if (mo < 3) l.add("Dana darurat baru cukup untuk ${f1(mo)} bulan. Targetkan 3 sampai 6 bulan pengeluaran.")
        else if (mo < 6) l.add("Dana darurat cukup untuk ${f1(mo)} bulan. Sedikit lagi menuju 6 bulan.")
        else l.add("Dana daruratmu sudah di atas 6 bulan pengeluaran. Pertimbangkan menaruh kelebihannya ke tujuan jangka panjang.")
    }
    if (st.income > 0 && recs.none { it.type == "tf" && !it.paused }) {
        l.add("Bayar diri sendiri lebih dulu: buat transfer berulang ke dompet Tabungan tiap tanggal gajian.")
    }
    return l.take(5)
}
