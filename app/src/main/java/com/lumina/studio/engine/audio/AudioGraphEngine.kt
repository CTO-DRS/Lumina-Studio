package com.lumina.studio.engine.audio

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Real audio graph engine.
 *
 * Decodes the ACTUAL audio track of any media file on the device through
 * MediaExtractor + MediaCodec into 16-bit PCM, then derives:
 *
 *  - a normalized RMS energy envelope ([AudioEnvelope.rmsByWindowMs]) used for
 *    the VU meter, timeline waveforms and zebra-honest silence detection
 *  - real rhythmic beat markers via adaptive-threshold transient peak picking
 *    ([detectBeats]) — no fake BPM assumptions anywhere.
 */
object AudioGraphEngine {

  private const val TAG = "AudioGraphEngine"

  /** Window length of the energy envelope. 40 ms ≈ 25 envelopes/second. */
  private const val WINDOW_MS = 40L

  data class AudioEnvelope(
    /** Real file duration in ms (from the extractor format). */
    val durationMs: Long,
    val sampleRate: Int,
    val channelCount: Int,
    /** One RMS value (0..1) per [windowMs] of audio. */
    val rms: FloatArray,
    val windowMs: Long,
    val peakRms: Float
  ) {
    val windowCount: Int get() = rms.size

    /**
     * Real amplitude (0..1) at an arbitrary timeline position, measured from
     * the decoded PCM — this is what drives the VU meter during playback.
     */
    fun amplitudeAt(positionMs: Long): Float {
      if (rms.isEmpty() || peakRms <= 0f) return 0f
      val idx = (positionMs / windowMs).toInt().coerceIn(0, rms.size - 1)
      return (rms[idx] / peakRms).coerceIn(0f, 1f)
    }

    /**
     * Downsamples the envelope to exactly [barCount] real amplitude bars for
     * waveform drawing (each bar = max of its slice → preserves transients).
     */
    fun waveformBars(barCount: Int): FloatArray {
      if (barCount <= 0 || rms.isEmpty()) return FloatArray(max(0, barCount))
      val out = FloatArray(barCount)
      val slice = rms.size.toFloat() / barCount
      for (b in 0 until barCount) {
        val start = (b * slice).toInt()
        val end = min(rms.size, ((b + 1) * slice).toInt().coerceAtLeast(start + 1))
        var m = 0f
        for (i in start until end) m = max(m, rms[i])
        out[b] = if (peakRms > 0f) (m / peakRms).coerceIn(0f, 1f) else 0f
      }
      return out
    }
  }

  // ---------------------------------------------------------------------
  // Real PCM decode
  // ---------------------------------------------------------------------

  /**
   * Decodes the audio track of [uri] and computes the real RMS envelope.
   * [maxDurationMs] caps the decode length for very long files (0 = full file).
   * Returns null when the media has no decodable audio track.
   */
  fun decodeEnvelope(
    context: Context,
    uri: Uri,
    maxDurationMs: Long = 0L,
    onProgress: ((Float) -> Unit)? = null
  ): AudioEnvelope? {
    val extractor = MediaExtractor()
    try {
      context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
        extractor.setDataSource(pfd.fileDescriptor)
      } ?: return null

      var audioTrackIndex = -1
      var format: MediaFormat? = null
      for (i in 0 until extractor.trackCount) {
        val f = extractor.getTrackFormat(i)
        val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
        if (mime.startsWith("audio/")) {
          audioTrackIndex = i
          format = f
          break
        }
      }
      if (audioTrackIndex < 0 || format == null) return null

      extractor.selectTrack(audioTrackIndex)
      val mime = format.getString(MediaFormat.KEY_MIME)!!
      val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
      val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

      val totalDurationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
        format.getLong(MediaFormat.KEY_DURATION)
      } else 0L
      val durationMs = totalDurationUs / 1000L

      val codec = MediaCodec.createDecoderByType(mime)
      codec.configure(format, null, null, 0)
      codec.start()

      // Two passes would be cleaner, but one pass into a growing array is
      // simpler and avoids holding raw PCM: we accumulate energy per window.
      val windowSizeSamples = (sampleRate * WINDOW_MS / 1000L).toInt().coerceAtLeast(1)
      val rmsList = ArrayList<Float>(max(64, (durationMs / WINDOW_MS).toInt() + 1))

      var currentWindowSum = 0.0
      var currentWindowCount = 0
      var currentWindowSamples = 0
      var lastProgressPublish = 0L
      var sawInputEos = false
      var sawOutputEos = false
      val bufferInfo = MediaCodec.BufferInfo()

      fun flushWindow() {
        if (currentWindowCount > 0) {
          rmsList.add(sqrt(currentWindowSum / currentWindowCount).toFloat())
        }
        currentWindowSum = 0.0
        currentWindowCount = 0
      }

