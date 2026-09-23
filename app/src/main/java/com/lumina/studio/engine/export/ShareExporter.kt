package com.lumina.studio.engine.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.lumina.studio.data.model.ExportCodec
import com.lumina.studio.data.model.VideoResolution
import java.io.File

/**
 * Real share exporter.
 *
 * shareExportedMedia() now shares the ACTUAL exported file via
 * Intent.ACTION_SEND + EXTRA_STREAM (previously it shared a text description
 * of settings that were never rendered — the classic fake export).
 */
object ShareExporter {

  enum class SocialPlatform(
    val platformName: String,
    val packageName: String,
    val iconName: String,
    val colorHex: Long
  ) {
    INSTAGRAM("انستغرام Reels / Stories", "com.instagram.android", "instagram", 0xFFE1306C),
    TIKTOK("تيك توك", "com.zhiliaoapp.musically", "tiktok", 0xFF000000),
    YOUTUBE("يوتيوب Shorts / 4K", "com.google.android.youtube", "youtube", 0xFFFF0000),
    WHATSAPP("واتساب HD", "com.whatsapp", "whatsapp", 0xFF25D366),
    X_TWITTER("منصة X", "com.twitter.android", "twitter", 0xFF1DA1F2),
    TELEGRAM("تيليجرام 4K", "org.telegram.messenger", "telegram", 0xFF0088CC),
    UNIVERSAL_SHARE("مشاركة عبر كافة التطبيقات", "", "share", 0xFF00F0FF)
  }

  /**
   * Estimates render output file size in Megabytes from bitrate × duration
   * (pure, real math used by the export sheet).
   */
  fun calculateEstimatedFileSizeMb(
    resolution: VideoResolution,
    durationMs: Long,
    bitrateMbps: Int
  ): Float {
    val durationSeconds = (durationMs / 1000f).coerceAtLeast(5f)
    val sizeMb = (bitrateMbps * durationSeconds) / 8f
    return (Math.round(sizeMb * 10f) / 10f).coerceAtLeast(0.1f)
  }

  /**
   * Shares the real exported media FILE to the chosen platform.
   *
   * @param fileUri a content:// FileProvider URI (or mediaStore URI) pointing
   *                to the actual exported file.
   */
  fun shareExportedMedia(
    context: Context,
    fileUri: Uri,
    isVideo: Boolean,
    title: String,
    platform: SocialPlatform
  ) {
    val mimeType = if (isVideo) "video/mp4" else "image/jpeg"
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
      type = mimeType
      putExtra(Intent.EXTRA_STREAM, fileUri)
      putExtra(Intent.EXTRA_SUBJECT, "Lumina Studio Export: $title")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    if (platform != SocialPlatform.UNIVERSAL_SHARE && platform.packageName.isNotEmpty()) {
      shareIntent.setPackage(platform.packageName)
    }

    val chooserTitle = "مشاركة «$title» عبر ${platform.platformName}"
    try {
      val packageManager = context.packageManager
      val activities = packageManager.queryIntentActivities(shareIntent, 0)
      if (activities.isNotEmpty()) {
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
      } else {
        // Target platform not installed → honest universal chooser fallback
        val chooser = Intent.createChooser(shareIntent, chooserTitle)
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
      }
    } catch (e: Exception) {
      try {
        val fallbackChooser = Intent.createChooser(shareIntent, chooserTitle)
        fallbackChooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(fallbackChooser)
      } catch (err: Exception) {
        // Nothing left to try — surface the failure honestly.
        android.widget.Toast.makeText(
          context,
          "تعذر بدء المشاركة: ${err.localizedMessage ?: err.message}",
          android.widget.Toast.LENGTH_SHORT
        ).show()
      }
    }
  }

  /** Convenience overload used right after a real export lands on disk. */
  fun shareExportedFile(
    context: Context,
    outputFile: File,
    isVideo: Boolean,
    title: String,
    platform: SocialPlatform
  ): Boolean {
    return try {
      val shareUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        outputFile
      )
      shareExportedMedia(context, shareUri, isVideo, title, platform)
      true
    } catch (e: Exception) {
      false
    }
  }
}
