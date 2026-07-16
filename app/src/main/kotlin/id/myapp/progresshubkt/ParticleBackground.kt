package id.myapp.progresshubkt

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.random.Random

internal data class Particle(
    val xFrac: Float,      // 0..1 horizontal position
    val startYFrac: Float, // 0..1 initial vertical position
    val radius: Float,     // px
    val speed: Float,      // full screen heights per loop
    val color: Color,
    val swayAmp: Float,    // px, horizontal sway
    val swaySeed: Float
)

private val particlePalette = listOf(
    AccentTeal, Color(0xFFE0A94E), Color(0xFFD16B8A), Color(0xFF8A7FD1)
)

internal fun generateParticles(count: Int, seed: Int = 42): List<Particle> {
    val rnd = Random(seed)
    return List(count) {
        Particle(
            xFrac = rnd.nextFloat(),
            startYFrac = rnd.nextFloat(),
            radius = rnd.nextFloat() * 10f + 4f,
            speed = rnd.nextFloat() * 0.6f + 0.25f,
            color = particlePalette[rnd.nextInt(particlePalette.size)],
            swayAmp = rnd.nextFloat() * 24f + 6f,
            swaySeed = rnd.nextFloat() * 1000f
        )
    }
}

/** The single source of truth for "what the particle field looks like right
 * now" — one shared particle list, one shared animated clock, and the pixel
 * size of the full-screen field they're laid out against. Hoisted once at
 * the app root and handed down via [LocalParticleField] so that every
 * [GlassCard] can redraw the exact same particles (translated to its own
 * position) and blur that redraw — a real backdrop-blur illusion built from
 * a field we fully control, without needing a layer-capture library. */
internal data class ParticleFieldState(
    val particles: List<Particle>,
    val time: Float,
    val fieldSize: Size
)

internal val LocalParticleField = compositionLocalOf<ParticleFieldState?> { null }

/** Drives the shared particle animation clock. Call once near the app root
 * (not per-card) so every consumer of [LocalParticleField] stays in sync. */
@Composable
internal fun rememberParticleTime(durationMillis: Int = 40000): Float {
    val transition = rememberInfiniteTransition(label = "particles")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )
    return time
}

/** Draws [particles] positioned against a field of size [fieldSize] into
 * whatever [DrawScope] is current — used both by the full-screen backdrop
 * and by each [GlassCard]'s local blurred echo of the same field. */
internal fun DrawScope.drawParticleField(particles: List<Particle>, time: Float, fieldSize: Size) {
    val w = fieldSize.width
    val h = fieldSize.height
    if (w <= 0f || h <= 0f) return
    for (p in particles) {
        // Vertical position loops from bottom to top, wrapping using the
        // particle's own speed so they don't all move in lockstep.
        val rawProgress = p.startYFrac + time * p.speed
        val progress = rawProgress - kotlin.math.floor(rawProgress)
        val y = h * (1f - progress)
        val sway = kotlin.math.sin((time * 6.283f * 1.3f) + p.swaySeed) * p.swayAmp
        val x = p.xFrac * w + sway
        // Fade in/out near the top and bottom edges so pop-in/out isn't abrupt.
        val edgeFade = kotlin.math.min(progress / 0.08f, (1f - progress) / 0.08f).coerceIn(0f, 1f)
        drawCircle(
            color = p.color.copy(alpha = 0.30f * edgeFade),
            radius = p.radius * 2.2f,
            center = Offset(x, y)
        )
        drawCircle(
            color = p.color.copy(alpha = 0.65f * edgeFade),
            radius = p.radius * 0.7f,
            center = Offset(x, y)
        )
    }
}

/** Full-screen layer of slow drifting bokeh dots. When [particles]/[time]
 * aren't supplied, it generates and animates its own (handy for previews or
 * standalone use) — but the app root supplies its own shared field via
 * [LocalParticleField] so [GlassCard] panels can echo it. */
@Composable
fun ParticleBackground(
    modifier: Modifier = Modifier,
    count: Int = 46,
    particles: List<Particle> = remember { generateParticles(count) },
    time: Float = rememberParticleTime()
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawParticleField(particles, time, size)
    }
}