      try {
        while (!sawOutputEos) {
          if (!sawInputEos) {
            val inIdx = codec.dequeueInputBuffer(10_000)
            if (inIdx >= 0) {
              val input = codec.getInputBuffer(inIdx)!!
              val size = extractor.readSampleData(input, 0)
              if (size < 0) {
                codec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                sawInputEos = true
              } else {
                val pts = extractor.sampleTime
                codec.queueInputBuffer(inIdx, 0, size, pts, 0)
                extractor.advance()
              }
            }
          }

          val outIdx = codec.dequeueOutputBuffer(bufferInfo, 10_000)
          when {
            outIdx >= 0 -> {
              val out = codec.getOutputBuffer(outIdx)
              if (out != null && bufferInfo.size > 0) {
                out.position(bufferInfo.offset)
                out.limit(bufferInfo.offset + bufferInfo.size)
                val isPcm = bufferInfo.size % 2 == 0
                if (isPcm) {
                  val shortBuf = out.order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                  val remaining = shortBuf.remaining()
                  val samples = ShortArray(remaining)
                  shortBuf.get(samples)
                  for (s in samples) {
                    val v = (s.toDouble() / Short.MAX_VALUE)
                    currentWindowSum += v * v
                    currentWindowCount++
                    currentWindowSamples++
                    if (currentWindowSamples >= windowSizeSamples) {
                      flushWindow()
                      currentWindowSamples = 0
                    }
                  }
                }
              }
              codec.releaseOutputBuffer(outIdx, false)
              if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                sawOutputEos = true
              }
              if (maxDurationMs > 0 && rmsList.isNotEmpty() &&
                rmsList.size * WINDOW_MS >= maxDurationMs
              ) {
                sawOutputEos = true
              }
              if (onProgress != null && totalDurationUs > 0) {
                val progress = (bufferInfo.presentationTimeUs.toFloat() / totalDurationUs)
                val now = System.nanoTime() / 1_000_000L
                if (now - lastProgressPublish > 50) {
                  lastProgressPublish = now
                  onProgress(progress.coerceIn(0f, 1f))
                }
              }
            }
            outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
              // Some decoders emit the real output format here; keep going.
            }
            else -> {
              // TRY_AGAIN_LATER — just loop.
            }
          }
        }
        flushWindow()
      } finally {
        try {
          codec.stop()
        } catch (_: Exception) {}
        try {
          codec.release()
        } catch (_: Exception) {}
      }

      if (rmsList.isEmpty()) return null

      var peak = 0f
      for (v in rmsList) peak = max(peak, v)
      if (peak <= 1e-6f) peak = 1f

      return AudioEnvelope(
        durationMs = if (durationMs > 0) durationMs else rmsList.size * WINDOW_MS,
        sampleRate = sampleRate,
        channelCount = channels,
        rms = rmsList.toFloatArray(),
        windowMs = WINDOW_MS,
        peakRms = peak
      )
    } catch (e: Exception) {
      Log.w(TAG, "Audio decode failed: ${e.message}")
      return null
    } finally {
      try {
        extractor.release()
      } catch (_: Exception) {}
    }
  }

  // ---------------------------------------------------------------------
  // Real transient/beat detection on the measured envelope
  // ---------------------------------------------------------------------

  data class BeatDetectionResult(
    val beatMarkersMs: List<Long>,
    val detectedBpm: Int,
    val transientCount: Int,
    val descriptionAr: String
  )

  /**
   * Adaptive-threshold onset detection:
   *  1. energy envelope E[n] (real RMS, 40 ms windows)
   *  2. spectral-flux-like positive derivative D[n] = max(0, E[n] - E[n-1])
   *  3. local mean M[n] over ±W windows → adaptive threshold T[n] = M[n] * k + δ
   *  4. peak picked where D[n] > T[n] and D[n] is a local maximum
   *  5. refractory period of 200 ms prevents double triggers
   *  6. BPM = median inter-onset interval mapped into 60..180
   */
  fun detectBeats(envelope: AudioEnvelope, sensitivity: Float = 1.9f): BeatDetectionResult {
    val rms = envelope.rms
    if (rms.size < 8) {
      return BeatDetectionResult(emptyList(), 0, 0, "المقطع قصير جداً لرصد الإيقاع")
    }

    // 1. positive flux
    val flux = FloatArray(rms.size)
    for (i in 1 until rms.size) flux[i] = max(0f, rms[i] - rms[i - 1])

    // 2..4 adaptive threshold peak picking
    val halfWindow = 12 // ±480 ms context
    val markers = ArrayList<Long>()
    for (i in 1 until flux.size - 1) {
      val from = max(0, i - halfWindow)
      val to = min(flux.size - 1, i + halfWindow)
      var mean = 0f
      for (j in from..to) mean += flux[j]
      mean /= (to - from + 1)
      val threshold = mean * sensitivity + 0.0025f

      if (flux[i] > threshold && flux[i] >= flux[i - 1] && flux[i] >= flux[i + 1]) {
        val tMs = i * envelope.windowMs
        val last = markers.lastOrNull()
        if (last == null || tMs - last >= 200) {
          markers.add(tMs)
        }
      }
    }

    // 6. BPM from median inter-onset interval
    var bpm = 0
    if (markers.size >= 3) {
      val intervals = ArrayList<Int>(markers.size - 1)
      for (i in 1 until markers.size) intervals.add((markers[i] - markers[i - 1]).toInt())
      intervals.sort()
      val median = intervals[intervals.size / 2]
      if (median > 0) {
        var candidate = 60000 / median
        while (candidate < 60) candidate *= 2
        while (candidate > 180) candidate /= 2
        bpm = candidate
      }
    }

    val description = if (markers.isEmpty()) {
      "تم تحليل الصوت الحقيقي: لا توجد ضربات واضحة (مقطع محيطي/هادئ)"
    } else {
      "تم رصد ${markers.size} ضربة من تحليل PCM الفعلي بإيقاع تقريبي $bpm BPM"
    }

    return BeatDetectionResult(markers, bpm, markers.size, description)
  }

  /**
   * Splits video clips at REAL detected beat positions (same policy as before,
   * but now driven by measured audio transients instead of a made-up BPM grid).
   */
  fun splitClipsOnBeats(
    clips: List<com.lumina.studio.data.model.TimelineClip>,
    beatMarkersMs: List<Long>,
    maxSplits: Int = 6
  ): List<com.lumina.studio.data.model.TimelineClip> {
    if (beatMarkersMs.isEmpty()) return clips
    val result = clips.toMutableList()
    val splitPoints = beatMarkersMs.filterIndexed { index, _ -> index % 2 == 0 }.take(maxSplits)
    for (splitMs in splitPoints) {
      val targetIndex = result.indexOfFirst {
        it.trackType == com.lumina.studio.data.model.TrackType.VIDEO &&
          splitMs > it.startMs + 600L &&
          splitMs < it.startMs + it.durationMs - 600L
      }
      if (targetIndex != -1) {
        val orig = result[targetIndex]
        val dur1 = splitMs - orig.startMs
        val dur2 = orig.durationMs - dur1
        val c1 = orig.copy(durationMs = dur1)
        val c2 = orig.copy(
          id = "beat_cut_${splitMs}_${System.currentTimeMillis() % 100000}",
          title = "${orig.title} (مقطع $splitMs)",
          startMs = splitMs,
          durationMs = dur2
        )
        result[targetIndex] = c1
        result.add(targetIndex + 1, c2)
      }
    }
    return result
  }

  /** RMS → dBFS, for honest VU meter calibration. */
  fun rmsToDb(amplitude: Float): Float {
    val a = amplitude.coerceIn(1e-4f, 1f)
    return 20f * kotlin.math.log10(a)
  }

  /** Formats a 0..1 amplitude as an honest dB meter label. */
  fun formatDb(amplitude: Float): String {
    val db = rmsToDb(amplitude)
    return if (amplitude < 1e-3f) "-∞ dB" else String.format("%.1f dB", db)
  }

  /** Simple deterministic WAV (PCM16 mono 44.1kHz) writer used by SfxSynthesizer. */
  fun writeWavFile(target: java.io.File, pcm: ShortArray, sampleRate: Int) {
    val dataSize = pcm.size * 2
    java.io.DataOutputStream(
      java.io.BufferedOutputStream(java.io.FileOutputStream(target), 1 shl 16)
    ).use { out ->
      // RIFF header
      out.writeBytes("RIFF")
      out.writeIntLe(36 + dataSize)
      out.writeBytes("WAVE")
      out.writeBytes("fmt ")
      out.writeIntLe(16)
      out.writeShortLe(1) // WAVE format PCM
      out.writeShortLe(1) // mono
      out.writeIntLe(sampleRate)
      out.writeIntLe(sampleRate * 2) // byte rate
      out.writeShortLe(2) // block align
      out.writeShortLe(16) // bits per sample
      out.writeBytes("data")
      out.writeIntLe(dataSize)
      for (s in pcm) {
        out.writeShortLe(s.toInt() and 0xFFFF)
      }
      out.flush()
    }
  }

  private fun java.io.DataOutputStream.writeIntLe(v: Int) {
    write(v and 0xFF)
    write((v shr 8) and 0xFF)
    write((v shr 16) and 0xFF)
    write((v shr 24) and 0xFF)
  }

  private fun java.io.DataOutputStream.writeShortLe(v: Int) {
    write(v and 0xFF)
    write((v shr 8) and 0xFF)
  }
}
