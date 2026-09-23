package com.lumina.studio.engine.media

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.FileProvider
import com.lumina.studio.data.model.AdjustmentsState
import com.lumina.studio.data.model.CropAspect
import com.lumina.studio.data.model.ProjectEntity
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import com.lumina.studio.engine.color.ColorMatrixEngine

data class VideoMetadata(
  val title: String,
  val durationMs: Long,
  val width: Int,
  val height: Int,
  val fps: Int,
  val sizeBytes: Long
)

data class PhotoMetadata(
  val title: String,
  val width: Int,
  val height: Int,
  val sizeBytes: Long
)

data class SaveResult(
  val isSuccess: Boolean,
  val mediaStoreUri: Uri?,
  val shareableUri: Uri?,
  val displayPath: String,
  val errorMessage: String? = null
)

object RealMediaManager {

  private const val TAG = "RealMediaManager"

  /**
   * Extracts real duration, resolution, fps and file name from an imported device video URI.
   */
  fun extractVideoMetadata(context: Context, uri: Uri): VideoMetadata {
    var title = "فيديو مستورد"
    var sizeBytes = 0L

    try {
      context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
          val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
          if (nameIndex != -1) {
            val name = cursor.getString(nameIndex)
            if (!name.isNullOrBlank()) title = name
          }
          if (sizeIndex != -1) {
            sizeBytes = cursor.getLong(sizeIndex)
          }
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Could not read openable columns: ${e.message}")
    }

    var durationMs = 30000L
    var width = 1920
    var height = 1080
    var fps = 30

    val retriever = MediaMetadataRetriever()
    try {
      retriever.setDataSource(context, uri)
      val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
      durStr?.toLongOrNull()?.let { if (it > 0) durationMs = it }

      val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
      widthStr?.toIntOrNull()?.let { if (it > 0) width = it }

      val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
      heightStr?.toIntOrNull()?.let { if (it > 0) height = it }

      val captureRateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
      captureRateStr?.toFloatOrNull()?.let { if (it > 0) fps = it.toInt() }
    } catch (e: Exception) {
      Log.w(TAG, "Retriever metadata extraction fallback: ${e.message}")
    } finally {
      try {
        retriever.release()
      } catch (_: Exception) {}
    }

    return VideoMetadata(
      title = title,
      durationMs = durationMs,
      width = width,
      height = height,
      fps = fps,
      sizeBytes = sizeBytes
    )
  }

  /**
   * Extracts real image dimensions and file name from an imported device photo URI.
   */
  fun extractPhotoMetadata(context: Context, uri: Uri): PhotoMetadata {
    var title = "صورة مستوردة"
    var sizeBytes = 0L

    try {
      context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
          val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
          if (nameIndex != -1) {
            val name = cursor.getString(nameIndex)
            if (!name.isNullOrBlank()) title = name
          }
          if (sizeIndex != -1) {
            sizeBytes = cursor.getLong(sizeIndex)
          }
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Could not query photo columns: ${e.message}")
    }

    var width = 1920
    var height = 1080

    try {
      val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, options)
        if (options.outWidth > 0 && options.outHeight > 0) {
          width = options.outWidth
          height = options.outHeight
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Could not decode photo bounds: ${e.message}")
    }

    return PhotoMetadata(
      title = title,
      width = width,
      height = height,
      sizeBytes = sizeBytes
    )
  }

