package com.lumina.studio.engine

import com.lumina.studio.data.model.TimelineClip
import com.lumina.studio.data.model.TrackType
import java.util.Locale
import kotlin.math.roundToLong

/**
 * High-Precision SMPTE Frame-Accurate Timecode & Multi-Track Trimming Engine.
 * Supports standard NLE frame rates (24, 25, 30, 50, 60 fps).
 */
object FrameAccurateTimecodeEngine {

  /**
   * SMPTE Timecode representation with Hours, Minutes, Seconds, and Frames.
   */
  data class SmpteTimecode(
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
    val frames: Int,
    val totalFrames: Long,
    val totalMs: Long,
    val fps: Int
  ) {
    /**
     * Standard Broadcast SMPTE representation: HH:MM:SS:FF
     */
    fun toSmpteString(): String =
      String.format(Locale.US, "%02d:%02d:%02d:%02d", hours, minutes, seconds, frames)

    /**
     * Compact representation for mobile UI: MM:SS:FF
     */
    fun toCompactString(): String =
      String.format(Locale.US, "%02d:%02d:%02d", minutes, seconds, frames)

    /**
     * Frame label with total frames count: e.g. "01:24:12 (Frame #2532)"
     */
    fun toDetailedLabel(): String =
      String.format(Locale.US, "%02d:%02d:%02d [F#%d]", minutes, seconds, frames, totalFrames)
  }

  /**
   * Converts timestamp in milliseconds to SMPTE frame-accurate timecode.
   */
  fun msToTimecode(ms: Long, fps: Int = 30): SmpteTimecode {
    val clampedMs = ms.coerceAtLeast(0L)
    val safeFps = fps.coerceIn(1, 120)
    val frameDurationMs = 1000.0 / safeFps.toDouble()

    val totalFrames = (clampedMs / frameDurationMs).roundToLong()
    val frames = (totalFrames % safeFps).toInt()

    val totalSeconds = (clampedMs / 1000L)
    val seconds = (totalSeconds % 60L).toInt()

    val totalMinutes = totalSeconds / 60L
    val minutes = (totalMinutes % 60L).toInt()

    val hours = (totalMinutes / 60L).toInt()

    return SmpteTimecode(
      hours = hours,
      minutes = minutes,
      seconds = seconds,
      frames = frames,
      totalFrames = totalFrames,
      totalMs = clampedMs,
      fps = safeFps
    )
  }

  /**
   * Converts frame number to exact millisecond timestamp.
   */
  fun frameToMs(frame: Long, fps: Int = 30): Long {
    val safeFps = fps.coerceIn(1, 120)
    val frameDurationMs = 1000.0 / safeFps.toDouble()
    return (frame * frameDurationMs).roundToLong().coerceAtLeast(0L)
  }

  /**
   * Converts milliseconds to exact frame count.
   */
  fun msToFrame(ms: Long, fps: Int = 30): Long {
    val safeFps = fps.coerceIn(1, 120)
    val frameDurationMs = 1000.0 / safeFps.toDouble()
    return (ms.coerceAtLeast(0L) / frameDurationMs).roundToLong()
  }

  /**
   * Quantizes a millisecond timestamp to the nearest frame boundary.
   * Ensures frame-exact snapping without sub-frame drift.
   */
  fun quantizeToFrame(ms: Long, fps: Int = 30, maxMs: Long = Long.MAX_VALUE): Long {
    val frame = msToFrame(ms, fps)
    val snappedMs = frameToMs(frame, fps)
    return snappedMs.coerceIn(0L, maxMs)
  }

  /**
   * Nudges a timestamp by a discrete number of frames (e.g., +1 frame, -1 frame, +10 frames).
   */
  fun nudgeByFrames(currentMs: Long, deltaFrames: Int, fps: Int = 30, maxMs: Long = Long.MAX_VALUE): Long {
    val currentFrame = msToFrame(currentMs, fps)
    val newFrame = (currentFrame + deltaFrames).coerceAtLeast(0L)
    val newMs = frameToMs(newFrame, fps)
    return newMs.coerceIn(0L, maxMs)
  }

  /**
   * Result of multi-track sequence trim operations.
   */
  data class MultiTrackTrimResult(
    val updatedClips: List<TimelineClip>,
    val newTotalDurationMs: Long,
    val newPlayheadMs: Long,
    val newTrimStartMs: Long,
    val newTrimEndMs: Long,
    val summaryAr: String
  )

