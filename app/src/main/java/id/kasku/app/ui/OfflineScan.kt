package id.kasku.app.ui

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.LocalDate
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Scan struk offline: OCR ML Kit di HP, lalu hasilnya diurai dengan aturan sederhana. */
suspend fun scanOffline(ctx: Context, uri: Uri): ScanResult {
    val image = InputImage.fromFilePath(ctx, uri)
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val text = suspendCancellableCoroutine<String> { cont ->
        recognizer.process(image)
            .addOnSuccessListener { cont.resume(it.text) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }
    recognizer.close()
    if (text.isBlank()) throw IllegalStateException("tidak ada teks terbaca")
    return parseReceipt(text)
}

private val NUM = Regex("""\d{1,3}(?:[.,]\d{3})+(?:[.,]\d{1,2})?|\d+(?:[.,]\d{1,2})?""")
private val SKIP_TOTAL = listOf("sub", "kembali", "tunai", "cash", "ppn", "pajak", "diskon", "hemat", "item", "qty", "poin", "point")

private fun amountOf(s: String): Long? {
    val t = s.trim().replace(Regex("""[.,]\d{2}$"""), "")
    val d = t.filter(Char::isDigit)
    return if (d.isEmpty() || d.length > 12) null else d.toLongOrNull()
}

private fun amountsIn(line: String): List<Long> =
    NUM.findAll(line).mapNotNull { amountOf(it.value) }.filter { it >= 100 }.toList()

private fun findTotal(lines: List<String>): Long {
    val groups = listOf(
        listOf("grand total", "total bayar", "total belanja", "total tagihan", "total harga", "jumlah bayar", "total pembayaran"),
        listOf("total"),
        listOf("jumlah", "tagihan", "amount")
    )
    for (keys in groups) {
        var found = 0L
        for ((i, raw) in lines.withIndex()) {
            val l = raw.lowercase()
            if (keys.none { l.contains(it) }) continue
            if (SKIP_TOTAL.any { l.contains(it) }) continue
            val a = amountsIn(raw).ifEmpty { if (i + 1 < lines.size) amountsIn(lines[i + 1]) else emptyList() }
            if (a.isNotEmpty()) found = a.max()
        }
        if (found > 0) return found
    }
    // cadangan: angka terbesar di struk, di luar baris tunai/kembalian
    var best = 0L
    for (raw in lines) {
        val l = raw.lowercase()
        if (listOf("tunai", "cash", "kembali", "telp", "tel", "npwp", "no.", "id ").any { l.contains(it) }) continue
        amountsIn(raw).filter { it < 1_000_000_000L }.forEach { if (it > best) best = it }
    }
    return best
}

private val MONTHS = listOf("jan", "feb", "mar", "apr", "mei", "jun", "jul", "agu", "sep", "okt", "nov", "des")

private fun mk(y: Int, m: Int, d: Int): String? = try {
    val yy = if (y < 100) 2000 + y else y
    LocalDate.of(yy, m, d).toString()
} catch (e: Exception) { null }

private fun findDate(text: String): String? {
    Regex("""(\d{4})[/\-.](\d{1,2})[/\-.](\d{1,2})""").findAll(text).forEach {
        mk(it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt())?.let { d -> return d }
    }
    Regex("""(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{2,4})""").findAll(text).forEach {
        mk(it.groupValues[3].toInt(), it.groupValues[2].toInt(), it.groupValues[1].toInt())?.let { d -> return d }
    }
    Regex("""(\d{1,2})\s+([A-Za-z]{3,})\s+(\d{2,4})""").findAll(text).forEach {
        val mon = it.groupValues[2].lowercase().take(3).replace("aug", "agu").replace("oct", "okt").replace("dec", "des")
        val mi = MONTHS.indexOf(mon)
        if (mi >= 0) mk(it.groupValues[3].toInt(), mi + 1, it.groupValues[1].toInt())?.let { d -> return d }
    }
    return null
}

private fun guessCategory(t: String): String {
    val l = t.lowercase()
    fun has(vararg k: String) = k.any { l.contains(it) }
    return when {
        has("pertamina", "shell", "spbu", "grab", "gojek", "gocar", "kai", "tol", "parkir", "bluebird") -> "Transport"
        has("pln", "pdam", "telkom", "indihome", "token listrik", "pulsa", "bpjs") -> "Tagihan"
        has("apotek", "klinik", "kimia farma", "guardian", "rumah sakit", "dokter") -> "Kesehatan"
        has("xxi", "cinema", "cgv", "bioskop", "netflix", "spotify") -> "Hiburan"
        has("resto", "warung", "kopi", "coffee", "cafe", "bakery", "bakso", "ayam", "nasi", "mie", "sate", "pizza", "burger", "kfc", "mcd", "starbucks", "dapur") -> "Makanan"
        has("indomaret", "alfamart", "alfamidi", "supermarket", "hypermart", "transmart", "giant", "lotte", "ranch market", "mart", "toko") -> "Belanja"
        else -> "Belanja"
    }
}

fun parseReceipt(text: String): ScanResult {
    val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
    val merchant = lines.take(6).firstOrNull { l -> l.count(Char::isLetter) >= 3 && l.count(Char::isLetter) > l.count(Char::isDigit) } ?: ""
    return ScanResult(merchant, findDate(text), findTotal(lines), guessCategory(text), emptyList())
}
