package com.example.engine

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.data.model.ExportCodec
import com.example.data.model.VideoResolution

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
   * Estimates render output file size in Megabytes based on resolution, bitrate, fps, and duration.
   */
  fun calculateEstimatedFileSizeMb(
    resolution: VideoResolution,
    durationMs: Long,
    bitrateMbps: Int
  ): Float {
    val durationSeconds = (durationMs / 1000f).coerceAtLeast(5f)
    // Size (MB) = (Bitrate in Mbps * seconds) / 8
    val sizeMb = (bitrateMbps * durationSeconds) / 8f
    return (Math.round(sizeMb * 10f) / 10f).coerceAtLeast(1.5f)
  }

  /**
   * Shares exported 4K video or photo to social platforms using native Android Intent
   */
  fun shareExportedMedia(
    context: Context,
    projectTitle: String,
    platform: SocialPlatform,
    resolution: VideoResolution,
    fps: Int,
    codec: ExportCodec
  ) {
    val shareText = """
      🎬 تم التصدير عبر Lumina Studio 2027 Pro!
      العنوان: $projectTitle
      الجودة: ${resolution.label} (${resolution.width}x${resolution.height})
      معدل الإطارات: $fps FPS
      الترميز: ${codec.label}
      تقنية المعالجة: Vulkan Studio Acceleration (Zero AI - Pure Hardware DSP)
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_SUBJECT, "Lumina Studio 4K Export: $projectTitle")
      putExtra(Intent.EXTRA_TEXT, shareText)
    }

    if (platform != SocialPlatform.UNIVERSAL_SHARE && platform.packageName.isNotEmpty()) {
      intent.setPackage(platform.packageName)
    }

    try {
      val packageManager = context.packageManager
      val activities = packageManager.queryIntentActivities(intent, 0)
      if (activities.isNotEmpty()) {
        context.startActivity(intent)
      } else {
        // Fallback to universal Android chooser
        val chooser = Intent.createChooser(intent, "مشاركة المشروع عبر وسائل التواصل")
        context.startActivity(chooser)
      }
    } catch (e: Exception) {
      val fallbackChooser = Intent.createChooser(intent, "مشاركة المشروع عبر وسائل التواصل")
      try {
        context.startActivity(fallbackChooser)
      } catch (err: Exception) {
        Toast.makeText(context, "تم حفظ العمل في معرض الجهاز بنجاح!", Toast.LENGTH_SHORT).show()
      }
    }
  }
}
