package com.example

import com.example.data.model.AdjustmentsState
import com.example.data.model.TimelineClip
import com.example.data.model.TrackType
import com.example.engine.SmartComputationalEngine
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testGrayWorldAwbCalculation() {
    val state = AdjustmentsState(temperature = 25f, tint = -10f)
    val awbResult = SmartComputationalEngine.computeGrayWorldAwb(state)

    assertNotNull(awbResult)
    assertTrue("Estimated Kelvin should be within natural lighting spectrum", awbResult.estimatedKelvin in 2800..8500)
    assertTrue("AWB description should not be blank", awbResult.descriptionAr.isNotBlank())
    assertEquals(96, awbResult.confidencePercent)
  }

  @Test
  fun testSmartAutoToneMapping() {
    val state = AdjustmentsState()
    val toneResult = SmartComputationalEngine.computeSmartAutoTone(state)

    assertNotNull(toneResult)
    assertTrue("Dynamic range expansion should exceed 2.0 EV", toneResult.dynamicRangeExpansionEv >= 2.0f)
    assertTrue("Shadows should be elevated to recover details", toneResult.recommendedShadows > 0f)
    assertTrue("Highlights should be compressed to prevent blowout", toneResult.recommendedHighlights < 0f)
    assertTrue("Contrast should be boosted for cinematic pop", toneResult.recommendedContrast > 0f)
  }

  @Test
  fun testSmartSharpness() {
    val sharpness = SmartComputationalEngine.computeSmartSharpness()
    assertEquals(45f, sharpness, 0.1f)
  }

  @Test
  fun testAudioBeatDetection() {
    val durationMs = 30000L
    val bpm = 124
    val result = SmartComputationalEngine.detectAudioBeats(durationMs, bpm)

    assertNotNull(result)
    assertEquals(124, result.detectedBpm)
    assertTrue("Should detect multiple rhythm transients", result.transientPeakCount > 10)
    assertTrue("All beat markers must be within duration", result.beatMarkersMs.all { it in 1L until durationMs })
    assertTrue("Beat markers must be monotonically increasing", result.beatMarkersMs.zipWithNext().all { it.first < it.second })
  }

  @Test
  fun testAutoSplitOnBeats() {
    val durationMs = 30000L
    val originalClip = TimelineClip(
      id = "main_video",
      title = "فيديو أساسي",
      startMs = 0L,
      durationMs = durationMs,
      trackType = TrackType.VIDEO
    )
    val beats = listOf(4000L, 8000L, 12000L, 16000L)
    val splitClips = SmartComputationalEngine.autoSplitClipsOnBeats(listOf(originalClip), beats, maxSplits = 3)

    assertTrue("Clip list should have expanded after beat splitting", splitClips.size > 1)
    val totalCovered = splitClips.sumOf { it.durationMs }
    assertEquals("Total clip duration must be preserved across splits", durationMs, totalCovered)
  }

  @Test
  fun testMotionStabilizationCalculations() {
    val lowSmooth = SmartComputationalEngine.computeMotionStabilization(20f)
    val highSmooth = SmartComputationalEngine.computeMotionStabilization(90f)

    assertTrue("Higher smoothing should suppress more jitter", highSmooth.jitterReductionPercent > lowSmooth.jitterReductionPercent)
    assertTrue("Damping coefficient should stay within [0.1, 0.6]", highSmooth.dampingCoefficient in 0.1f..0.6f)
    assertTrue("Crop margin must be non-negative", highSmooth.cropMarginPercent > 0f)
  }

  @Test
  fun testAutoHorizonLeveling() {
    val horizonResult = SmartComputationalEngine.computeAutoHorizonLeveling(-2.8f)

    assertEquals("Counter correction must cancel out detected tilt", 2.8f, horizonResult.counterCorrectionDeg, 0.05f)
    assertTrue("Auto-crop scale factor must be >= 1.0 to eliminate black corners", horizonResult.autoCropScale >= 1.0f)
  }

  @Test
  fun testSpeedRampingPresets() {
    val presets = SmartComputationalEngine.availableSpeedRamps
    assertEquals(3, presets.size)
    assertTrue("All presets must have valid curve multipliers", presets.all { it.curvePoints.isNotEmpty() && it.curvePoints.all { p -> p > 0f } })
  }

  @Test
  fun testSmpteTimecodeCalculation() {
    // At 30 FPS, 1 frame is ~33.33ms. 1000ms = 30 frames = 1 second 0 frames.
    val tc30 = com.example.engine.FrameAccurateTimecodeEngine.msToTimecode(1000L, fps = 30)
    assertEquals(0, tc30.minutes)
    assertEquals(1, tc30.seconds)
    assertEquals(0, tc30.frames)
    assertEquals(30L, tc30.totalFrames)
    assertEquals("00:01:00", tc30.toCompactString())

    // 1500ms at 30 fps = 1 second 15 frames
    val tcHalf = com.example.engine.FrameAccurateTimecodeEngine.msToTimecode(1500L, fps = 30)
    assertEquals(1, tcHalf.seconds)
    assertEquals(15, tcHalf.frames)
    assertEquals(45L, tcHalf.totalFrames)
    assertEquals("00:01:15", tcHalf.toCompactString())

    // 60 FPS
    val tc60 = com.example.engine.FrameAccurateTimecodeEngine.msToTimecode(500L, fps = 60)
    assertEquals(0, tc60.seconds)
    assertEquals(30, tc60.frames)
    assertEquals(30L, tc60.totalFrames)
  }

  @Test
  fun testFrameQuantizationAndNudge() {
    val fps = 30
    // Test quantization
    val snapped = com.example.engine.FrameAccurateTimecodeEngine.quantizeToFrame(1015L, fps = fps)
    val frameNum = com.example.engine.FrameAccurateTimecodeEngine.msToFrame(snapped, fps = fps)
    assertEquals(30L, frameNum)

    // Test nudge +1 frame
    val nudgedPlus1 = com.example.engine.FrameAccurateTimecodeEngine.nudgeByFrames(snapped, deltaFrames = 1, fps = fps)
    val framePlus1 = com.example.engine.FrameAccurateTimecodeEngine.msToFrame(nudgedPlus1, fps = fps)
    assertEquals(31L, framePlus1)

    // Test nudge -10 frames
    val nudgedMinus10 = com.example.engine.FrameAccurateTimecodeEngine.nudgeByFrames(snapped, deltaFrames = -10, fps = fps)
    val frameMinus10 = com.example.engine.FrameAccurateTimecodeEngine.msToFrame(nudgedMinus10, fps = fps)
    assertEquals(20L, frameMinus10)
  }

  @Test
  fun testMultiTrackExtractRange() {
    val clips = listOf(
      com.example.data.model.TimelineClip(
        id = "v1",
        trackType = com.example.data.model.TrackType.VIDEO,
        title = "Clip 1",
        startMs = 0L,
        durationMs = 10000L
      ),
      com.example.data.model.TimelineClip(
        id = "a1",
        trackType = com.example.data.model.TrackType.AUDIO,
        title = "Audio Track",
        startMs = 0L,
        durationMs = 10000L
      )
    )

    // Extract range 2000ms to 6000ms (duration 4000ms)
    val result = com.example.engine.FrameAccurateTimecodeEngine.extractRangeAcrossTracks(
      clips = clips,
      startMs = 2000L,
      endMs = 6000L,
      fps = 30
    )

    assertEquals(4000L, result.newTotalDurationMs)
    assertEquals(2, result.updatedClips.size)
    // All extracted clips should start at 0ms and have duration 4000ms
    assertEquals(0L, result.updatedClips[0].startMs)
    assertEquals(4000L, result.updatedClips[0].durationMs)
  }

  @Test
  fun testMultiTrackRippleDelete() {
    val clips = listOf(
      com.example.data.model.TimelineClip(
        id = "c1",
        trackType = com.example.data.model.TrackType.VIDEO,
        title = "First Clip",
        startMs = 0L,
        durationMs = 2000L
      ),
      com.example.data.model.TimelineClip(
        id = "c2",
        trackType = com.example.data.model.TrackType.VIDEO,
        title = "Second Clip",
        startMs = 4000L,
        durationMs = 3000L
      )
    )

    // Ripple delete range 2000ms to 4000ms (the 2000ms gap)
    val result = com.example.engine.FrameAccurateTimecodeEngine.rippleDeleteRangeAcrossTracks(
      clips = clips,
      startMs = 2000L,
      endMs = 4000L,
      totalDurationMs = 7000L,
      fps = 30
    )

    assertEquals(5000L, result.newTotalDurationMs)
    // First clip unchanged
    assertEquals(0L, result.updatedClips[0].startMs)
    assertEquals(2000L, result.updatedClips[0].durationMs)
    // Second clip pulled earlier by 2000ms: 4000L - 2000L = 2000L
    assertEquals(2000L, result.updatedClips[1].startMs)
    assertEquals(3000L, result.updatedClips[1].durationMs)
  }
}

