package com.lumina.studio.engine

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Real pixel-level image analysis engine.
 *
 * Every number produced here is measured from the ACTUAL pixels of the frame
 * currently loaded in the editor — no slider echo, no simulation:
 *
 *  - [analyzeColorBalance] : true per-channel spatial means (basis of Gray World AWB)
 *  - [analyzeTone]         : true luma histogram percentiles (basis of Auto-Levels)
 *  - [analyzeSharpness]    : Laplacian variance over a grayscale copy
 *  - [computeHistogram]    : real 64-bin R/G/B histograms for the scopes UI
 *  - [computeEdgeMask]     : Sobel gradient magnitude mask (real focus peaking)
 *  - [computeOverexposureMask] : luma > 95% pixel mask (real zebra warning)
 */
object MediaAnalysisEngine {

  // ---------------------------------------------------------------------
  // Result types
  // ---------------------------------------------------------------------

  data class ColorBalanceStats(
    val meanR: Float,
    val meanG: Float,
    val meanB: Float,
    val meanLuma: Float
  )

  data class ToneStats(
    val p1: Int,
    val p50: Int,
    val p99: Int,
    val meanLuma: Int,
    val clippedHighlightsRatio: Float,
    val crushedShadowsRatio: Float
  )

  data class SharpnessStats(
    val laplacianVariance: Float,
    /** 0..1 normalized acuity; ~0 soft/blurry, ~1 extremely sharp */
    val normalizedAcuity: Float
  )

  data class HistogramData(
    val red: IntArray,
    val green: IntArray,
    val blue: IntArray,
    val luma: IntArray,
    val binCount: Int,
    val pixelCount: Int
  )

  data class FrameMasks(
    /** Per-pixel Sobel edge mask at the analysis resolution (true = strong edge). */
    val edgeMask: BooleanArray,
    val maskWidth: Int,
    val maskHeight: Int,
    /** Per-pixel over-exposure mask (luma >= 243/255 ≈ 95 IRE). */
    val overexposureMask: BooleanArray,
    val overexposureRatio: Float
  )

  // ---------------------------------------------------------------------
  // Downscale helper — analysis always runs on a small real copy of the frame
  // ---------------------------------------------------------------------

  fun downscaleForAnalysis(source: Bitmap, targetShortSide: Int = 320): Bitmap {
    val shortSide = min(source.width, source.height)
    if (shortSide <= targetShortSide) return source
    val scale = targetShortSide.toFloat() / shortSide
    val w = max(1, (source.width * scale).toInt())
    val h = max(1, (source.height * scale).toInt())
    return Bitmap.createScaledBitmap(source, w, h, true)
  }

  private fun extractPixels(source: Bitmap): Triple<IntArray, Int, Int> {
    val w = source.width
    val h = source.height
    val pixels = IntArray(w * h)
    source.getPixels(pixels, 0, w, 0, 0, w, h)
    return Triple(pixels, w, h)
  }

  // ---------------------------------------------------------------------
  // 1. Real Gray-World color balance
  // ---------------------------------------------------------------------

  /**
   * Measures the true spatial means of the R/G/B channels. The Gray World
   * assumption states that, in a well-exposed scene, the average reflectance
   * is achromatic; deviation of the measured means from equal-energy gray
   * quantifies the color cast.
   */
  fun analyzeColorBalance(source: Bitmap): ColorBalanceStats {
    val (pixels, _, _) = extractPixels(downscaleForAnalysis(source))
    var sumR = 0.0
    var sumG = 0.0
    var sumB = 0.0
    var count = 0
    for (p in pixels) {
      sumR += Color.red(p)
      sumG += Color.green(p)
      sumB += Color.blue(p)
      count++
    }
    if (count == 0) return ColorBalanceStats(128f, 128f, 128f, 128f)
    val meanR = (sumR / count).toFloat()
    val meanG = (sumG / count).toFloat()
    val meanB = (sumB / count).toFloat()
    val meanLuma = (0.299f * meanR + 0.587f * meanG + 0.114f * meanB)
    return ColorBalanceStats(meanR, meanG, meanB, meanLuma)
  }

  // ---------------------------------------------------------------------
  // 2. Real tone / histogram percentiles
  // ---------------------------------------------------------------------

