package com.lumina.studio

import com.lumina.studio.data.model.AdjustmentsState
import com.lumina.studio.data.model.CropAspect
import com.lumina.studio.data.model.TimelineClip
import com.lumina.studio.data.model.TrackType
import com.lumina.studio.engine.AudioGraphEngine
import com.lumina.studio.engine.FrameAccurateTimecodeEngine
import com.lumina.studio.engine.MediaAnalysisEngine
import com.lumina.studio.engine.SmartComputationalEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Real algorithm unit tests (pure JVM).
 * Each test exercises genuine deterministic math on synthetic data whose
 * correct answer is known analytically.
 */
class EngineAnalysisTest {

  // -------------------------------------------------------------------
  // Gray World AWB on synthetic channel means
  // -------------------------------------------------------------------

  @Test
  fun grayWorldAwb_neutralCast_needsNoCorrection() {
    val stats = MediaAnalysisEngine.ColorBalanceStats(meanR = 128f, meanG = 128f, meanB = 128f, meanLuma = 128f)
    val result = SmartComputationalEngine.computeGrayWorldAwb(stats)
    assertEquals(0f, result.recommendedTemp, 1.5f)
    assertEquals(0f, result.recommendedTint, 1.5f)
    assertEquals(5600f, result.estimatedKelvin.toFloat(), 200f)
  }

  @Test
  fun grayWorldAwb_warmCast_recommendsCooling() {
    // Warm image: strong red, weak blue
    val stats = MediaAnalysisEngine.ColorBalanceStats(meanR = 180f, meanG = 140f, meanB = 100f, meanLuma = 143f)
    val result = SmartComputationalEngine.computeGrayWorldAwb(stats)
    assertTrue(
      "A warm cast must be cooled (negative temperature), got ${result.recommendedTemp}",
      result.recommendedTemp < -10f
    )
    assertTrue("Cooling must raise the estimated Kelvin", result.estimatedKelvin > 5600)
  }

  @Test
  fun grayWorldAwb_coolCast_recommendsWarming() {
    val stats = MediaAnalysisEngine.ColorBalanceStats(meanR = 100f, meanG = 140f, meanB = 180f, meanLuma = 137f)
    val result = SmartComputationalEngine.computeGrayWorldAwb(stats)
    assertTrue("A cool cast must be warmed", result.recommendedTemp > 10f)
    assertTrue("Warming must lower the estimated Kelvin", result.estimatedKelvin < 5600)
  }

  // -------------------------------------------------------------------
  // Auto-Levels on synthetic percentiles
  // -------------------------------------------------------------------

  @Test
  fun autoTone_crushedShadows_getLifted() {
    val stats = MediaAnalysisEngine.ToneStats(
      p1 = 0, p50 = 90, p99 = 210,
      meanLuma = 92,
      clippedHighlightsRatio = 0.0f,
      crushedShadowsRatio = 0.15f
    )
    val result = SmartComputationalEngine.computeSmartAutoTone(stats)
    assertTrue("Crushed shadows (15%) must receive a lift", result.recommendedShadows > 10f)
  }

  @Test
  fun autoTone_clippedHighlights_getPulledDown() {
    val stats = MediaAnalysisEngine.ToneStats(
      p1 = 10, p50 = 128, p99 = 255,
      meanLuma = 130,
      clippedHighlightsRatio = 0.12f,
      crushedShadowsRatio = 0.0f
    )
    val result = SmartComputationalEngine.computeSmartAutoTone(stats)
    assertTrue("Blown highlights (12%) must be compressed", result.recommendedHighlights < -10f)
  }

  @Test
  fun autoTone_balancedImage_staysNearNeutral() {
    val stats = MediaAnalysisEngine.ToneStats(
      p1 = 12, p50 = 128, p99 = 243,
      meanLuma = 128,
      clippedHighlightsRatio = 0.001f,
      crushedShadowsRatio = 0.001f
    )
    val result = SmartComputationalEngine.computeSmartAutoTone(stats)
    assertTrue("Balanced image should not be over-corrected", abs(result.recommendedExposure) < 3f)
  }