  /**
   * Loads the base bitmap for a project from its REAL media file.
   * Returns null when the project has no media attached or the file cannot
   * be decoded — callers must show an honest empty state instead of a stock
   * photo (the old drawable fallback was removed as it displayed fake media).
   */
  fun loadProjectBitmapOrNull(context: Context, project: ProjectEntity?): Bitmap? {
    if (project == null || project.mediaUri.isNullOrBlank()) return null
    return try {
      val uri = Uri.parse(project.mediaUri)
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream)
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to load bitmap from mediaUri: ${e.message}")
      null
    }
  }

  /**
   * Loads a downscaled bitmap for a video at an arbitrary timestamp using the
   * real decoder (MediaMetadataRetriever) — used by the frame-accurate preview.
   */
  fun loadVideoFrameAt(context: Context, mediaUri: String, positionMs: Long): Bitmap? {
    if (mediaUri.isBlank()) return null
    var retriever: MediaMetadataRetriever? = null
    return try {
      retriever = MediaMetadataRetriever()
      retriever.setDataSource(context, Uri.parse(mediaUri))
      retriever.getFrameAtTime(positionMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
    } catch (e: Exception) {
      Log.w(TAG, "getFrameAtTime failed: ${e.message}")
      null
    } finally {
      try {
        retriever?.release()
      } catch (_: Exception) {}
    }
  }

  /**
   * Processes the bitmap with the active ColorMatrix, adjustments and aspect crop,
   * producing a real final rendered Bitmap.
   */
  fun renderProcessedBitmap(
    sourceBitmap: Bitmap,
    adjustments: AdjustmentsState
  ): Bitmap {
    // 1. Crop to selected aspect ratio if not original
    val croppedBitmap = if (adjustments.selectedCrop != CropAspect.ORIGINAL && adjustments.selectedCrop.ratio > 0f) {
      val targetRatio = adjustments.selectedCrop.ratio
      val srcWidth = sourceBitmap.width
      val srcHeight = sourceBitmap.height
      val srcRatio = srcWidth.toFloat() / srcHeight.toFloat()

      val cropRect = if (srcRatio > targetRatio) {
        // Source is wider: crop width
        val newWidth = (srcHeight * targetRatio).toInt().coerceIn(1, srcWidth)
        val left = (srcWidth - newWidth) / 2
        Rect(left, 0, left + newWidth, srcHeight)
      } else {
        // Source is taller: crop height
        val newHeight = (srcWidth / targetRatio).toInt().coerceIn(1, srcHeight)
        val top = (srcHeight - newHeight) / 2
        Rect(0, top, srcWidth, top + newHeight)
      }

      try {
        Bitmap.createBitmap(sourceBitmap, cropRect.left, cropRect.top, cropRect.width(), cropRect.height())
      } catch (e: Exception) {
        sourceBitmap
      }
    } else {
      sourceBitmap
    }

    // 2. Render with ColorMatrixColorFilter
    val outputBitmap = Bitmap.createBitmap(
      croppedBitmap.width,
      croppedBitmap.height,
      Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(outputBitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    val matrixValues = ColorMatrixEngine.createUnifiedMatrix(adjustments).values
    val androidColorMatrix = android.graphics.ColorMatrix(matrixValues)
    paint.colorFilter = android.graphics.ColorMatrixColorFilter(androidColorMatrix)

    canvas.drawBitmap(croppedBitmap, 0f, 0f, paint)

    return outputBitmap
  }

  /**
   * Saves the processed image directly into device Gallery (Pictures/LuminaStudio)
   * using Android MediaStore and creates a shareable FileProvider URI.
   */
  fun saveImageToDeviceGallery(
    context: Context,
    project: ProjectEntity?,
    adjustments: AdjustmentsState
  ): SaveResult {
    return try {
      val rawBitmap = loadProjectBitmapOrNull(context, project)
        ?: return SaveResult(
          isSuccess = false,
          mediaStoreUri = null,
          shareableUri = null,
          displayPath = "",
          errorMessage = "لا توجد وسائط مرفقة بالمشروع. قم باستيراد أو التقاط صورة أولاً."
        )
      val processedBitmap = renderProcessedBitmap(rawBitmap, adjustments)

      val fileName = "LUMINA_${System.currentTimeMillis()}.jpg"

      // 1. Insert into Android MediaStore (Permanent Gallery location)
      val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/LuminaStudio")
          put(MediaStore.Images.Media.IS_PENDING, 1)
        }
      }

      val resolver = context.contentResolver
      val mediaStoreUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

      if (mediaStoreUri != null) {
        resolver.openOutputStream(mediaStoreUri)?.use { out ->
          processedBitmap.compress(Bitmap.CompressFormat.JPEG, 98, out)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          contentValues.clear()
          contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
          resolver.update(mediaStoreUri, contentValues, null, null)
        }
      }

      // 2. Also save to app cache for instant FileProvider sharing
      val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
      val shareFile = File(cacheDir, fileName)
      FileOutputStream(shareFile).use { out ->
        processedBitmap.compress(Bitmap.CompressFormat.JPEG, 98, out)
      }

      val shareableUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        shareFile
      )

      SaveResult(
        isSuccess = true,
        mediaStoreUri = mediaStoreUri,
        shareableUri = shareableUri,
        displayPath = "Pictures/LuminaStudio/$fileName"
      )
    } catch (e: Exception) {
      Log.e(TAG, "Error saving photo: ${e.message}", e)
      SaveResult(
        isSuccess = false,
        mediaStoreUri = null,
        shareableUri = null,
        displayPath = "",
        errorMessage = e.localizedMessage ?: "فشل في حفظ الصورة"
      )
    }
  }

  /**
   * Registers a REAL exported MP4 file (produced by CinematicVideoExporter)
   * into the device Gallery (Movies/LuminaStudio) via MediaStore, and creates
   * a shareable FileProvider URI for it.
   */
  fun saveExportedVideoFileToGallery(context: Context, videoFile: File): SaveResult {
    return try {
      if (!videoFile.exists() || videoFile.length() == 0L) {
        return SaveResult(
          isSuccess = false,
          mediaStoreUri = null,
          shareableUri = null,
          displayPath = "",
          errorMessage = "ملف التصدير غير موجود"
        )
      }

      val fileName = videoFile.name
      val resolver = context.contentResolver

      val contentValues = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/LuminaStudio")
          put(MediaStore.Video.Media.IS_PENDING, 1)
        }
      }

      val mediaStoreUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
      if (mediaStoreUri == null) {
        return SaveResult(false, null, null, "", "فشل إنشاء الموقع في معرض الصور")
      }

      resolver.openOutputStream(mediaStoreUri)?.use { out ->
        videoFile.inputStream().use { input -> input.copyTo(out) }
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        contentValues.clear()
        contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
        resolver.update(mediaStoreUri, contentValues, null, null)
      }

      val cacheShareDir = File(context.cacheDir, "exports").apply { mkdirs() }
      val shareFile = File(cacheShareDir, fileName)
      videoFile.inputStream().use { input ->
        FileOutputStream(shareFile).use { out -> input.copyTo(out) }
      }

      val shareableUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        shareFile
      )

      SaveResult(
        isSuccess = true,
        mediaStoreUri = mediaStoreUri,
        shareableUri = shareableUri,
        displayPath = "Movies/LuminaStudio/$fileName"
      )
    } catch (e: Exception) {
      Log.e(TAG, "Error saving exported video: ${e.message}", e)
      SaveResult(
        isSuccess = false,
        mediaStoreUri = null,
        shareableUri = null,
        displayPath = "",
        errorMessage = e.localizedMessage ?: "فشل في حفظ الفيديو"
      )
    }
  }

  /**
   * Saves the video into device Gallery (Movies/LuminaStudio) by copying the
   * REAL source bytes when no re-encode is requested. Projects without a
   * source file are rejected honestly — writing JPEG bytes into an .mp4 was
   * the old fake behaviour and has been removed.
   */
  fun saveVideoToDeviceGallery(
    context: Context,
    project: ProjectEntity?,
    adjustments: AdjustmentsState
  ): SaveResult {
    if (project?.mediaUri.isNullOrBlank()) {
      return SaveResult(
        isSuccess = false,
        mediaStoreUri = null,
        shareableUri = null,
        displayPath = "",
        errorMessage = "لا يوجد فيديو مصدري في المشروع. استخدم التصدير السينمائي لإنتاج ملف حقيقي."
      )
    }
    return try {
      val fileName = "LUMINA_VIDEO_${System.currentTimeMillis()}.mp4"
      val resolver = context.contentResolver

      val contentValues = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/LuminaStudio")
          put(MediaStore.Video.Media.IS_PENDING, 1)
        }
      }

      val mediaStoreUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)

      val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
      val shareFile = File(cacheDir, fileName)

      val srcUri = Uri.parse(project!!.mediaUri)
      resolver.openInputStream(srcUri)?.use { input ->
        if (mediaStoreUri != null) {
          resolver.openOutputStream(mediaStoreUri)?.use { out ->
            input.copyTo(out)
          }
        }
      }
      resolver.openInputStream(srcUri)?.use { input ->
        FileOutputStream(shareFile).use { out ->
          input.copyTo(out)
        }
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mediaStoreUri != null) {
        contentValues.clear()
        contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
        resolver.update(mediaStoreUri, contentValues, null, null)
      }

      val shareableUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        shareFile
      )

      SaveResult(
        isSuccess = true,
        mediaStoreUri = mediaStoreUri,
        shareableUri = shareableUri,
        displayPath = "Movies/LuminaStudio/$fileName"
      )
    } catch (e: Exception) {
      Log.e(TAG, "Error saving video: ${e.message}", e)
      SaveResult(
        isSuccess = false,
        mediaStoreUri = null,
        shareableUri = null,
        displayPath = "",
        errorMessage = e.localizedMessage ?: "فشل في حفظ الفيديو"
      )
    }
  }

  /**
   * Triggers the real Android system share sheet with native Intent.ACTION_SEND
   * for an actual image or video file.
   */
  fun shareMediaFile(
    context: Context,
    fileUri: Uri,
    isVideo: Boolean,
    title: String
  ) {
    val mimeType = if (isVideo) "video/mp4" else "image/jpeg"
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
      type = mimeType
      putExtra(Intent.EXTRA_STREAM, fileUri)
      putExtra(Intent.EXTRA_SUBJECT, "Lumina Studio Export: $title")
      putExtra(
        Intent.EXTRA_TEXT,
        "تم التعديل والمعالجة عبر Lumina Studio\nالمشروع: $title"
      )
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooser = Intent.createChooser(shareIntent, "مشاركة عبر تطبيقات الهاتف")
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
      context.startActivity(chooser)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to launch native share chooser: ${e.message}")
    }
  }
}
