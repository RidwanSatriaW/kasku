package id.kasku.app.ui

import id.kasku.app.data.*
import org.json.JSONArray
import org.json.JSONObject

/** Format JSON sama dengan backup versi web Kasku, jadi bisa saling pulihkan. */
class Snapshot(
    val wallets: List<Wallet>,
    val txs: List<Tx>,
    val budgets: List<Budget>,
    val recs: List<Recurring>,
    val goals: List<Goal>,
    val logs: List<GoalLog>,
    val shop: List<ShopItem>,
    val hide: Boolean,
    val theme: String
)

private fun JSONObject.flag(k: String): Boolean = when (val v = opt(k)) {
    is Boolean -> v
    is Number -> v.toInt() != 0
    else -> false
}

private fun JSONObject.longOrNull(k: String): Long? = if (has(k) && !isNull(k)) optLong(k) else null
private fun JSONObject.str(k: String): String = if (has(k) && !isNull(k)) optString(k) else ""
private fun JSONObject.strOrNull(k: String): String? = if (has(k) && !isNull(k)) optString(k) else null

fun toJson(s: Snapshot): String {
    val o = JSONObject()
    o.put("w", JSONArray().also { a ->
        s.wallets.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("init", it.initialBalance)) }
    })
    o.put("tx", JSONArray().also { a ->
        s.txs.forEach { t ->
            val j = JSONObject().put("id", t.id).put("type", t.type).put("amount", t.amount)
                .put("wallet", t.walletId).put("note", t.note).put("date", t.date)
            t.toWalletId?.let { j.put("to", it) }
            t.category?.let { j.put("cat", it) }
            if (t.auto) j.put("auto", 1)
            t.goalId?.let { j.put("gid", it) }
            if (t.ref) j.put("ref", 1)
            a.put(j)
        }
    })
    o.put("shop", JSONArray().also { a ->
        s.shop.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("price", it.price).put("done", it.done)) }
    })
    o.put("bud", JSONObject().also { b -> s.budgets.forEach { b.put(it.category, it.limitAmount) } })
    o.put("goal", JSONArray().also { a ->
        s.goals.forEach { g ->
            val j = JSONObject().put("id", g.id).put("name", g.name).put("target", g.target)
                .put("saved", g.saved).put("date", g.date).put("c", g.colorIdx)
            j.put("log", JSONArray().also { la ->
                s.logs.filter { it.goalId == g.id }.forEach { l ->
                    la.put(JSONObject().put("id", l.txId ?: l.id).put("date", l.date).put("type", l.type)
                        .put("amount", l.amount).put("wallet", l.walletId).put("note", l.note))
                }
            })
            if (g.closedDate.isNotEmpty()) {
                j.put("closed", JSONObject().put("date", g.closedDate).put("wallet", g.closedWallet)
                    .put("amount", g.closedAmount).put("kept", g.closedKept))
            }
            a.put(j)
        }
    })
    o.put("rec", JSONArray().also { a ->
        s.recs.forEach { r ->
            val j = JSONObject().put("id", r.id).put("type", r.type).put("amount", r.amount)
                .put("wallet", r.walletId).put("note", r.note).put("day", r.day).put("last", r.last)
                .put("paused", if (r.paused) 1 else 0)
            r.toWalletId?.let { j.put("to", it) }
            r.category?.let { j.put("cat", it) }
            a.put(j)
        }
    })
    o.put("ui", JSONObject().put("hide", if (s.hide) 1 else 0).put("theme", s.theme))
    return o.toString(1)
}

