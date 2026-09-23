package com.lumina.studio.ui.editor

import android.content.Context
import com.lumina.studio.data.model.EditorMode
import com.lumina.studio.data.model.ExportCodec
import com.lumina.studio.data.model.VideoResolution

/**
 * Real persisted studio preferences (SharedPreferences).
 * These values genuinely drive app behaviour:
 *  - [getDefaultEditorMode] is applied by StudioViewModel at construction
 *  - export defaults pre-select the export sheet options
 */
object StudioPreferences {

  private const val PREFS = "lumina_studio_prefs"
  private const val KEY_EDITOR_MODE = "editor_mode"
  private const val KEY_EXPORT_RESOLUTION = "export_resolution"
  private const val KEY_EXPORT_FPS = "export_fps"
  private const val KEY_EXPORT_CODEC = "export_codec"

  private fun prefs(context: Context) =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

  fun getDefaultEditorMode(context: Context): EditorMode {
    val raw = prefs(context).getString(KEY_EDITOR_MODE, EditorMode.BEGINNER.name)
    return try {
      EditorMode.valueOf(raw ?: EditorMode.BEGINNER.name)
    } catch (e: Exception) {
      EditorMode.BEGINNER
    }
  }

  fun setDefaultEditorMode(context: Context, mode: EditorMode) {
    prefs(context).edit().putString(KEY_EDITOR_MODE, mode.name).apply()
  }

  fun getDefaultExportResolution(context: Context): VideoResolution {
    val raw = prefs(context).getString(KEY_EXPORT_RESOLUTION, VideoResolution.FHD_1080P.name)
    return try {
      VideoResolution.valueOf(raw ?: VideoResolution.FHD_1080P.name)
    } catch (e: Exception) {
      VideoResolution.FHD_1080P
    }
  }

  fun setDefaultExportResolution(context: Context, resolution: VideoResolution) {
    prefs(context).edit().putString(KEY_EXPORT_RESOLUTION, resolution.name).apply()
  }

  fun getDefaultExportFps(context: Context): Int =
    prefs(context).getInt(KEY_EXPORT_FPS, 30)

  fun setDefaultExportFps(context: Context, fps: Int) {
    prefs(context).edit().putInt(KEY_EXPORT_FPS, fps.coerceIn(24, 60)).apply()
  }

  fun getDefaultExportCodec(context: Context): ExportCodec {
    val raw = prefs(context).getString(KEY_EXPORT_CODEC, ExportCodec.AVC_H264.name)
    return try {
      ExportCodec.valueOf(raw ?: ExportCodec.AVC_H264.name)
    } catch (e: Exception) {
      ExportCodec.AVC_H264
    }
  }

  fun setDefaultExportCodec(context: Context, codec: ExportCodec) {
    prefs(context).edit().putString(KEY_EXPORT_CODEC, codec.name).apply()
  }
}