  /**
   * Builds a 256-bin luma histogram (Rec.601 luma on real pixels) and derives
   * the statistical percentiles used by Auto-Levels tone mapping.
   */
  fun analyzeTone(source: Bitmap): ToneStats {
    val (pixels, _, _) = extractPixels(downscaleForAnalysis(source))
    val hist = IntArray(256)
    var count = 0
    for (p in pixels) {
      val luma = (0.299f * Color.red(p) + 0.587f * Color.green(p) + 0.114f * Color.blue(p)).toInt()
      hist[luma.coerceIn(0, 255)]++
      count++
    }
    if (count == 0) {
      return ToneStats(0, 128, 255, 128, 0f, 0f)
    }

    fun percentile(fraction: Float): Int {
      val target = (fraction * count).toInt().coerceIn(1, count)
      var acc = 0
      for (i in 0 until 256) {
        acc += hist[i]
        if (acc >= target) return i
      }
      return 255
    }

    var sum = 0.0
    for (i in 0 until 256) sum += i.toDouble() * hist[i]
    val mean = (sum / count).toInt()

    val clipped = (hist[255].toDouble() / count).toFloat()
    val crushed = (hist[0].toDouble() / count).toFloat()

    return ToneStats(
      p1 = percentile(0.01f),
      p50 = percentile(0.50f),
      p99 = percentile(0.99f),
      meanLuma = mean,
      clippedHighlightsRatio = clipped,
      crushedShadowsRatio = crushed
    )
  }

  // ---------------------------------------------------------------------
  // 3. Real Laplacian variance sharpness estimation
  // ---------------------------------------------------------------------

  /**
   * Computes the variance of the 4-neighbor Laplacian on a grayscale copy.
   * A well-focused frame has high edge energy → large variance; a blurry one
   * approaches zero. This is the standard reference-free blur metric.
   */
  fun analyzeSharpness(source: Bitmap): SharpnessStats {
    val small = downscaleForAnalysis(source, 256)
    val (pixels, w, h) = extractPixels(small)
    if (w < 3 || h < 3) return SharpnessStats(0f, 0f)

    val gray = IntArray(w * h)
    for (i in pixels.indices) {
      gray[i] = (0.299f * Color.red(pixels[i]) + 0.587f * Color.green(pixels[i]) + 0.114f * Color.blue(pixels[i])).toInt()
    }

    var sum = 0.0
    var sumSq = 0.0
    var n = 0
    for (y in 1 until h - 1) {
      val row = y * w
      for (x in 1 until w - 1) {
        val i = row + x
        val lap = gray[i - 1] + gray[i + 1] + gray[i - w] + gray[i + w] - 4 * gray[i]
        sum += lap
        sumSq += lap.toDouble() * lap
        n++
      }
    }
    if (n == 0) return SharpnessStats(0f, 0f)
    val mean = sum / n
    val variance = (sumSq / n - mean * mean).toFloat().coerceAtLeast(0f)

    // Empirical normalization: variance saturates around ~4000 on crisp 256px edges
    val acuity = (sqrt(variance) / 63f).coerceIn(0f, 1f)
    return SharpnessStats(variance, acuity)
  }

  // ---------------------------------------------------------------------
  // 4. Real RGB histograms for scopes UI
  // ---------------------------------------------------------------------

  fun computeHistogram(source: Bitmap, binCount: Int = 64): HistogramData {
    val (pixels, _, _) = extractPixels(downscaleForAnalysis(source, 256))
    val binSize = 256 / binCount
    val red = IntArray(binCount)
    val green = IntArray(binCount)
    val blue = IntArray(binCount)
    val luma = IntArray(binCount)
    for (p in pixels) {
      val r = Color.red(p)
      val g = Color.green(p)
      val b = Color.blue(p)
      red[min(binCount - 1, r / binSize)]++
      green[min(binCount - 1, g / binSize)]++
      blue[min(binCount - 1, b / binSize)]++
      val y = (0.299f * r + 0.587f * g + 0.114f * b).toInt().coerceIn(0, 255)
      luma[min(binCount - 1, y / binSize)]++
    }
    return HistogramData(red, green, blue, luma, binCount, pixels.size)
  }

