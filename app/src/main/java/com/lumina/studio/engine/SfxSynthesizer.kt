package com.lumina.studio.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import java.io.File
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Real procedural SFX synthesizer.
 *
 * Every library entry is synthesized deterministically into a genuine
 * 44.1 kHz / 16-bit PCM WAV file stored in the app's private storage the
 * first time it is needed. Preview playback uses a real MediaPlayer, and the
 * waveform shown in the UI is the real RMS envelope of the generated PCM.
 *
 * This replaces the previous fake "SFX library" whose play button toggled an
 * animated drawing without producing any sound at all.
 */
object SfxSynthesizer {

  private const val TAG = "SfxSynthesizer"
  private const val SAMPLE_RATE = 44_100

  data class SfxSpec(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val categoryAr: String,
    val durationMs: Long
  )

  val availableSfx: List<SfxSpec> = listOf(
    SfxSpec("sfx_whoosh", "انتقال سويش سينمائي فائق السرعة", "Cinematic Whoosh Transition FX", "انتقالات", 2500L),
    SfxSpec("sfx_boom", "ضربة بيس سفلية عميقة Sub Boom", "Deep Sub Bass Impact Drop", "درامي سينمائي", 4200L),
    SfxSpec("sfx_film_reel", "صوت بكرة فيلم ريترو 35mm", "Vintage 35mm Film Reel Ambient", "محيطي فنتدج", 8000L),
    SfxSpec("sfx_shutter", "مصراع كاميرا احترافية كلاسيكية", "Pro Mechanical Camera Shutter", "مؤثرات واقعية", 1200L),
    SfxSpec("sfx_rain", "مطر ورعد سينمائي هادئ", "Cinematic Rain & Distant Thunder", "محيطي فنتدج", 15000L),
    SfxSpec("sfx_cyber_riser", "تصاعد سايبربانك سينث رايزر 80s", "80s Cyberpunk Synth Riser FX", "درامي سينمائي", 5500L),
    SfxSpec("sfx_vinyl", "شوشرة أسطوانة فينيل دافئة", "Warm Vinyl Crackle & Lo-Fi Hiss", "محيطي فنتدج", 10000L)
  )

  fun specById(id: String): SfxSpec? = availableSfx.firstOrNull { it.id == id }

  private val pcmCache = HashMap<String, ShortArray>()
  private val envelopeCache = HashMap<String, AudioGraphEngine.AudioEnvelope>()

  // ---------------------------------------------------------------------
  // File management
  // ---------------------------------------------------------------------

  private fun sfxDir(context: Context): File =
    File(context.filesDir, "sfx").apply { mkdirs() }

  fun getFile(context: Context, id: String): File {
    val dir = sfxDir(context)
    val file = File(dir, "$id.wav")
    if (!file.exists() || file.length() == 0L) {
      val spec = specById(id) ?: throw IllegalArgumentException("Unknown SFX id: $id")
      val pcm = synthesize(spec)
      AudioGraphEngine.writeWavFile(file, pcm, SAMPLE_RATE)
      pcmCache[id] = pcm
      envelopeCache.remove(id)
    }
    return file
  }

  fun ensureAllGenerated(context: Context): Map<String, File> {
    return availableSfx.associate { it.id to getFile(context, it.id) }
  }

  // ---------------------------------------------------------------------
  // Real envelope from the generated PCM (for waveforms / VU / beats)
  // ---------------------------------------------------------------------

  fun getEnvelope(context: Context, id: String): AudioGraphEngine.AudioEnvelope? {
    envelopeCache[id]?.let { return it }
    val pcm = getPcm(context, id) ?: return null
    val windowSamples = SAMPLE_RATE * 40L / 1000L
    val windows = (pcm.size / windowSamples).coerceAtLeast(1)
    val rms = FloatArray(windows)
    var peak = 0f
    for (w in 0 until windows) {
      var sum = 0.0
      val start = w * windowSamples
      val end = min(pcm.size, (start + windowSamples).toInt())
      for (i in start until end) {
        val v = pcm[i] / 32768.0
        sum += v * v
      }
      val r = kotlin.math.sqrt(sum / (end - start)).toFloat()
      rms[w] = r
      peak = max(peak, r)
    }
    if (peak <= 1e-6f) peak = 1f
    val envelope = AudioGraphEngine.AudioEnvelope(
      durationMs = pcm.size * 1000L / SAMPLE_RATE,
      sampleRate = SAMPLE_RATE,
      channelCount = 1,
      rms = rms,
      windowMs = 40L,
      peakRms = peak
    )
    envelopeCache[id] = envelope
    return envelope
  }

  fun getPcm(context: Context, id: String): ShortArray? {
    pcmCache[id]?.let { return it }
    return try {
      val file = getFile(context, id)
      // Re-read from disk so cache and file always agree.
      val bytes = file.readBytes()
      val shorts = ShortArray(bytes.size / 2)
      for (i in shorts.indices) {
        shorts[i] = (((bytes[i * 2 + 1].toInt() and 0xFF) shl 8) or (bytes[i * 2].toInt() and 0xFF)).toShort()
      }
      pcmCache[id] = shorts
      shorts
    } catch (e: Exception) {
      Log.w(TAG, "Failed to load SFX PCM for $id: ${e.message}")
      null
    }
  }