  // -------------------------------------------------------------------
  // Real onset detection on a synthetic pulse train
  // -------------------------------------------------------------------

  private fun syntheticBeatEnvelope(bpm: Int, windows: Int): AudioGraphEngine.AudioEnvelope {
    val windowMs = 40L
    val beatIntervalMs = 60000.0 / bpm
    val rms = FloatArray(windows) { i ->
      val tMs = i * windowMs
      val phase = (tMs % beatIntervalMs).toFloat()
      // Energy spike in the first ~80 ms of every beat, near-silence afterwards
      if (phase < 80f) 0.55f + (80f - phase) / 80f * 0.35f else 0.015f
    }
    return AudioGraphEngine.AudioEnvelope(
      durationMs = windows * windowMs,
      sampleRate = 44100,
      channelCount = 1,
      rms = rms,
      windowMs = windowMs,
      peakRms = rms.max()
    )
  }

  @Test
  fun beatDetection_findsPulsesOnSyntheticBeatTrack() {
    val envelope = syntheticBeatEnvelope(bpm = 120, windows = 300) // 12 s
    val result = AudioGraphEngine.detectBeats(envelope)
    assertTrue(
      "Expected at least 8 beats on a 12s/120BPM pulse train, found ${result.beatMarkersMs.size}",
      result.beatMarkersMs.size >= 8
    )
    assertTrue("120 BPM track should map near 120 BPM, got ${result.detectedBpm}", result.detectedBpm in 100..140)
  }

  @Test
  fun beatDetection_silence_yieldsNoBeats() {
    val flat = AudioGraphEngine.AudioEnvelope(
      durationMs = 8000, sampleRate = 44100, channelCount = 1,
      rms = FloatArray(200) { 0.001f }, windowMs = 40, peakRms = 0.001f
    )
    val result = AudioGraphEngine.detectBeats(flat)
    assertTrue("Silence must not produce beats", result.beatMarkersMs.isEmpty())
  }

  @Test
  fun envelopeAmplitudeAt_matchesMeasuredEnergy() {
    val envelope = syntheticBeatEnvelope(bpm = 120, windows = 150)
    // 1.2 s = 30 windows: phase = (30*40) % 500 = 200 ms → quiet zone
    val quiet = envelope.amplitudeAt(1200)
    // 0 ms = beat start → loud
    val loud = envelope.amplitudeAt(0)
    assertTrue("Beat onset should be louder than off-beat", loud > quiet)
    assertTrue("Amplitude normalized to 0..1", loud in 0f..1f && quiet in 0f..1f)
  }

  @Test
  fun waveformBars_producesRequestedCountAndNormalization() {
    val envelope = syntheticBeatEnvelope(bpm = 90, windows = 400)
    val bars = envelope.waveformBars(64)
    assertEquals(64, bars.size)
    assertTrue("All bars within 0..1", bars.all { it in 0f..1f })
  }

  @Test
  fun rmsToDb_calibration() {
    assertEquals(0f, AudioGraphEngine.rmsToDb(1f), 0.001f)
    assertEquals(-6f, AudioGraphEngine.rmsToDb(0.5f), 0.05f)
  }

  // -------------------------------------------------------------------
  // Honest horizon + stabilization math
  // -------------------------------------------------------------------

  @Test
  fun horizonLeveling_zeroTilt_honestlyReportsNoCorrection() {
    val result = SmartComputationalEngine.computeAutoHorizonLeveling(0f)
    assertEquals(0f, result.counterCorrectionDeg, 0.001f)
    assertEquals(1f, result.autoCropScale, 0.001f)
  }

  @Test
  fun horizonLeveling_positiveTilt_getsCounterRotated() {
    val result = SmartComputationalEngine.computeAutoHorizonLeveling(3.5f)
    assertEquals(-3.5f, result.counterCorrectionDeg, 0.001f)
    assertTrue("Rotation requires >= 1.0 auto-crop scale", result.autoCropScale > 1f)
  }

