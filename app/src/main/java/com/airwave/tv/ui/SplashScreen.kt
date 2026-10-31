package com.airwave.tv.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.first

private const val IntroMs = 2300f
private const val OutroMs = 450

/** Fast start, long soft landing. */
private val EaseOutExpo = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val EaseInOut = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

/**
 * Logo intro, timed off one clock (milliseconds):
 *  100–520  the white play button lands
 *  380–940  the two waves draw out of it, one after the other
 *  950–1700 the mark glides left and uncovers the wordmark that was hidden behind it
 * The lockup then holds (with a quiet loader if channels are still loading), fades out,
 * and [onFinished] is called.
 */
@Composable
fun Splash(ready: Boolean, onFinished: () -> Unit) {
    val intro = remember { Animatable(0f) }
    val outro = remember { Animatable(0f) }
    var holding by remember { mutableStateOf(false) }
    val isReady by rememberUpdatedState(ready)
    val finish by rememberUpdatedState(onFinished)
    LaunchedEffect(Unit) {
        intro.animateTo(IntroMs, tween(IntroMs.toInt(), easing = LinearEasing))
        holding = true
        snapshotFlow { isReady }.first { it }
        holding = false
        outro.animateTo(1f, tween(OutroMs, easing = LinearEasing))
        finish()
    }

    val scan by rememberInfiniteTransition(label = "splashLoader").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1100, easing = EaseInOut)), label = "scan",
    )
    val loader by animateFloatAsState(
        if (holding) 1f else 0f, tween(400, delayMillis = if (holding) 500 else 0), label = "loader",
    )

    val measurer = rememberTextMeasurer()
    val wordmark = remember(measurer) {
        measurer.measure(
            buildAnnotatedString {
                append("Airwave")
                withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.62f))) {
                    append(" TV")
                }
            },
            TextStyle(color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        )
    }

    Canvas(
        Modifier
            .fillMaxSize()
            .background(Palette.Bg),
    ) {
        val t = intro.value
        val o = outro.value
        val k = 1.6.dp.toPx() // size of one logo grid unit
        // The mark (play button + waves) spans x 33..79, y 32..76 on the logo's 108-unit grid.
        val markW = 46f * k
        val gap = 34.dp.toPx()
        val lockupW = markW + gap + wordmark.size.width
        val cx = size.width / 2f
        val cy = size.height / 2f

        val slide = t.phase(950f, 1700f, EaseInOut)
        val markCx = lerp(cx, cx - lockupW / 2f + markW / 2f, slide)
        fun grid(x: Float, y: Float) = Offset(markCx + (x - 56f) * k, cy + (y - 54f) * k)

        val exit = EaseInCubic.transform(o)
        val exitScale = 1f - 0.04f * exit

        withTransform({ scale(exitScale, exitScale, Offset(cx, cy)) }) {
            drawIntoCanvas { it.saveLayer(Rect(Offset.Zero, size), Paint().apply { alpha = 1f - exit }) }

            // Play button lands.
            val triAlpha = t.phase(100f, 240f)
            if (triAlpha > 0f) {
                val land = t.phase(100f, 520f, EaseOutExpo)
                val a = grid(35f, 39f)
                val b = grid(35f, 69f)
                val c = grid(57f, 54f)
                val triangle = Path().apply {
                    moveTo(a.x, a.y)
                    lineTo(b.x, b.y)
                    lineTo(c.x, c.y)
                    close()
                }
                val s = lerp(0.55f, 1f, land)
                withTransform({ scale(s, s, grid(42f, 54f)) }) {
                    drawPath(triangle, Color.White, alpha = triAlpha)
                    drawPath(triangle, Color.White, alpha = triAlpha, style = Stroke(4f * k, join = StrokeJoin.Round))
                }
            }

            // Waves draw outwards from their middle.
            val waveCenter = grid(46f, 54f)
            listOf(Triple(22f, 380f, 820f), Triple(31f, 500f, 940f)).forEach { (radius, start, end) ->
                val p = t.phase(start, end, EaseOutExpo)
                if (p > 0f) {
                    val rr = radius * k
                    val sweep = 80f * p
                    drawArc(
                        Color.White,
                        startAngle = -sweep / 2f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(waveCenter.x - rr, waveCenter.y - rr),
                        size = Size(2f * rr, 2f * rr),
                        alpha = t.phase(start, start + 60f),
                        style = Stroke(4f * k, cap = StrokeCap.Round),
                    )
                }
            }

            // Wordmark is uncovered left to right, starting from behind the mark as it glides left.
            val textX = cx - lockupW / 2f + markW + gap + (1f - slide) * 16.dp.toPx()
            val edge = markCx + markW / 2f + gap * 0.4f
            val wipe = textX + wordmark.size.width * t.phase(1050f, 1750f, EaseInOut) + 8.dp.toPx()
            // An empty (inverted) clip would draw unclipped, so only draw once the wipe has passed the mark.
            if (wipe > edge) {
                clipRect(left = edge, top = 0f, right = wipe, bottom = size.height) {
                    drawText(
                        wordmark,
                        topLeft = Offset(textX, cy - wordmark.size.height / 2f),
                        alpha = t.phase(950f, 1400f),
                    )
                }
            }
            drawIntoCanvas { it.restore() }
        }

        // Quiet loader, only while channels are still loading after the intro.
        if (loader > 0f) {
            val half = 90.dp.toPx()
            val y = cy + 110.dp.toPx()
            val stroke = 2.dp.toPx()
            val alpha = loader * (1f - exit)
            drawLine(
                Color.White.copy(alpha = 0.10f),
                Offset(cx - half, y),
                Offset(cx + half, y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
                alpha = alpha,
            )
            val seg = 36.dp.toPx()
            val x = lerp(cx - half, cx + half - seg, scan)
            drawLine(
                Color.White.copy(alpha = 0.75f),
                Offset(x, y),
                Offset(x + seg, y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
                alpha = alpha,
            )
        }
    }
}

private fun Float.phase(start: Float, end: Float, easing: Easing = LinearEasing): Float =
    easing.transform(((this - start) / (end - start)).coerceIn(0f, 1f))

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
