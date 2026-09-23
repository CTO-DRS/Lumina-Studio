package com.lumina.studio.engine.color

import androidx.compose.ui.graphics.ColorMatrix
import com.lumina.studio.data.model.AdjustmentsState
import com.lumina.studio.data.model.FilterPreset
import kotlin.math.cos
import kotlin.math.sin

object ColorMatrixEngine {

  /**
   * Generates a single unified 4x5 ColorMatrix combining all manual adjustments
   * and selected color grading LUT/presets through direct matrix mathematics.
   * Completely deterministic, fast, hardware-accelerated, zero AI overhead.
   */
  fun createUnifiedMatrix(state: AdjustmentsState): ColorMatrix {
    val matrix = ColorMatrix()

    // 1. Auto-Enhance factor (if beginner auto-enhance slider is applied)
    val autoEnhanceFactor = state.autoEnhance / 100f
    val effectiveContrast = state.contrast + (autoEnhanceFactor * 18f)
    val effectiveSaturation = state.saturation + (autoEnhanceFactor * 22f)
    val effectiveBrightness = state.brightness + (autoEnhanceFactor * 8f)
    val effectiveExposure = state.exposure + (autoEnhanceFactor * 10f)

    // 2. Base Brightness & Exposure
    val brightnessOffset = (effectiveBrightness * 1.5f) + (effectiveExposure * 1.8f)

    // 3. Contrast multiplier
    // Standard formula: c = (contrast + 100) / 100. Range: [0..2]
    val c = ((effectiveContrast + 100f) / 100f).coerceIn(0.1f, 3.0f)
    val cOffset = 128f * (1f - c)

    // 4. Saturation components
    val s = ((effectiveSaturation + 100f) / 100f).coerceIn(0f, 3.0f)
    val lr = 0.2126f
    val lg = 0.7152f
    val lb = 0.0722f
    val invS = 1f - s

    // 5. Temperature & Tint
    val tempNorm = state.temperature / 100f // -1..1
    val rTemp = 1f + (tempNorm * 0.28f)
    val bTemp = 1f - (tempNorm * 0.28f)

    val tintNorm = state.tint / 100f // -1..1
    val gTint = 1f - (tintNorm * 0.25f)
    val mTint = 1f + (tintNorm * 0.15f) // shifts red and blue for magenta

    // Compose base adjustments into array
    var r1 = (invS * lr + s) * c * rTemp * mTint
    var g1 = (invS * lg) * c * gTint
    var b1 = (invS * lb) * c * bTemp * mTint

    var r2 = (invS * lr) * c * rTemp * mTint
    var g2 = (invS * lg + s) * c * gTint
    var b2 = (invS * lb) * c * bTemp * mTint

    var r3 = (invS * lr) * c * rTemp * mTint
    var g3 = (invS * lg) * c * gTint
    var b3 = (invS * lb + s) * c * bTemp * mTint

    var offR = cOffset + brightnessOffset
    var offG = cOffset + brightnessOffset
    var offB = cOffset + brightnessOffset

    // 6. Apply Preset Grade Matrix Multipliers with LUT intensity blending
    val origR1 = r1; val origG1 = g1; val origB1 = b1; val origOffR = offR
    val origR2 = r2; val origG2 = g2; val origB2 = b2; val origOffG = offG
    val origR3 = r3; val origG3 = g3; val origB3 = b3; val origOffB = offB

    when (state.selectedPreset) {
      FilterPreset.ORIGINAL -> {
        // Unaltered raw baseline
      }
      FilterPreset.TEAL_ORANGE -> {
        // High-end Hollywood Blockbuster: warm orange highlights, deep cyan/teal shadows
        r1 *= 1.25f
        b1 *= 0.82f
        b3 *= 1.35f
        r3 *= 0.75f
        offR += 14f
        offB += 10f
      }
      FilterPreset.BLOCKBUSTER_COLD -> {
        // Nordic Cold Thriller: cool blue shadow tone, high tension contrast
        r1 *= 0.84f
        g2 *= 0.96f
        b3 *= 1.34f
        offB += 20f
        offR -= 10f
      }
      FilterPreset.BLEACH_BYPASS -> {
        // 500T Bleach Bypass: desaturated colors, harsh silver contrast
        val mono = (r1 * 0.299f + g2 * 0.587f + b3 * 0.114f)
        r1 = r1 * 0.55f + mono * 0.50f
        g2 = g2 * 0.55f + mono * 0.50f
        b3 = b3 * 0.55f + mono * 0.50f
        offR += 6f
        offG += 6f
        offB += 6f
      }
      FilterPreset.DESERT_HEAT -> {
        // Desert Heat 35mm: intense golden amber sunlit landscapes
        r1 *= 1.34f
        g2 *= 1.15f
        b3 *= 0.68f
        offR += 26f
        offG += 10f
        offB -= 18f
      }
      FilterPreset.CYBERPUNK -> {
        // Neo-Tokyo: saturated neon cyan, magenta highlights, ultra high contrast
        r1 *= 1.18f
        g2 *= 0.90f
        b3 *= 1.45f
        offR += 22f
        offB += 35f
        offG -= 15f
      }
      FilterPreset.FILM_NOIR -> {
        // Deep monochrome with rich silver midtones & crushed blacks
        val monoR = 0.299f * c
        val monoG = 0.587f * c
        val monoB = 0.114f * c
        r1 = monoR; g1 = monoG; b1 = monoB
        r2 = monoR; g2 = monoG; b2 = monoB
        r3 = monoR; g3 = monoG; b3 = monoB
        offR = cOffset - 10f
        offG = cOffset - 10f
        offB = cOffset - 10f
      }
      FilterPreset.SILVER_MONO -> {
        // Smooth continuous greyscale with rich soft studio midtones
        val monoSilver = (0.26f * r1 + 0.60f * g2 + 0.14f * b3)
        r1 = monoSilver; g1 = monoSilver; b1 = monoSilver
        r2 = monoSilver; g2 = monoSilver; b2 = monoSilver
        r3 = monoSilver; g3 = monoSilver; b3 = monoSilver
        offR = cOffset + 8f
        offG = cOffset + 8f
        offB = cOffset + 8f
      }
      FilterPreset.SEPIA_ANTIQUE -> {
        // Turn of the century antique warm sepia
        val monoSepia = (0.299f * r1 + 0.587f * g2 + 0.114f * b3)
        r1 = monoSepia * 1.18f; g1 = monoSepia * 0.95f; b1 = monoSepia * 0.72f
        r2 = monoSepia * 1.18f; g2 = monoSepia * 0.95f; b2 = monoSepia * 0.72f
        r3 = monoSepia * 1.18f; g3 = monoSepia * 0.95f; b3 = monoSepia * 0.72f
        offR = cOffset + 30f
        offG = cOffset + 18f
        offB = cOffset - 8f
      }
      FilterPreset.GOLDEN_HOUR -> {
        // Sunset warmth & soft golden glow
        r1 *= 1.35f
        g2 *= 1.12f
        b3 *= 0.70f
        offR += 28f
        offG += 12f
        offB -= 16f
      }
      FilterPreset.MIDNIGHT_BLUE -> {
        // Deep moonlight indigo wash with preserved shadow details
        r1 *= 0.70f
        g2 *= 0.82f
        b3 *= 1.40f
        offB += 24f
        offR -= 16f
        offG -= 10f
      }
      FilterPreset.AUTUMN_VIBE -> {
        // Warm golden leaves and cozy saturated terracotta earth tones
        r1 *= 1.26f
        g2 *= 1.06f
        b3 *= 0.74f
        offR += 24f
        offG += 8f
        offB -= 16f
      }
      FilterPreset.RETRO_70S -> {
        // Vintage 1970s analog roll: lifted black levels, creamy yellow-green bias
        r1 *= 1.08f
        g2 *= 1.15f
        b3 *= 0.88f
        offR += 18f
        offG += 18f
        offB += 32f // lifted blacks
      }
      FilterPreset.KODACHROME_64 -> {
        // Iconic punchy reds, intense blues, and documentary contrast
        r1 *= 1.28f
        g2 *= 1.04f
        b3 *= 1.12f
        offR += 14f
        offG -= 4f
        offB += 8f
      }
      FilterPreset.POLAROID_WARM -> {
        // Warm nostalgic hues with gentle dynamic range and soft highlights
        r1 *= 1.14f
        g2 *= 1.08f
        b3 *= 0.90f
        offR += 20f
        offG += 14f
        offB += 26f
      }
      FilterPreset.EMERALD -> {
        // Lush cinematic foliage and cold moody shadows
        g2 *= 1.32f
        r1 *= 0.85f
        b3 *= 1.10f
        offG += 16f
      }
      FilterPreset.INFRARED -> {
        // False-color Aerochrome thermal spectrum
        val tempR = r1
        r1 = b3 * 1.3f
        b3 = tempR * 0.9f
        offR += 40f
        offB += 20f
      }
      FilterPreset.MATRIX_GREEN -> {
        // Digital phosphorescent green wash inspired by sci-fi cyberpunk worlds
        r1 *= 0.55f
        g2 *= 1.48f
        b3 *= 0.65f
        offG += 30f
        offR -= 18f
        offB -= 18f
      }
      FilterPreset.VIVID_HDR -> {
        // Super-charged dynamic range
        r1 *= 1.22f
        g2 *= 1.22f
        b3 *= 1.22f
        offR += 12f
        offG += 12f
        offB += 12f
      }
      FilterPreset.CLEAN_COMMERCIAL -> {
        // Neutral balanced tones and pristine whites for crisp commercial aesthetics
        r1 *= 1.08f
        g2 *= 1.08f
        b3 *= 1.10f
        offR += 10f
        offG += 10f
        offB += 12f
      }
      FilterPreset.KODAK_PORTRA -> {
        // Legendary analog portrait film: smooth skin tones, warm golden highlights, pastel shadows
        r1 *= 1.15f
        g2 *= 1.05f
        b3 *= 0.95f
        offR += 14f
        offG += 6f
        offB -= 8f
      }
      FilterPreset.FUJI_CHROME -> {
        // Fujifilm Velvia: striking cobalt skies, ultra saturated emerald greens and deep blacks
        r1 *= 0.96f
        g2 *= 1.24f
        b3 *= 1.28f
        offR -= 6f
        offG += 10f
        offB += 16f
      }
      FilterPreset.PASTEL_DREAM -> {
        // Dreamy low-contrast anamorphic haze with soft peach and lavender highlights
        r1 *= 1.08f
        g2 *= 1.02f
        b3 *= 1.12f
        offR += 22f
        offG += 18f
        offB += 24f
      }
      FilterPreset.SUNSET_CORAL -> {
        // Vibrant glowing coral and peach highlights with energized warmth
        r1 *= 1.28f
        g2 *= 0.96f
        b3 *= 1.16f
        offR += 26f
        offG += 4f
        offB += 18f
      }
    }

    // Blend between pre-preset matrix and LUT preset based on lutIntensity (0% to 100%)
    val lutAlpha = (state.lutIntensity / 100f).coerceIn(0f, 1f)
    if (lutAlpha < 0.999f && state.selectedPreset != FilterPreset.ORIGINAL) {
      r1 = origR1 + (r1 - origR1) * lutAlpha
      g1 = origG1 + (g1 - origG1) * lutAlpha
      b1 = origB1 + (b1 - origB1) * lutAlpha
      offR = origOffR + (offR - origOffR) * lutAlpha

      r2 = origR2 + (r2 - origR2) * lutAlpha
      g2 = origG2 + (g2 - origG2) * lutAlpha
      b2 = origB2 + (b2 - origB2) * lutAlpha
      offG = origOffG + (offG - origOffG) * lutAlpha

      r3 = origR3 + (r3 - origR3) * lutAlpha
      g3 = origG3 + (g3 - origG3) * lutAlpha
      b3 = origB3 + (b3 - origB3) * lutAlpha
      offB = origOffB + (offB - origOffB) * lutAlpha
    }

    val finalValues = floatArrayOf(
      r1, g1, b1, 0f, offR,
      r2, g2, b2, 0f, offG,
      r3, g3, b3, 0f, offB,
      0f, 0f, 0f, 1f, 0f
    )

    return ColorMatrix(finalValues)
  }

  /**
   * Generates a ColorMatrix purely for a given filter preset and intensity
   */
  fun createFilterPresetMatrix(preset: FilterPreset, intensity: Float = 100f): ColorMatrix {
    return createUnifiedMatrix(AdjustmentsState(selectedPreset = preset, lutIntensity = intensity))
  }
}
