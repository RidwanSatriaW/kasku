package id.kasku.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun Donut(by: List<Pair<String, Long>>, total: Long, sel: String?, colorOf: (String) -> Color) {
    val track = MaterialTheme.colorScheme.outline
    val cs = MaterialTheme.colorScheme
    Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(18.dp)) {
            drawArc(track, 0f, 360f, false, style = Stroke(width = 13.dp.toPx()))
            var start = -90f
            by.forEach { (c, v) ->
                val sw = 360f * v / total
                val on = sel == null || sel == c
                drawArc(
                    colorOf(c).copy(alpha = if (on) 1f else 0.28f), start, max(0.1f, sw - 2f), false,
                    style = Stroke(width = (if (c == sel) 17 else 13).dp.toPx())
                )
                start += sw
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(sel ?: "Total", fontSize = 11.sp, color = cs.onSurfaceVariant)
            val v = if (sel != null) by.firstOrNull { it.first == sel }?.second ?: total else total
            Text(cp(v), fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun Ring(p: Float, color: Color, size: Dp, stroke: Dp, label: String, fontSize: Int) {
    val track = MaterialTheme.colorScheme.outline
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(stroke / 2 + 2.dp)) {
            drawArc(track, 0f, 360f, false, style = Stroke(width = stroke.toPx()))
            drawArc(color, -90f, 360f * p.coerceIn(0f, 1f), false, style = Stroke(width = stroke.toPx(), cap = StrokeCap.Round))
        }
        Text(label, fontSize = fontSize.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ScoreRing(score: Double, color: Color) = Ring((score / 100).toFloat(), color, 96.dp, 9.dp, score.roundToInt().toString(), 26)

@Composable
fun MonthBars(data: List<Triple<String, Long, Long>>, incomeColor: Color, expenseColor: Color) {
    val mx = max(1L, data.maxOf { max(it.second, it.third) }).toFloat()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        data.forEach { (label, i, o) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.height(104.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(Modifier.width(11.dp).height(max(2f, i / mx * 104f).dp).background(incomeColor, androidx.compose.foundation.shape.RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                    Box(Modifier.width(11.dp).height(max(2f, o / mx * 104f).dp).background(expenseColor, androidx.compose.foundation.shape.RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                }
                Spacer(Modifier.height(6.dp))
                Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
