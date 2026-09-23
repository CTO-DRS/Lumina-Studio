package com.lumina.studio.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * High-performance Kotlin Image Processing Engine.
 * Provides deterministic image processing algorithms for CameraX photos:
 * - Grayscale conversion (Luma formula Y = 0.299R + 0.587G + 0.114B)
 * - Sepia tone tinting (Standard Photographic Sepia matrix)
 * - Brightness adjustment (Offset/Scale in normalized ranges)
 *
 * Designed with zero AI dependency, pure Kotlin & Android graphics hardware acceleration.
 */
object ImageProcessingEngine {

  /**
   * Applies grayscale transformation to a bitmap.
   */
  fun applyGrayscale(source: Bitmap): Bitmap {
    val result = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val colorMatrix = ColorMatrix().apply {
      setSaturation(0f)
    }
    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
    canvas.drawBitmap(source, 0f, 0f, paint)
    return result
  }

  /**
   * Applies photographic Sepia tone to a bitmap.
   */
  fun applySepia(source: Bitmap): Bitmap {
    val result = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Standard Photographic Sepia color transformation matrix
    val sepiaMatrix = ColorMatrix(
      floatArrayOf(
        0.393f, 0.769f, 0.189f, 0f, 0f,
        0.349f, 0.686f, 0.168f, 0f, 0f,
        0.272f, 0.534f, 0.131f, 0f, 0f,
        0f,     0f,     0f,     1f, 0f
      )
    )
    paint.colorFilter = ColorMatrixColorFilter(sepiaMatrix)
    canvas.drawBitmap(source, 0f, 0f, paint)
    return result
  }

  /**
   * Applies brightness adjustment to a bitmap.
   * @param brightness value between -100f (darkest) and +100f (brightest). 0f means unchanged.
   */
  fun applyBrightness(source: Bitmap, brightness: Float): Bitmap {
    val result = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val brightnessMatrix = ColorMatrix(
      floatArrayOf(
        1f, 0f, 0f, 0f, brightness,
        0f, 1f, 0f, 0f, brightness,
        0f, 0f, 1f, 0f, brightness,
        0f, 0f, 0f, 1f, 0f
      )
    )
    paint.colorFilter = ColorMatrixColorFilter(brightnessMatrix)
    canvas.drawBitmap(source, 0f, 0f, paint)
    return result
  }

  /**
   * Applies combined adjustments (Grayscale, Sepia, Brightness) in a single unified GPU pass.
   */
  fun processBitmap(
    source: Bitmap,
    isGrayscale: Boolean = false,
    isSepia: Boolean = false,
    brightness: Float = 0f
  ): Bitmap {
    if (!isGrayscale && !isSepia && brightness == 0f) {
      return source
    }

    val result = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val unifiedMatrix = ColorMatrix()

    if (isGrayscale) {
      val grayMatrix = ColorMatrix().apply { setSaturation(0f) }
      unifiedMatrix.postConcat(grayMatrix)
    }

    if (isSepia) {
      val sepiaMatrix = ColorMatrix(
        floatArrayOf(
          0.393f, 0.769f, 0.189f, 0f, 0f,
          0.349f, 0.686f, 0.168f, 0f, 0f,
          0.272f, 0.534f, 0.131f, 0f, 0f,
          0f,     0f,     0f,     1f, 0f
        )
      )
      unifiedMatrix.postConcat(sepiaMatrix)
    }

    if (brightness != 0f) {
      val brightMatrix = ColorMatrix(
        floatArrayOf(
          1f, 0f, 0f, 0f, brightness,
          0f, 1f, 0f, 0f, brightness,
          0f, 0f, 1f, 0f, brightness,
          0f, 0f, 0f, 1f, 0f
        )
      )
      unifiedMatrix.postConcat(brightMatrix)
    }

    paint.colorFilter = ColorMatrixColorFilter(unifiedMatrix)
    canvas.drawBitmap(source, 0f, 0f, paint)
    return result
  }

  /**
   * Pure Kotlin Direct Pixel-by-Pixel processing implementation.
   * Useful for algorithmic transformations or when direct byte buffer access is required.
   */
  fun processPixelsDirectly(
    source: Bitmap,
    isGrayscale: Boolean,
    isSepia: Boolean,
    brightness: Float
  ): Bitmap {
    val width = source.width
    val height = source.height
    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)

    val brightnessOffset = brightness.toInt()

    for (i in pixels.indices) {
      val pixel = pixels[i]
      val a = Color.alpha(pixel)
      var r = Color.red(pixel)
      var g = Color.green(pixel)
      var b = Color.blue(pixel)

      // 1. Grayscale pass
      if (isGrayscale) {
        val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
        r = gray
        g = gray
        b = gray
      }

      // 2. Sepia pass
      if (isSepia) {
        val newR = (0.393 * r + 0.769 * g + 0.189 * b).toInt()
        val newG = (0.349 * r + 0.686 * g + 0.168 * b).toInt()
        val newB = (0.272 * r + 0.534 * g + 0.131 * b).toInt()
        r = min(255, newR)
        g = min(255, newG)
        b = min(255, newB)
      }

      // 3. Brightness adjustment
      if (brightnessOffset != 0) {
        r = min(255, max(0, r + brightnessOffset))
        g = min(255, max(0, g + brightnessOffset))
        b = min(255, max(0, b + brightnessOffset))
      }

      pixels[i] = Color.argb(a, r, g, b)
    }

    val resultBitmap = Bitmap.createBitmap(width, height, source.config ?: Bitmap.Config.ARGB_8888)
    resultBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return resultBitmap
  }

  /**
   * Asynchronously processes an image file captured by CameraX and writes the processed output.
   */
  suspend fun processCapturedImageFile(
    inputFile: File,
    outputFile: File,
    isGrayscale: Boolean,
    isSepia: Boolean,
    brightness: Float,
    quality: Int = 95
  ): File = withContext(Dispatchers.IO) {
    if (!inputFile.exists()) return@withContext inputFile
    if (!isGrayscale && !isSepia && brightness == 0f) {
      return@withContext inputFile
    }

    val bitmap = BitmapFactory.decodeFile(inputFile.absolutePath) ?: return@withContext inputFile
    val processed = processBitmap(bitmap, isGrayscale, isSepia, brightness)

    FileOutputStream(outputFile).use { out ->
      processed.compress(Bitmap.CompressFormat.JPEG, quality, out)
      out.flush()
    }
    outputFile
  }
}
