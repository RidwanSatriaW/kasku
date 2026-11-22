package id.kasku.app.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import id.kasku.app.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import kotlin.math.max

class KaskuVM(app: Application) : AndroidViewModel(app) {
    private val db = KaskuDb.get(app)
    private val dao = db.dao()
    private val prefs = app.getSharedPreferences("kasku", Context.MODE_PRIVATE)

    private fun <T> kotlinx.coroutines.flow.Flow<List<T>>.state() =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<T>())

    val wallets = dao.wallets().state()
    val txs = dao.transactions().state()
    val budgets = dao.budgets().state()
    val recs = dao.recurring().state()
    val goals = dao.goals().state()
    val logs = dao.goalLogs().state()
    val shop = dao.shop().state()

    var month by mutableStateOf(YearMonth.now())
        private set
    var theme by mutableStateOf(prefs.getString("theme", "auto") ?: "auto")
        private set
    var palette by mutableStateOf(prefs.getString("palette", "aurora") ?: "aurora")
        private set
    var hide by mutableStateOf(prefs.getBoolean("hide", false))
        private set
    var apiKey by mutableStateOf(prefs.getString("apikey", "") ?: "")
        private set

    init {
        viewModelScope.launch { runRecurring() }
    }

    fun shift(k: Int) { month = month.plusMonths(k.toLong()) }
    fun resetMonth() { month = YearMonth.now() }
    fun changeTheme(t: String) { theme = t; prefs.edit().putString("theme", t).apply() }
    fun choosePalette(n: String) { palette = n; prefs.edit().putString("palette", n).apply() }
    fun toggleHide() { hide = !hide; prefs.edit().putBoolean("hide", hide).apply() }
    fun saveApiKey(k: String) { apiKey = k.trim(); prefs.edit().putString("apikey", apiKey).apply() }

    // ---------- transaksi ----------
    fun save(t: Tx) {
        month = YearMonth.parse(t.date.take(7))
        viewModelScope.launch { if (t.id == 0L) dao.insertTx(t) else dao.updateTx(t) }
    }

    fun delete(t: Tx) {
        viewModelScope.launch {
            val gid = t.goalId
            if (gid != null) {
                val g = dao.goalById(gid)
                if (g != null && g.closedDate.isEmpty()) {
                    dao.updateGoal(g.copy(saved = max(0L, g.saved + (if (t.type == "in") t.amount else -t.amount))))
                }
                dao.deleteLogByTx(t.id)
            }
            dao.deleteTx(t)
        }
    }

    // ---------- dompet ----------
    fun saveWallet(w: Wallet) {
        viewModelScope.launch { if (w.id == 0L) dao.insertWallet(w) else dao.updateWallet(w) }
    }

    /** done(null) = berhasil, selain itu pesan kenapa gagal. */
    fun deleteWallet(w: Wallet, done: (String?) -> Unit) {
        viewModelScope.launch {
            if (wallets.value.size < 2) done("Minimal harus ada satu dompet")
            else if (dao.walletUsage(w.id) > 0) done("Dompet ini masih dipakai transaksi atau transfer berulang")
            else { dao.deleteWallet(w); done(null) }
        }
    }

    // ---------- budget ----------
    fun setBudgets(m: Map<String, Long>) {
        viewModelScope.launch {
            for ((c, v) in m) if (v > 0) dao.upsertBudget(Budget(c, v)) else dao.deleteBudget(c)
        }
    }

    // ---------- transaksi berulang ----------
    private suspend fun runRecurring() {
        val t = today()
        val ws = dao.walletsOnce()
        for (r0 in dao.recurringOnce()) {
            if (r0.paused) continue
            var r = r0
            var changed = false
            for (k in 0 until 36) {
                val d = nextDue(r)
                if (d > t) break
                dao.insertTx(
                    Tx(
                        type = r.type, amount = r.amount, walletId = r.walletId, toWalletId = r.toWalletId,
                        category = r.category, note = recName(r, ws), date = d, auto = true
                    )
                )
                r = r.copy(last = d.take(7))
                changed = true
            }
            if (changed) dao.updateRec(r)
        }
    }

    fun addRec(r: Recurring) {
        viewModelScope.launch {
            dao.insertRec(r.copy(last = initLast(r.day)))
            runRecurring()
        }
    }

    fun toggleRec(r: Recurring) {
        viewModelScope.launch {
            dao.updateRec(if (r.paused) r.copy(paused = false, last = initLast(r.day)) else r.copy(paused = true))
        }
    }

    fun deleteRec(r: Recurring) { viewModelScope.launch { dao.deleteRec(r) } }

    // ---------- target tabungan ----------
    fun saveGoal(g: Goal, initial: Long) {
        viewModelScope.launch {
            if (g.id == 0L) {
                val id = dao.insertGoal(g.copy(saved = initial))
                if (initial > 0) dao.insertLog(GoalLog(goalId = id, date = today(), type = "in", amount = initial, note = "Saldo awal"))
            } else dao.updateGoal(g)
        }
    }

    fun deposit(gid: Long, v: Long, w: Long) {
        viewModelScope.launch {
            val g = dao.goalById(gid) ?: return@launch
            val t = today()
            val tid = dao.insertTx(Tx(type = "out", amount = v, walletId = w, category = "Tabungan", note = "Setor: " + g.name, date = t, goalId = g.id))
            dao.updateGoal(g.copy(saved = g.saved + v))
            dao.insertLog(GoalLog(goalId = g.id, txId = tid, date = t, type = "in", amount = v, walletId = w, note = "Setoran"))
        }
    }

    fun withdraw(gid: Long, v: Long, w: Long) {
        viewModelScope.launch {
            val g = dao.goalById(gid) ?: return@launch
            if (v > g.saved) return@launch
            val t = today()
            val tid = dao.insertTx(Tx(type = "in", amount = v, walletId = w, category = "Tabungan", note = "Tarik: " + g.name, date = t, goalId = g.id, ref = true))
            dao.updateGoal(g.copy(saved = g.saved - v))
            dao.insertLog(GoalLog(goalId = g.id, txId = tid, date = t, type = "out", amount = v, walletId = w, note = "Penarikan"))
        }
    }

    /** w = 0 berarti dana tidak dikembalikan ke dompet manapun. */
    fun closeGoal(gid: Long, w: Long) {
        viewModelScope.launch {
            val g = dao.goalById(gid) ?: return@launch
            val t = today()
            val amt = g.saved
            if (amt > 0 && w != 0L) {
                val tid = dao.insertTx(Tx(type = "in", amount = amt, walletId = w, category = "Tabungan", note = "Dikembalikan: " + g.name, date = t, goalId = g.id, ref = true))
                dao.insertLog(GoalLog(goalId = g.id, txId = tid, date = t, type = "out", amount = amt, walletId = w, note = "Dikembalikan ke dompet"))
                dao.updateGoal(g.copy(closedDate = t, closedWallet = w, closedAmount = amt, saved = 0))
            } else {
                if (amt > 0) dao.insertLog(GoalLog(goalId = g.id, date = t, type = "out", amount = amt, note = "Ditutup tanpa pengembalian"))
                dao.updateGoal(g.copy(closedDate = t, closedWallet = 0, closedAmount = 0, closedKept = amt))
            }
        }
    }

    // ---------- daftar belanja ----------
    fun addShop(name: String, price: Long) { viewModelScope.launch { dao.insertShop(ShopItem(name = name, price = price)) } }
    fun toggleShop(s: ShopItem) { viewModelScope.launch { dao.updateShop(s.copy(done = !s.done)) } }
    fun deleteShop(s: ShopItem) { viewModelScope.launch { dao.deleteShop(s) } }

    fun buyShop(w: Long) {
        viewModelScope.launch {
            val d = shop.value.filter { it.done }
            val total = d.sumOf { it.price }
            if (total > 0) {
                dao.insertTx(Tx(type = "out", amount = total, walletId = w, category = "Belanja", note = ("Belanja: " + d.joinToString(", ") { it.name }).take(60), date = today()))
                resetMonth()
            }
            dao.deleteDoneShop()
        }
    }

    // ---------- ekspor dan impor ----------
    private fun snapshot() = Snapshot(wallets.value, txs.value, budgets.value, recs.value, goals.value, logs.value, shop.value, hide, theme)
    fun exportJson(): String = toJson(snapshot())
    fun exportCsv(): String = toCsv(txs.value, wallets.value)

    fun importJson(text: String, done: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val s = parseSnapshot(text)
                db.withTransaction {
                    dao.clearTx(); dao.clearWallets(); dao.clearBudgets(); dao.clearRec(); dao.clearGoals(); dao.clearLogs(); dao.clearShop()
                    s.wallets.forEach { dao.insertWallet(it) }
                    s.txs.forEach { dao.insertTx(it) }
                    s.budgets.forEach { dao.upsertBudget(it) }
                    s.recs.forEach { dao.insertRec(it) }
                    s.goals.forEach { dao.insertGoal(it) }
                    s.logs.forEach { dao.insertLog(it) }
                    s.shop.forEach { dao.insertShop(it) }
                }
                if (s.wallets.isEmpty()) dao.insertWallet(Wallet(name = "Tunai"))
                changeTheme(s.theme)
                if (s.hide != hide) toggleHide()
                resetMonth()
                runRecurring()
                done(true)
            } catch (e: Exception) {
                done(false)
            }
        }
    }
}
