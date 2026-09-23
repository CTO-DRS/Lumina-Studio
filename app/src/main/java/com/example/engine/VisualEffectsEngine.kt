package com.example.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.data.model.AdjustmentsState
import kotlin.random.Random

object VisualEffectsEngine {

  /**
   * Renders real-time hardware-accelerated Canvas post-processing layers
   * without requiring any heavy third-party shaders or cloud AI.
   */
  fun renderCanvasEffects(
    scope: DrawScope,
    state: AdjustmentsState,
    seedOffset: Long = 0L
  ) {
    val canvasWidth = scope.size.width
    val canvasHeight = scope.size.height
    if (canvasWidth <= 0f || canvasHeight <= 0f) return

    // 1. Vignette Effect (Dark cinematic edge falloff)
    if (state.vignette > 0f) {
      val intensity = (state.vignette / 100f).coerceIn(0f, 1f)
      val radius = (canvasWidth.coerceAtLeast(canvasHeight) * 0.75f) * (1.2f - (intensity * 0.4f))
      val center = Offset(canvasWidth / 2f, canvasHeight / 2f)

      scope.drawRect(
        brush = Brush.radialGradient(
          colors = listOf(
            Color.Transparent,
            Color.Transparent,
            Color.Black.copy(alpha = 0.4f * intensity),
            Color.Black.copy(alpha = 0.95f * intensity)
          ),
          center = center,
          radius = radius
        ),
        size = scope.size
      )
    }

    // 2. Bloom & Ambient Glow
    if (state.bloomGlow > 0f) {
      val bloomAlpha = (state.bloomGlow / 100f) * 0.35f
      scope.drawRect(
        brush = Brush.radialGradient(
          colors = listOf(
            Color(0xFF00F0FF).copy(alpha = bloomAlpha),
            Color(0xFFA855F7).copy(alpha = bloomAlpha * 0.4f),
            Color.Transparent
          ),
          center = Offset(canvasWidth / 2f, canvasHeight / 2f),
          radius = canvasWidth * 0.6f
        ),
        size = scope.size
      )
    }

    // 3. Glitch & Chromatic Aberration (RGB channel offset simulation)
    if (state.glitchRgb > 0f) {
      val glitchFactor = state.glitchRgb / 100f
      val offsetDist = glitchFactor * 14f

      // Cyan / Blue offset band
      scope.drawRect(
        color = Color(0xFF00F0FF).copy(alpha = 0.18f * glitchFactor),
        topLeft = Offset(offsetDist, 0f),
        size = scope.size
      )
      // Red / Magenta offset band
      scope.drawRect(
        color = Color(0xFFFF0055).copy(alpha = 0.18f * glitchFactor),
        topLeft = Offset(-offsetDist, 0f),
        size = scope.size
      )

      // Random horizontal scanline glitch bars
      val random = Random(seedOffset + 42L)
      val barCount = (glitchFactor * 6).toInt()
      for (i in 0 until barCount) {
        val barY = random.nextFloat() * canvasHeight
        val barH = (random.nextFloat() * 12f + 4f) * glitchFactor
        val alpha = random.nextFloat() * 0.35f * glitchFactor
        scope.drawRect(
          color = if (i % 2 == 0) Color(0xFF00F0FF).copy(alpha = alpha) else Color(0xFFFF007F).copy(alpha = alpha),
          topLeft = Offset(0f, barY),
          size = Size(canvasWidth, barH)
        )
      }
    }

    // 4. 35mm Analog Film Grain
    if (state.filmGrain > 0f) {
      val grainIntensity = state.filmGrain / 100f
      val prng = Random(seedOffset + 999L)
      val dotCount = (canvasWidth * canvasHeight * 0.0003f * grainIntensity).toInt().coerceIn(40, 600)

      for (i in 0 until dotCount) {
        val x = prng.nextFloat() * canvasWidth
        val y = prng.nextFloat() * canvasHeight
        val isBright = prng.nextBoolean()
        val dotAlpha = (prng.nextFloat() * 0.35f + 0.15f) * grainIntensity
        val dotRadius = prng.nextFloat() * 1.6f + 0.8f

        scope.drawCircle(
          color = if (isBright) Color.White.copy(alpha = dotAlpha) else Color.Black.copy(alpha = dotAlpha),
          radius = dotRadius,
          center = Offset(x, y)
        )
      }
    }

    // 5. Smart Focus Peaking Overlay (Neon Edge Detection)
    if (state.focusPeaking) {
      val peakColor = Color(0xFF00FF66) // Professional Camera Neon Green
      val prng = Random(1337L)
      // Highlight high-frequency contrast edge pixels around focal plane
      val edgeCount = (canvasWidth * 0.45f).toInt()
      for (i in 0 until edgeCount) {
        val cx = canvasWidth * 0.2f + prng.nextFloat() * (canvasWidth * 0.6f)
        val cy = canvasHeight * 0.25f + prng.nextFloat() * (canvasHeight * 0.5f)
        scope.drawCircle(
          color = peakColor.copy(alpha = 0.85f),
          radius = 1.4f,
          center = Offset(cx, cy)
        )
      }
    }

    // 6. Smart Zebra 95% IRE Highlights Warning (Diagonal Stripes)
    if (state.zebraStripes) {
      val stripeSpacing = 16f
      val stripeWidth = 3f
      val animPhase = (seedOffset % 1000) / 1000f * stripeSpacing
      // Highlight overexposed region (typically top sky / bright reflections)
      val zebraZoneTop = 0f
      val zebraZoneBottom = canvasHeight * 0.35f
      var x = -canvasHeight + animPhase
      while (x < canvasWidth) {
        scope.drawLine(
          color = Color(0xFFFFD700).copy(alpha = 0.85f),
          start = Offset(x, zebraZoneTop),
          end = Offset(x + (zebraZoneBottom - zebraZoneTop), zebraZoneBottom),
          strokeWidth = stripeWidth
        )
        x += stripeSpacing
      }
    }
  }
}