/** Melempar exception kalau bukan backup Kasku yang valid. */
fun parseSnapshot(text: String): Snapshot {
    val o = JSONObject(text)
    val wa = o.getJSONArray("w")
    val ta = o.getJSONArray("tx")
    val wallets = (0 until wa.length()).map {
        val j = wa.getJSONObject(it)
        Wallet(j.getLong("id"), j.str("name"), j.optLong("init", 0L))
    }
    val txs = (0 until ta.length()).map {
        val j = ta.getJSONObject(it)
        Tx(
            id = j.getLong("id"), type = j.str("type"), amount = j.optLong("amount", 0L),
            walletId = j.optLong("wallet", 0L), toWalletId = j.longOrNull("to"),
            category = j.strOrNull("cat"), note = j.str("note"), date = j.str("date"),
            auto = j.flag("auto"), goalId = j.longOrNull("gid"), ref = j.flag("ref")
        )
    }
    val budgets = ArrayList<Budget>()
    o.optJSONObject("bud")?.let { b -> b.keys().forEach { k -> budgets.add(Budget(k, b.optLong(k, 0L))) } }
    val shop = ArrayList<ShopItem>()
    o.optJSONArray("shop")?.let { a ->
        for (i in 0 until a.length()) {
            val j = a.getJSONObject(i)
            shop.add(ShopItem(j.getLong("id"), j.str("name"), j.optLong("price", 0L), j.flag("done")))
        }
    }
    val recs = ArrayList<Recurring>()
    o.optJSONArray("rec")?.let { a ->
        for (i in 0 until a.length()) {
            val j = a.getJSONObject(i)
            recs.add(
                Recurring(
                    id = j.getLong("id"), type = j.str("type"), amount = j.optLong("amount", 0L),
                    walletId = j.optLong("wallet", 0L), toWalletId = j.longOrNull("to"),
                    category = j.strOrNull("cat"), note = j.str("note"), day = j.optInt("day", 1),
                    last = j.str("last").ifEmpty { curM() }, paused = j.flag("paused")
                )
            )
        }
    }
    val goals = ArrayList<Goal>()
    val logs = ArrayList<GoalLog>()
    o.optJSONArray("goal")?.let { a ->
        for (i in 0 until a.length()) {
            val j = a.getJSONObject(i)
            val gid = j.getLong("id")
            val c = j.optJSONObject("closed")
            goals.add(
                Goal(
                    id = gid, name = j.str("name"), target = j.optLong("target", 0L), saved = j.optLong("saved", 0L),
                    date = j.str("date"), colorIdx = j.optInt("c", 0),
                    closedDate = c?.str("date") ?: "", closedWallet = c?.optLong("wallet", 0L) ?: 0L,
                    closedAmount = c?.optLong("amount", 0L) ?: 0L, closedKept = c?.optLong("kept", 0L) ?: 0L
                )
            )
            val la = j.optJSONArray("log")
            if (la != null) {
                for (k in 0 until la.length()) {
                    val l = la.getJSONObject(k)
                    val lid = l.optLong("id", 0L)
                    logs.add(
                        GoalLog(
                            goalId = gid, txId = if (lid != 0L && txs.any { it.id == lid }) lid else null,
                            date = l.str("date"), type = l.str("type"), amount = l.optLong("amount", 0L),
                            walletId = l.optLong("wallet", 0L), note = l.str("note")
                        )
                    )
                }
            }
        }
    }
    val ui = o.optJSONObject("ui")
    return Snapshot(wallets, txs, budgets, recs, goals, logs, shop, ui?.flag("hide") ?: false, ui?.str("theme")?.ifEmpty { "auto" } ?: "auto")
}

fun toCsv(txs: List<Tx>, wallets: List<Wallet>): String {
    val sb = StringBuilder("tanggal,jenis,kategori,dompet,tujuan,jumlah,catatan\n")
    txs.sortedBy { it.date }.forEach { t ->
        sb.append(t.date).append(',').append(t.type).append(',').append(t.category ?: "").append(',')
            .append('"').append(wn(wallets, t.walletId)).append("\",")
            .append(if (t.toWalletId != null) "\"" + wn(wallets, t.toWalletId) + "\"" else "").append(',')
            .append(t.amount).append(",\"").append(t.note.replace("\"", "\"\"")).append("\"\n")
    }
    return sb.toString()
}
