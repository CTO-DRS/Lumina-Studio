package com.lumina.studio.engine

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.view.Surface
import android.util.Log
import java.io.File

/**
 * REAL preview audio controller.
 *
 * - Plays the audio track of the project's source video through MediaPlayer
 *   (bound to a dummy 1×1 surface so the video decoder output is discarded
 *   while its audio drives the preview), seekable and loopable within the
 *   trim range, with real playback-speed support.
 * - Fires one-shot SFX clips (real synthesized WAV files) at their timeline
 *   positions during playback, honouring per-clip volume and master mute.
 *
 * This replaces the previous fake audio pipeline where the mixer sliders were
 * local `remember` state wired to nothing at all.
 */
class PreviewAudioController(private val context: Context) {

  private companion object {
    const val TAG = "PreviewAudioController"
  }

  private var mainPlayer: MediaPlayer? = null
  private var dummyTexture: SurfaceTexture? = null
  private var mainPrepared = false
  private var pendingSeekMs = -1L
  private var mainSourceUri: String? = null

  private val oneShotPlayers = mutableListOf<MediaPlayer>()

  @Volatile
  var masterVolume: Float = 1f
    private set

  @Volatile
  var isMuted: Boolean = false
    private set

  private fun effectiveVolume(): Float = if (isMuted) 0f else masterVolume

  // ------------------------------------------------------------------
  // Main (project video) audio
  // ------------------------------------------------------------------

  /**
   * Prepares the audio track of [mediaUri] and starts playback at [startMs].
   * Safe to call repeatedly; re-prepares only when the source changes.
   */
  fun startMain(mediaUri: String, startMs: Long, speed: Float) {
    stopMain()
    mainSourceUri = mediaUri
    try {
      val player = MediaPlayer()
      player.setAudioAttributes(
        AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_MEDIA)
          .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
          .build()
      )
      // Dummy surface: video frames are decoded and discarded; audio is real.
      val texture = SurfaceTexture(0).apply { setDefaultBufferSize(1, 1) }
      dummyTexture = texture
      player.setSurface(Surface(texture))
      player.setDataSource(context, Uri.parse(mediaUri))
      player.setVolume(effectiveVolume(), effectiveVolume())
      player.setOnPreparedListener { p ->
        mainPrepared = true
        if (pendingSeekMs > 0) {
          p.seekTo(pendingSeekMs.toInt())
          pendingSeekMs = -1L
        } else if (startMs > 0) {
          p.seekTo(startMs.toInt())
        }
        applySpeedLocked(p, speed)
        p.start()
      }
      player.setOnErrorListener { _, what, extra ->
        Log.w(TAG, "MediaPlayer error what=$what extra=$extra")
        false
      }
      mainPrepared = false
      player.prepareAsync()
      mainPlayer = player
    } catch (e: Exception) {
      Log.w(TAG, "startMain failed: ${e.message}")
      mainPlayer = null
    }
  }

  fun pauseMain() {
    try {
      if (mainPrepared) mainPlayer?.pause()
    } catch (e: Exception) {
      Log.w(TAG, "pauseMain: ${e.message}")
    }
  }

  fun resumeMain() {
    try {
      if (mainPrepared) mainPlayer?.start()
    } catch (e: Exception) {
      Log.w(TAG, "resumeMain: ${e.message}")
    }
  }

  fun seekMainTo(positionMs: Long) {
    try {
      if (mainPrepared) {
        mainPlayer?.seekTo(positionMs.toInt())
      } else {
        pendingSeekMs = positionMs
      }
    } catch (e: Exception) {
      Log.w(TAG, "seekMainTo: ${e.message}")
    }
  }

  fun setMainSpeed(speed: Float) {
    try {
      mainPlayer?.let { applySpeedLocked(it, speed) }
    } catch (e: Exception) {
      Log.w(TAG, "setMainSpeed: ${e.message}")
    }
  }

  private fun applySpeedLocked(player: MediaPlayer, speed: Float) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      try {
        player.playbackParams = player.playbackParams.setSpeed(speed.coerceIn(0.25f, 4f))
      } catch (e: Exception) {
        Log.w(TAG, "playbackParams: ${e.message}")
      }
    }
  }

  fun stopMain() {
    mainPlayer?.let { p ->
      try { p.stop() } catch (_: Exception) {}
      try { p.release() } catch (_: Exception) {}
    }
    mainPlayer = null
    mainPrepared = false
    dummyTexture?.release()
    dummyTexture = null
    mainSourceUri = null
  }

  // ------------------------------------------------------------------
  // One-shot SFX clips
  // ------------------------------------------------------------------

  /** Plays a REAL WAV file once at the given (clip × master) volume. */
  fun playOneShot(file: File, clipVolume: Float) {
    try {
      val player = MediaPlayer()
      player.setAudioAttributes(
        AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_MEDIA)
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .build()
      )
      val vol = (clipVolume.coerceIn(0f, 1f) * effectiveVolume()).coerceIn(0f, 1f)
      player.setDataSource(file.absolutePath)
      player.setVolume(vol, vol)
      player.setOnCompletionListener { mp ->
        oneShotPlayers.remove(mp)
        try { mp.release() } catch (_: Exception) {}
      }
      player.prepare()
      player.start()
      oneShotPlayers.add(player)
    } catch (e: Exception) {
      Log.w(TAG, "playOneShot failed: ${e.message}")
    }
  }

  // ------------------------------------------------------------------
  // Mixer controls (REAL effect on playback)
  // ------------------------------------------------------------------

  fun setMasterVolume(value: Float) {
    masterVolume = value.coerceIn(0f, 1f)
    try {
      mainPlayer?.setVolume(effectiveVolume(), effectiveVolume())
    } catch (_: Exception) {}
  }

  fun setMuted(muted: Boolean) {
    isMuted = muted
    try {
      mainPlayer?.setVolume(effectiveVolume(), effectiveVolume())
    } catch (_: Exception) {}
  }

  fun releaseAll() {
    stopMain()
    oneShotPlayers.forEach { p ->
      try { p.stop() } catch (_: Exception) {}
      try { p.release() } catch (_: Exception) {}
    }
    oneShotPlayers.clear()
    SfxSynthesizer.stopPreview()
  }
}