  /**
   * Extracts the range [startMs, endMs] across all tracks.
   * Everything before startMs and after endMs is removed, and the remaining clips
   * are shifted to start at 0ms. Total sequence duration becomes (endMs - startMs).
   */
  fun extractRangeAcrossTracks(
    clips: List<TimelineClip>,
    startMs: Long,
    endMs: Long,
    fps: Int = 30
  ): MultiTrackTrimResult {
    val qStart = quantizeToFrame(startMs, fps)
    val qEnd = quantizeToFrame(endMs, fps)
    val rangeDuration = (qEnd - qStart).coerceAtLeast(frameToMs(1, fps))

    val resultClips = mutableListOf<TimelineClip>()

    clips.forEach { clip ->
      val clipStart = clip.startMs
      val clipEnd = clip.startMs + clip.durationMs

      // Check overlap with [qStart, qEnd]
      val overlapStart = maxOf(clipStart, qStart)
      val overlapEnd = minOf(clipEnd, qEnd)

      if (overlapEnd > overlapStart) {
        // Shift overlap relative to new sequence start (0ms)
        val newClipStart = overlapStart - qStart
        val newClipDuration = overlapEnd - overlapStart
        resultClips.add(
          clip.copy(
            startMs = newClipStart,
            durationMs = newClipDuration
          )
        )
      }
    }

    val tcIn = msToTimecode(qStart, fps).toCompactString()
    val tcOut = msToTimecode(qEnd, fps).toCompactString()

    return MultiTrackTrimResult(
      updatedClips = resultClips,
      newTotalDurationMs = rangeDuration,
      newPlayheadMs = 0L,
      newTrimStartMs = 0L,
      newTrimEndMs = rangeDuration,
      summaryAr = "تم استخلاص المدى المحدد من $tcIn إلى $tcOut عبر كافة المسارات"
    )
  }

  /**
   * Ripple deletes the range [startMs, endMs] across all tracks.
   * Everything within [startMs, endMs] is cut, and following footage is shifted earlier
   * by the cut duration, eliminating any empty gap.
   */
  fun rippleDeleteRangeAcrossTracks(
    clips: List<TimelineClip>,
    startMs: Long,
    endMs: Long,
    totalDurationMs: Long,
    fps: Int = 30
  ): MultiTrackTrimResult {
    val qStart = quantizeToFrame(startMs, fps)
    val qEnd = quantizeToFrame(endMs, fps)
    val cutDuration = (qEnd - qStart).coerceAtLeast(0L)
    val newTotalDuration = (totalDurationMs - cutDuration).coerceAtLeast(frameToMs(1, fps))

    val resultClips = mutableListOf<TimelineClip>()

    clips.forEach { clip ->
      val clipStart = clip.startMs
      val clipEnd = clip.startMs + clip.durationMs

      when {
        // Case 1: Clip ends before cut start -> unchanged
        clipEnd <= qStart -> {
          resultClips.add(clip)
        }
        // Case 2: Clip starts after cut end -> shift earlier by cutDuration
        clipStart >= qEnd -> {
          resultClips.add(clip.copy(startMs = clipStart - cutDuration))
        }
        // Case 3: Clip straddles the cut region -> split or trim
        else -> {
          // Part before cut
          if (clipStart < qStart) {
            val preDuration = qStart - clipStart
            resultClips.add(clip.copy(durationMs = preDuration))
          }
          // Part after cut
          if (clipEnd > qEnd) {
            val postStart = qStart
            val postDuration = clipEnd - qEnd
            resultClips.add(
              clip.copy(
                id = "${clip.id}_post",
                startMs = postStart,
                durationMs = postDuration
              )
            )
          }
        }
      }
    }

    return MultiTrackTrimResult(
      updatedClips = resultClips,
      newTotalDurationMs = newTotalDuration,
      newPlayheadMs = qStart.coerceAtMost(newTotalDuration),
      newTrimStartMs = 0L,
      newTrimEndMs = newTotalDuration,
      summaryAr = "تم حذف المدى المحدد وسحب المسارات لغلق الفراغ (Ripple Delete)"
    )
  }

  /**
   * Trims a single targeted clip by adjusting its in/out point on its specific track.
   */
  fun trimSingleClip(
    clips: List<TimelineClip>,
    targetClipId: String,
    newStartMs: Long,
    newDurationMs: Long,
    fps: Int = 30
  ): List<TimelineClip> {
    val qStart = quantizeToFrame(newStartMs, fps)
    val qDur = quantizeToFrame(newDurationMs, fps).coerceAtLeast(frameToMs(1, fps))

    return clips.map { clip ->
      if (clip.id == targetClipId) {
        clip.copy(startMs = qStart, durationMs = qDur)
      } else {
        clip
      }
    }
  }
}