  // ---------------------------------------------------------------------
  // 5. Real Sobel edge mask (focus peaking) + overexposure mask (zebra)
  // ---------------------------------------------------------------------

  /**
   * Computes both real analysis masks in one pass over the pixels:
   *  - Sobel gradient magnitude ≥ [edgeThreshold] → edge (focus peaking overlay)
   *  - luma ≥ 243 (95 IRE) → overexposed (zebra stripes overlay)
   */
  fun computeFrameMasks(source: Bitmap, edgeThreshold: Int = 48): FrameMasks {
    val small = downscaleForAnalysis(source, 256)
    val (pixels, w, h) = extractPixels(small)
    if (w < 3 || h < 3) {
      return FrameMasks(
        BooleanArray(w * h), w, h,
        BooleanArray(w * h), 0f
      )
    }

    val gray = IntArray(w * h)
    var overexposed = 0
    for (i in pixels.indices) {
      val r = Color.red(pixels[i])
      val g = Color.green(pixels[i])
      val b = Color.blue(pixels[i])
      val y = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
      gray[i] = y
      if (y >= 243) overexposed++
    }

    val edgeMask = BooleanArray(w * h)
    val overMask = BooleanArray(w * h)
    var edgeCount = 0
    for (y in 1 until h - 1) {
      val row = y * w
      for (x in 1 until w - 1) {
        val i = row + x
        val tl = gray[i - w - 1]; val t = gray[i - w]; val tr = gray[i - w + 1]
        val l = gray[i - 1];                          val r = gray[i + 1]
        val bl = gray[i + w - 1]; val bo = gray[i + w]; val br = gray[i + w + 1]

        val gx = (tl + 2 * l + bl) - (tr + 2 * r + br)
        val gy = (tl + 2 * t + tr) - (bl + 2 * bo + br)
        val mag = sqrt((gx * gx + gy * gy).toDouble()).toInt()

        if (mag >= edgeThreshold) {
          edgeMask[i] = true
          edgeCount++
        }
        if (gray[i] >= 243) overMask[i] = true
      }
    }

    return FrameMasks(
      edgeMask = edgeMask,
      maskWidth = w,
      maskHeight = h,
      overexposureMask = overMask,
      overexposureRatio = overexposed.toFloat() / (w * h).toFloat()
    )
  }

  /**
   * Converts a boolean mask into an ARGB overlay bitmap (highlight color where
   * the mask is true) so the UI can draw REAL detected regions, not random dots.
   */
  fun maskToOverlayBitmap(mask: BooleanArray, width: Int, height: Int, color: Int): Bitmap? {
    if (width <= 0 || height <= 0 || mask.size < width * height) return null
    val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(width * height)
    for (i in 0 until width * height) {
      pixels[i] = if (mask[i]) color else Color.TRANSPARENT
    }
    out.setPixels(pixels, 0, width, 0, 0, width, height)
    return out
  }

  /** True channel-imbalance gain needed to neutralize a cast, for AWB math. */
  fun grayWorldGains(stats: ColorBalanceStats): Triple<Float, Float, Float> {
    val avg = max(1f, (stats.meanR + stats.meanG + stats.meanB) / 3f)
    val gainR = avg / max(1f, stats.meanR)
    val gainG = avg / max(1f, stats.meanG)
    val gainB = avg / max(1f, stats.meanB)
    return Triple(gainR, gainG, gainB)
  }

  /** Useful debug string describing the real measured cast. */
  fun describeCast(stats: ColorBalanceStats): String {
    val rb = stats.meanR - stats.meanB
    val gm = stats.meanG - (stats.meanR + stats.meanB) / 2f
    val rbDesc = when {
      rb > 18f -> "صبغة دافئة (احمرار +${abs(rb).toInt()})"
      rb < -18f -> "صبغة باردة (ازرقاق ${abs(rb).toInt()})"
      else -> "توازن محايد"
    }
    val gDesc = when {
      gm > 15f -> "انزياح أخضر"
      gm < -15f -> "انزياح ماجنتا"
      else -> "أخضر-ماجنتا متوازن"
    }
    return "$rbDesc • $gDesc"
  }
}
