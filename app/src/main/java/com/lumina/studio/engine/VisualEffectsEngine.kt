package com.lumina.studio.engine

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import com.lumina.studio.data.model.AdjustmentsState
import kotlin.random.Random

/**
 * Real-time Canvas post-processing layers.
 *
 * The stylistic effects (vignette, bloom, glitch, film grain) ARE the feature
 * itself, so their procedural drawing is real. The two ANALYSIS overlays are
 * now driven by REAL measured masks:
 *
 *  - Focus peaking draws the actual Sobel edge mask computed from the frame
 *    pixels by [MediaAnalysisEngine.computeFrameMasks] — no random dots.
 *  - Zebra stripes draw over pixels whose measured luma really exceeds 95 IRE
 *    — no fixed "top 35% of the screen" guess.
 */
object VisualEffectsEngine {

  fun renderCanvasEffects(
    scope: DrawScope,
    state: AdjustmentsState,
    seedOffset: Long = 0L,
    edgeOverlay: Bitmap? = null,
    zebraOverlay: Bitmap? = null
  ) {
    val canvasWidth = scope.size.width
    val canvasHeight = scope.size.height
    if (canvasWidth <= 0f || canvasHeight <= 0f) return

    // 1. Vignette (dark cinematic edge falloff)
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

    // 2. Bloom & ambient glow
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

    // 3. Glitch & chromatic aberration (the artistic effect itself)
    if (state.glitchRgb > 0f) {
      val glitchFactor = state.glitchRgb / 100f
      val offsetDist = glitchFactor * 14f

      scope.drawRect(
        color = Color(0xFF00F0FF).copy(alpha = 0.18f * glitchFactor),
        topLeft = Offset(offsetDist, 0f),
        size = scope.size
      )
      scope.drawRect(
        color = Color(0xFFFF0055).copy(alpha = 0.18f * glitchFactor),
        topLeft = Offset(-offsetDist, 0f),
        size = scope.size
      )

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

    // 4. 35mm analog film grain (the artistic effect itself)
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

    // 5. REAL focus peaking: draws the actual Sobel edge mask pixels
    if (state.focusPeaking) {
      drawRealMaskOverlay(scope, edgeOverlay, canvasWidth, canvasHeight)
    }

    // 6. REAL zebra: stripes only over measured ≥95-IRE pixels
    if (state.zebraStripes) {
      drawRealMaskOverlay(scope, zebraOverlay, canvasWidth, canvasHeight)
    }
  }

  /**
   * Draws a real analysis mask (small ARGB bitmap) scaled to fill the canvas
   * with nearest-neighbour feel preserved by FilterQuality.Low.
   */
  private fun drawRealMaskOverlay(
    scope: DrawScope,
    overlay: Bitmap?,
    canvasWidth: Float,
    canvasHeight: Float
  ) {
    if (overlay == null || overlay.width <= 0 || overlay.height <= 0) return
    val image = overlay.asImageBitmap()
    withTransform({
      scale(
        scaleX = canvasWidth / overlay.width,
        scaleY = canvasHeight / overlay.height,
        pivot = Offset.Zero
      )
    }) {
      drawImage(
        image = image,
        srcOffset = androidx.compose.ui.unit.IntOffset.Zero,
        srcSize = androidx.compose.ui.geometry.IntSize(overlay.width, overlay.height),
        dstOffset = androidx.compose.ui.unit.IntOffset.Zero,
        dstSize = androidx.compose.ui.geometry.IntSize(overlay.width, overlay.height),
        filterQuality = androidx.compose.ui.graphics.FilterQuality.Low
      )
    }
  }
}
