package id.myapp.progresshubkt

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Real frosted-glass panel. The particle field behind every screen is
 * fully known (shared via [LocalParticleField]), so instead of faking a
 * blur with a translucent gradient, this card redraws that exact same
 * field — translated so it lines up with the card's own position on
 * screen — into its own local Canvas, then blurs *only that layer* with a
 * genuine RenderEffect blur (Modifier.blur, Android 12+). A soft tint and
 * a top-lit border sit on top of the blur, and the actual content (text,
 * inputs) is a separate, unblurred layer on top of everything — so the
 * glass looks properly frosted without ever blurring what you're reading. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = GlassBorder,
    radius: androidx.compose.ui.unit.Dp = 20.dp,
    hero: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(radius)
    val field = LocalParticleField.current
    var positionInRoot by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .onGloballyPositioned { coords -> positionInRoot = coords.positionInRoot() }
            .shadow(
                elevation = if (hero) 20.dp else 12.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.28f),
                spotColor = Color.Black.copy(alpha = 0.28f)
            )
            .clip(shape)
    ) {
        // Layer 1 — the actual frosted-glass illusion: the same particles
        // drifting behind this card, redrawn at this card's own offset and
        // blurred. Falls back to nothing (just the tint below) if the
        // shared field hasn't been measured yet.
        if (field != null && field.fieldSize.width > 0f) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .blur(if (hero) 30.dp else 22.dp)
            ) {
                translate(left = -positionInRoot.x, top = -positionInRoot.y) {
                    drawParticleField(field.particles, field.time, field.fieldSize)
                }
            }
        }

        // Layer 2 — dark top-to-bottom tint (so text stays readable over
        // bright bokeh) plus a top-lit border for a sense of glass volume.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color(0x1FFFFFFF),
                            0.22f to Color(0x0AFFFFFF),
                            1f to Color(0xE30B0F15)
                        )
                    )
                )
                .border(
                    width = 1.1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            borderColor.copy(alpha = 0.55f),
                            borderColor.copy(alpha = 0.14f)
                        )
                    ),
                    shape = shape
                )
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
fun PhaseBadge(name: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(name, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

/** Header shown at the top of every tab: accent badge + weight range title,
 * phase badge, and a quick stat strip (week / streak / current weight) —
 * mirrors AppHeader from the Flutter app. */
@Composable
fun AppHeader(
    phaseName: String,
    phaseColor: Color,
    week: Int,
    totalWeeks: Int,
    streak: Int,
    currentWeight: Double,
    startWeight: Double,
    goalWeight: Double
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0x4D0C0F14), Color(0x2E0C0F14))
                )
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.linearGradient(listOf(AccentTeal, AccentTeal.copy(alpha = 0.55f)))),
                contentAlignment = Alignment.Center
            ) {
                Text("🚴", fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("PROGRAM PENURUNAN BERAT", color = TextDim, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${startWeight.toInt()} → ${goalWeight.toInt()} KG",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.width(8.dp))
                    PhaseBadge(phaseName, phaseColor)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatItem("MINGGU", "$week/$totalWeeks")
            StatItem("STREAK", "$streak hari")
            StatItem("BERAT", "%.1f kg".format(currentWeight))
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextDim, fontSize = 9.sp, letterSpacing = 0.5.sp)
    }
}

/** Circular progress ring with animated sweep + centered text, ported from
 * progress_ring.dart's CustomPainter. */
@Composable
fun ProgressRing(
    fraction: Float,
    color: Color,
    centerText: String,
    centerLabel: String,
    size: androidx.compose.ui.unit.Dp = 132.dp
) {
    val animated = remember { Animatable(0f) }
    LaunchedEffect(fraction) {
        animated.animateTo(fraction.coerceIn(0f, 1f), animationSpec = tween(900))
    }
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 12.dp.toPx()
            val diameter = kotlin.math.min(this.size.width, this.size.height) - strokeWidth
            val topLeft = androidx.compose.ui.geometry.Offset(
                (this.size.width - diameter) / 2f,
                (this.size.height - diameter) / 2f
            )
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = Color.White.copy(alpha = 0.08f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animated.value,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerText, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(centerLabel, color = TextDim, fontSize = 9.5.sp, letterSpacing = 0.5.sp)
        }
    }
}