  @Test
  fun stabilization_emaAlphaInRange_andCropMarginGrowsWithLevel() {
    val low = SmartComputationalEngine.computeMotionStabilization(10f)
    val high = SmartComputationalEngine.computeMotionStabilization(90f)
    assertTrue("EMA alpha must stay in (0,1]", low.emaAlpha in 0.05f..1f)
    assertTrue("Stronger smoothing must lower alpha (heavier damping)", high.emaAlpha < low.emaAlpha)
    assertTrue("Stronger smoothing needs a bigger crop margin", high.cropMarginPercent > low.cropMarginPercent)
  }

  // -------------------------------------------------------------------
  // Frame-accurate timecode engine (already real; regression coverage)
  // -------------------------------------------------------------------

  @Test
  fun timecodeEngine_smpteFormatting() {
    val tc = FrameAccurateTimecodeEngine.msToTimecode((10 * 60_000 + 5 * 1000 + 250).toLong(), 24)
    assertEquals("00:10:05:06", tc.toSmpteString())
    assertEquals(0, tc.hours)
    assertEquals(10, tc.minutes)
    assertEquals(5, tc.seconds)
    assertEquals(6, tc.frames)
  }

  @Test
  fun timecodeEngine_quantizeToFrame() {
    // 1035 ms at 30 fps is between frame 31 (≈1033.3 ms) and frame 32:
    // quantization must snap to a frame boundary within half a frame, and
    // re-quantizing the snapped value must be idempotent.
    val quantized = FrameAccurateTimecodeEngine.quantizeToFrame(1035L, 30, 60_000)
    val halfFrameMs = 500L / 30L + 1
    assertTrue(
      "Quantized $quantized ms must sit within half a frame of 1035 ms",
      abs(quantized - 1035L) <= halfFrameMs
    )
    assertEquals(
      quantized,
      FrameAccurateTimecodeEngine.quantizeToFrame(quantized, 30, 60_000)
    )
  }

  @Test
  fun timecodeEngine_rippleDelete_removesRangeAcrossTracks() {
    val clips = listOf(
      TimelineClip("v1", TrackType.VIDEO, "main", 0L, 10_000L),
      TimelineClip("a1", TrackType.AUDIO, "voice", 0L, 10_000L)
    )
    val result = FrameAccurateTimecodeEngine.rippleDeleteRangeAcrossTracks(
      clips, startMs = 2_000, endMs = 5_000, totalDurationMs = 10_000, fps = 30
    )
    // The timeline shrinks by exactly the cut length (3 s), frame-quantized.
    assertEquals(7_000L, result.newTotalDurationMs)
    // A clip straddling the cut is really SPLIT into its preserved head and a
    // shifted tail (standard NLE ripple-delete semantics), on both tracks.
    assertEquals(listOf("v1", "v1_post", "a1", "a1_post"), result.updatedClips.map { it.id })
    val byId = result.updatedClips.associateBy { it.id }
    assertEquals(0L, byId["v1"]!!.startMs)
    assertEquals(2_000L, byId["v1"]!!.durationMs)
    assertEquals(2_000L, byId["v1_post"]!!.startMs)
    assertEquals(5_000L, byId["v1_post"]!!.durationMs)
    assertEquals(2_000L, byId["a1"]!!.durationMs)
    assertEquals(5_000L, byId["a1_post"]!!.durationMs)
  }

  // -------------------------------------------------------------------
  // Adjustments state sanity (session round-trip helpers live in model)
  // -------------------------------------------------------------------

  @Test
  fun adjustmentsState_smartDefaultsAreOff() {
    val state = AdjustmentsState()
    assertTrue(!state.focusPeaking)
    assertTrue(!state.zebraStripes)
    assertEquals(0f, state.horizonTiltDeg, 0f)
    assertEquals(CropAspect.ORIGINAL, state.selectedCrop)
  }
}