  // ---------------------------------------------------------------------
  // Real preview playback (actual sound!)
  // ---------------------------------------------------------------------

  private var previewPlayer: MediaPlayer? = null
  private var currentlyPlayingId: String? = null

  /** Starts REAL audio playback of the given SFX. Returns the id played. */
  fun playPreview(context: Context, id: String, volume: Float = 1f): String? {
    stopPreview()
    return try {
      val file = getFile(context, id)
      val player = MediaPlayer()
      player.setAudioAttributes(
        AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_MEDIA)
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .build()
      )
      player.setDataSource(file.absolutePath)
      player.setVolume(volume.coerceIn(0f, 1f), volume.coerceIn(0f, 1f))
      player.setOnCompletionListener {
        if (currentlyPlayingId == id) {
          currentlyPlayingId = null
        }
        it.release()
        if (previewPlayer === it) previewPlayer = null
      }
      player.prepare()
      player.start()
      previewPlayer = player
      currentlyPlayingId = id
      id
    } catch (e: Exception) {
      Log.e(TAG, "SFX preview failed: ${e.message}")
      null
    }
  }

  fun stopPreview() {
    previewPlayer?.let {
      try {
        it.stop()
      } catch (_: Exception) {}
      try {
        it.release()
      } catch (_: Exception) {}
    }
    previewPlayer = null
    currentlyPlayingId = null
  }

  fun isPlaying(id: String): Boolean = currentlyPlayingId == id

  // ---------------------------------------------------------------------
  // DSP synthesis — deterministic, seeded, real audio
  // ---------------------------------------------------------------------

  private fun synthesize(spec: SfxSpec): ShortArray {
    val sampleCount = (SAMPLE_RATE * spec.durationMs / 1000.0).toInt()
    val rnd = Random(spec.id.hashCode() * 7919L) // deterministic
    val samples = when (spec.id) {
      "sfx_whoosh" -> synthWhoosh(sampleCount, rnd)
      "sfx_boom" -> synthBoom(sampleCount, rnd)
      "sfx_film_reel" -> synthFilmReel(sampleCount, rnd)
      "sfx_shutter" -> synthShutter(sampleCount, rnd)
      "sfx_rain" -> synthRain(sampleCount, rnd)
      "sfx_cyber_riser" -> synthRiser(sampleCount, rnd)
      "sfx_vinyl" -> synthVinyl(sampleCount, rnd)
      else -> FloatArray(sampleCount)
    }
    // Normalize to -0.9 dBFS peak (real loudness control)
    var peak = 1e-6f
    for (v in samples) peak = max(peak, kotlin.math.abs(v))
    val gain = (0.9f / peak).coerceAtMost(4f)
    return ShortArray(sampleCount) { i -> (samples[i] * gain * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort() }
  }

  private fun onePoleLowpass(input: FloatArray, cutoffNorm: Float): FloatArray {
    val out = FloatArray(input.size)
    val a = exp(-2.0 * PI * cutoffNorm).toFloat()
    var y = 0f
    for (i in input.indices) {
      y = (1f - a) * input[i] + a * y
      out[i] = y
    }
    return out
  }

  private fun synthWhoosh(n: Int, rnd: Random): FloatArray {
    val noise = FloatArray(n) { rnd.nextFloat() * 2f - 1f }
    val out = FloatArray(n)
    var lp1 = 0f
    var lp2 = 0f
    for (i in 0 until n) {
      val t = i.toFloat() / n
      // Sweeping band: lowpass cutoff rises 0.02 → 0.45 then falls
      val sweep = 0.03f + 0.42f * sin(PI * t).toFloat().let { it * it }
      val a = exp(-2.0 * PI * sweep).toFloat()
      lp1 += (1f - a) * (noise[i] - lp1)
      lp2 += (1f - a * 0.35f) * (lp1 - lp2)
      val band = lp1 - lp2
      // Motion envelope: sharp swell, fast tail
      val env = 4f * t * (1f - t).pow(2.2f)
      out[i] = band * 5.5f * env
    }
    return out
  }

  private fun synthBoom(n: Int, rnd: Random): FloatArray {
    val out = FloatArray(n)
    var lp = 0f
    for (i in 0 until n) {
      val t = i.toFloat() / n
      val freq = 42f + 85f * exp(-t * 9f) // pitch drop 127→42 Hz
      val phase = 2.0 * PI * freq * (i.toDouble() / SAMPLE_RATE)
      val body = sin(phase).toFloat() * exp(-t * 3.4f)
      val sub = sin(phase / 2.0).toFloat() * exp(-t * 2.2f) * 0.6f
      val thump = (rnd.nextFloat() * 2f - 1f) * exp(-t * 55f) * 0.8f
      lp += 0.25f * (thump - lp)
      out[i] = body * 0.9f + sub * 0.6f + lp * 0.5f
    }
    return out
  }

  private fun synthFilmReel(n: Int, rnd: Random): FloatArray {
    val out = FloatArray(n)
    var lp = 0f
    val tickPeriod = (SAMPLE_RATE * 0.486f).toInt() // ~24 fps projector cadence
    for (i in 0 until n) {
      val t = i.toFloat() / n
      // Mechanical hum (motor)
      val hum = 0.16f * sin(2.0 * PI * 47.0 * (i.toDouble() / SAMPLE_RATE)).toFloat()
      // Sprocket ticks
      val inTickWindow = i % tickPeriod < 260
      val tick = if (inTickWindow) (rnd.nextFloat() * 2f - 1f) * 0.5f * exp(-(i % tickPeriod) / 60f) else 0f
      lp += 0.4f * (tick - lp)
      val wobble = 1f + 0.08f * sin(2.0 * PI * 0.9 * t * 8.0)
      out[i] = (hum + lp * 1.4f) * 0.8f * wobble
    }
    return out
  }

  private fun synthShutter(n: Int, rnd: Random): FloatArray {
    val out = FloatArray(n)
    fun click(atSample: Int, length: Int, gain: Float) {
      for (k in 0 until length) {
        val idx = atSample + k
        if (idx >= n) break
        val env = exp(-k / (length / 6f)).toFloat()
        out[idx] += (rnd.nextFloat() * 2f - 1f) * env * gain
      }
    }
    click(SAMPLE_RATE / 50, 1400, 0.9f)           // mirror up
    click(SAMPLE_RATE * 95 / 1000, 2200, 1.0f)    // curtain
    click(SAMPLE_RATE * 210 / 1000, 1600, 0.7f)   // mirror down
    return onePoleLowpass(out, 0.55f)
  }

  private fun synthRain(n: Int, rnd: Random): FloatArray {
    val hiss = FloatArray(n) { rnd.nextFloat() * 2f - 1f }
    val rain = onePoleLowpass(hiss, 0.32f)
    val hpRain = FloatArray(n) { i -> hiss[i] - rain[i] } // remove rumble → patter
    val out = FloatArray(n)
    var lpThunder = 0f
    for (i in 0 until n) {
      val t = i.toFloat() / n
      val patter = hpRain[i] * 0.5f
      out[i] = patter
      // distant thunder at ~32% and ~68% of the file
      if ((t > 0.32f && t < 0.45f) || (t > 0.68f && t < 0.82f)) {
        val rumble = (rnd.nextFloat() * 2f - 1f) * 0.8f
        lpThunder += 0.015f * (rumble - lpThunder)
        val swell = sin(PI * ((t % 0.2f) / 0.2f)).toFloat()
        out[i] += lpThunder * 2.4f * swell
      }
    }
    return out
  }

  private fun synthRiser(n: Int, rnd: Random): FloatArray {
    val out = FloatArray(n)
    var phase = 0.0
    for (i in 0 until n) {
      val t = i.toFloat() / n
      val freq = 160f * 2f.pow(t * 3.0f) // 160 Hz → ~1.3 kHz exponential sweep
      phase += 2.0 * PI * freq / SAMPLE_RATE
      val tone = sin(phase).toFloat() * 0.55f
      val fifth = sin(phase * 1.5).toFloat() * 0.3f
      val noise = (rnd.nextFloat() * 2f - 1f) * 0.22f * t
      val env = t * t * (1f - 0.05f * sin(t * 40.0).toFloat())
      out[i] = (tone + fifth + noise) * env
    }
    // hard cutoff at the end (classic riser exit)
    val cutAt = (n * 0.97f).toInt()
    for (i in cutAt until n) out[i] *= (n - i).toFloat() / (n - cutAt).toFloat()
    return out
  }

  private fun synthVinyl(n: Int, rnd: Random): FloatArray {
    val hiss = FloatArray(n) { rnd.nextFloat() * 2f - 1f }
    val low = onePoleLowpass(hiss, 0.6f)
    val hissHi = FloatArray(n) { i -> low[i] * 1f } // gentle
    val out = FloatArray(n)
    var crackleLp = 0f
    var nextCrackle = 0
    for (i in 0 until n) {
      val t = i.toFloat() / n
      // crackle impulses
      if (i >= nextCrackle) {
        val burstLen = 40 + rnd.nextInt(240)
        val amp = 0.25f + rnd.nextFloat() * 0.5f
        var k = 0
        while (k < burstLen && i + k < n) {
          val env = exp(-k / 60f).toFloat()
          out[i + k] += (rnd.nextFloat() * 2f - 1f) * env * amp
          k++
        }
        nextCrackle = i + (SAMPLE_RATE * (0.02f + rnd.nextFloat() * 0.12f)).toInt()
      }
      crackleLp += 0.5f * (out[i] - crackleLp)
      val wobble = 1f + 0.05f * sin(2.0 * PI * 0.33 * t * 6.0)
      out[i] = crackleLp * 1.1f + hissHi[i] * 0.12f * wobble
    }
    return out
  }
}
