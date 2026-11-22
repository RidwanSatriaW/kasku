package id.kasku.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max
import kotlin.math.min

class ScanResult(val merchant: String, val date: String?, val total: Long, val category: String, val items: List<String>)

private const val MODEL = "claude-sonnet-4-6"

/** Memperkecil foto struk (maks 1400 px) lalu mengubahnya ke JPEG base64. */
fun loadJpegB64(ctx: Context, uri: Uri): String {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var s = 1
    while (max(bounds.outWidth, bounds.outHeight) / s > 2800) s *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = s }
    val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        ?: throw IllegalStateException("Foto tidak bisa dibuka")
    val k = min(1f, 1400f / max(bmp.width, bmp.height))
    val sc = if (k < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * k).toInt(), (bmp.height * k).toInt(), true) else bmp
    val bo = ByteArrayOutputStream()
    sc.compress(Bitmap.CompressFormat.JPEG, 80, bo)
    return Base64.encodeToString(bo.toByteArray(), Base64.NO_WRAP)
}

suspend fun scanReceipt(b64: String, apiKey: String): ScanResult = withContext(Dispatchers.IO) {
    val prompt = "Baca struk belanja pada gambar. Jawab HANYA JSON: " +
        "{\"merchant\":string,\"date\":\"YYYY-MM-DD\" atau null,\"total\":angka rupiah tanpa titik," +
        "\"category\":salah satu dari ${JSONArray(OUT_CATS)},\"items\":[string]}"
    val img = JSONObject().put("type", "image").put(
        "source", JSONObject().put("type", "base64").put("media_type", "image/jpeg").put("data", b64)
    )
    val body = JSONObject().put("model", MODEL).put("max_tokens", 1000).put(
        "messages", JSONArray().put(
            JSONObject().put("role", "user").put(
                "content", JSONArray().put(img).put(JSONObject().put("type", "text").put("text", prompt))
            )
        )
    )
    val conn = URL("https://api.anthropic.com/v1/messages").openConnection() as HttpURLConnection
    try {
        conn.requestMethod = "POST"
        conn.connectTimeout = 20000
        conn.readTimeout = 60000
        conn.doOutput = true
        conn.setRequestProperty("content-type", "application/json")
        conn.setRequestProperty("x-api-key", apiKey)
        conn.setRequestProperty("anthropic-version", "2023-06-01")
        conn.outputStream.use { it.write(body.toString().toByteArray()) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) {
            throw IllegalStateException(if (code == 401) "API key ditolak (401)" else "Layanan scan menjawab $code")
        }
        val content = JSONObject(text).getJSONArray("content")
        val sb = StringBuilder()
        for (i in 0 until content.length()) sb.append(content.getJSONObject(i).optString("text"))
        val raw = sb.toString().replace("```json", "").replace("```", "")
        val a = raw.indexOf('{')
        val b = raw.lastIndexOf('}')
        if (a < 0 || b <= a) throw IllegalStateException("Balasan bukan JSON")
        val o = JSONObject(raw.substring(a, b + 1))
        val t = o.opt("total")
        val total = when (t) {
            is Number -> t.toLong()
            is String -> t.replace(Regex("(?i)rp|\\s"), "").replace(Regex("[.,]\\d{1,2}\$"), "").filter(Char::isDigit).toLongOrNull() ?: 0L
            else -> 0L
        }
        val items = ArrayList<String>()
        o.optJSONArray("items")?.let { ia -> for (i in 0 until ia.length()) items.add(ia.optString(i)) }
        ScanResult(
            o.optString("merchant", ""),
            if (o.isNull("date")) null else o.optString("date"),
            total,
            o.optString("category", "Belanja"),
            items
        )
    } finally {
        conn.disconnect()
    }
}
