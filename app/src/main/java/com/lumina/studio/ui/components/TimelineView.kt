package com.lumina.studio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumina.studio.data.model.FilterPreset
import com.lumina.studio.data.model.TimelineClip
import com.lumina.studio.data.model.TrackType
import com.lumina.studio.engine.analysis.FrameAccurateTimecodeEngine
import com.lumina.studio.ui.theme.AudioTrackColor
import com.lumina.studio.ui.theme.CyberGold
import com.lumina.studio.ui.theme.EffectTrackColor
import com.lumina.studio.ui.theme.ElectricCyan
import com.lumina.studio.ui.theme.EmeraldGreen
import com.lumina.studio.ui.theme.NeonViolet
import com.lumina.studio.ui.theme.ObsidianBg
import com.lumina.studio.ui.theme.ObsidianBorder
import com.lumina.studio.ui.theme.ObsidianBorderActive
import com.lumina.studio.ui.theme.ObsidianSurface
import com.lumina.studio.ui.theme.ObsidianSurfaceContainer
import com.lumina.studio.ui.theme.ObsidianSurfaceElevated
import com.lumina.studio.ui.theme.SunsetCoral
import com.lumina.studio.ui.theme.TextMuted
import com.lumina.studio.ui.theme.TextPrimary
import com.lumina.studio.ui.theme.TextSecondary
import com.lumina.studio.ui.theme.TextTrackColor
import com.lumina.studio.ui.theme.VideoTrackColor
import java.util.Locale
import kotlin.math.roundToInt

/**
 * High-Precision Multi-Track Timeline with Frame-Accurate Video Trimming.
 * Allows interactive dragging of In/Out points across all tracks, SMPTE timecode
 * display (HH:MM:SS:FF), single-frame and 10-frame micro-jogging, frame snapping,
 * and sequence extraction/ripple deletion.
 */
@Composable
fun TimelineView(
  isPlaying: Boolean,
  playheadMs: Long,
  durationMs: Long,
  timelineClips: List<TimelineClip>,
  isTrimmingActive: Boolean = false,
  trimStartMs: Long = 0L,
  trimEndMs: Long = durationMs,
  timelineZoom: Float = 1.0f,
  fps: Int = 30,
  snapToFrames: Boolean = true,
  onZoomChange: (Float) -> Unit = {},
  beatMarkers: List<Long> = emptyList(),
  audioWaveform: FloatArray? = null,
  audioClipCount: Int = 0,
  onTogglePlay: () -> Unit,
  onSeekTo: (Long) -> Unit,
  onStepFrame: (Boolean) -> Unit,
  onSplitClip: () -> Unit,
  onToggleTrimMode: () -> Unit = {},
  onTrimChange: (startMs: Long, endMs: Long) -> Unit = { _, _ -> },
  onNudgeStartFrame: (deltaFrames: Int) -> Unit = {},
  onNudgeEndFrame: (deltaFrames: Int) -> Unit = {},
  onSetStartToPlayhead: () -> Unit = {},
  onSetEndToPlayhead: () -> Unit = {},
  onPreviewStartFrame: () -> Unit = {},
  onPreviewEndFrame: () -> Unit = {},
  onApplyTrimExtract: () -> Unit = {},
  onApplyRippleDelete: () -> Unit = {},
  onResetTrim: () -> Unit = {},
  onToggleSnapToFrames: () -> Unit = {},
  onFpsChange: (Int) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val safeDuration = durationMs.coerceAtLeast(1000L)
  val safeStart = trimStartMs.coerceIn(0L, safeDuration - FrameAccurateTimecodeEngine.frameToMs(2, fps))
  val safeEnd = trimEndMs.coerceIn(safeStart + FrameAccurateTimecodeEngine.frameToMs(2, fps), safeDuration)
  val activeDuration = (safeEnd - safeStart).coerceAtLeast(FrameAccurateTimecodeEngine.frameToMs(1, fps))
  val scrollState = rememberScrollState()

  // Local drag state for interactive tooltips
  var isDraggingStartHandle by remember { mutableStateOf(false) }
  var isDraggingEndHandle by remember { mutableStateOf(false) }

  val playheadTc = FrameAccurateTimecodeEngine.msToTimecode(playheadMs, fps)
  val totalTc = FrameAccurateTimecodeEngine.msToTimecode(safeDuration, fps)
  val inTc = FrameAccurateTimecodeEngine.msToTimecode(safeStart, fps)
  val outTc = FrameAccurateTimecodeEngine.msToTimecode(safeEnd, fps)
  val activeTc = FrameAccurateTimecodeEngine.msToTimecode(activeDuration, fps)

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(ObsidianSurface)
      .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
      .padding(8.dp)
      .testTag("timeline_view_panel")
  ) {
    // 1. Top Bar: Split, Trim Mode Toggle, Playback Controls, SMPTE Timecode
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Action Buttons
      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Split Button
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ObsidianSurfaceElevated)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
            .clickable { onSplitClip() }
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .testTag("split_clip_button")
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.ContentCut,
              contentDescription = "قص المقطع عند المؤشر",
              tint = ElectricCyan,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "قص / Split",
              color = TextPrimary,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        // Trim In-Out Mode Toggle
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isTrimmingActive) CyberGold.copy(alpha = 0.2f) else ObsidianSurfaceElevated)
            .border(
              1.dp,
              if (isTrimmingActive) CyberGold else ObsidianBorder,
              RoundedCornerShape(8.dp)
            )
            .clickable { onToggleTrimMode() }
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .testTag("toggle_trim_mode_button")
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.ContentCut,
              contentDescription = "أداة قص الأطراف الإطارية الدقيقة",
              tint = if (isTrimmingActive) CyberGold else TextSecondary,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = if (isTrimmingActive) "قص إطاري نشط" else "قص الأطراف Trim",
              color = if (isTrimmingActive) CyberGold else TextPrimary,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      // Transport Playback Controls
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        IconButton(
          onClick = { onStepFrame(false) },
          modifier = Modifier
            .size(30.dp)
            .testTag("step_backward_button")
        ) {
          Icon(
            imageVector = Icons.Default.FastRewind,
            contentDescription = "إطار سابق",
            tint = TextSecondary,
            modifier = Modifier.size(16.dp)
          )
        }

        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (isPlaying) SunsetCoral else ElectricCyan)
            .clickable { onTogglePlay() }
            .testTag("play_pause_button"),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
            tint = Color.Black,
            modifier = Modifier.size(20.dp)
          )
        }

        IconButton(
          onClick = { onStepFrame(true) },
          modifier = Modifier
            .size(30.dp)
            .testTag("step_forward_button")
        ) {
          Icon(
            imageVector = Icons.Default.FastForward,
            contentDescription = "إطار تالي",
            tint = TextSecondary,
            modifier = Modifier.size(16.dp)
          )
        }
      }

      // SMPTE Timecode Display with Frame Counter
      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = "${playheadTc.toCompactString()} / ${totalTc.toCompactString()}",
          color = CyberGold,
          fontSize = 11.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.testTag("timeline_timecode_readout")
        )
        Text(
          text = "إطار #${playheadTc.totalFrames} • $fps FPS",
          color = TextMuted,
          fontSize = 8.5.sp,
          fontFamily = FontFamily.Monospace
        )
      }
    }

    // 2. Expandable Frame-Accurate Trimming Deck (Visible when isTrimmingActive is true)
    AnimatedVisibility(
      visible = isTrimmingActive,
      enter = fadeIn() + expandVertically(),
      exit = fadeOut() + shrinkVertically()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 6.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(ObsidianSurfaceElevated)
          .border(1.dp, CyberGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
          .padding(8.dp)
          .testTag("frame_accurate_trimming_deck")
      ) {
        // Deck Top Bar: Status, Snapping & FPS controls
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "لوحة القص الإطاري الدقيق للمسارات",
              color = CyberGold,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }

          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Snap to frames toggle chip
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (snapToFrames) EmeraldGreen.copy(alpha = 0.15f) else ObsidianBg)
                .border(
                  1.dp,
                  if (snapToFrames) EmeraldGreen else ObsidianBorder,
                  RoundedCornerShape(4.dp)
                )
                .clickable { onToggleSnapToFrames() }
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .testTag("toggle_snap_frames_button")
            ) {
              Text(
                text = if (snapToFrames) "محاذاة للإطارات ✓" else "محاذاة حرة",
                color = if (snapToFrames) EmeraldGreen else TextMuted,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold
              )
            }

            // FPS Selection Chip (30 FPS / 60 FPS)
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(ElectricCyan.copy(alpha = 0.15f))
                .border(1.dp, ElectricCyan.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .clickable { onFpsChange(if (fps == 30) 60 else 30) }
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .testTag("toggle_fps_rate_button")
            ) {
              Text(
                text = "$fps FPS (${String.format(Locale.US, "%.1f", 1000f / fps)}ms)",
                color = ElectricCyan,
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // In & Out Precision Control Cards
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // Card 1: Start Point (IN)
          Column(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(6.dp))
              .background(ObsidianSurfaceContainer)
              .border(1.dp, ObsidianBorderActive, RoundedCornerShape(6.dp))
              .padding(6.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("البداية IN", color = CyberGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
              Text(
                text = "F#${inTc.totalFrames}",
                color = TextSecondary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
              )
            }

            Text(
              text = inTc.toCompactString(),
              color = TextPrimary,
              fontSize = 13.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(vertical = 2.dp)
            )

            // Mark IN & Preview IN Row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              // Mark IN at Playhead
              Box(
                modifier = Modifier
                  .weight(1.3f)
                  .height(26.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(CyberGold.copy(alpha = 0.2f))
                  .border(1.dp, CyberGold.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                  .clickable { onSetStartToPlayhead() }
                  .testTag("mark_in_at_playhead_button"),
                contentAlignment = Alignment.Center
              ) {
                Text("[ تحديد IN", color = CyberGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
              }

              // Preview IN Frame
              Box(
                modifier = Modifier
                  .weight(0.9f)
                  .height(26.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(ObsidianBg)
                  .border(1.dp, ObsidianBorder, RoundedCornerShape(4.dp))
                  .clickable { onPreviewStartFrame() }
                  .testTag("preview_in_frame_button"),
                contentAlignment = Alignment.Center
              ) {
                Text("معاينة", color = TextSecondary, fontSize = 8.5.sp)
              }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Frame Micro-Jog Row for IN (-10F, -1F, +1F, +10F)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
              listOf(-10 to "-10F", -1 to "◄ 1F", 1 to "1F ►", 10 to "+10F").forEach { (delta, label) ->
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .height(22.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(ObsidianBg)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(3.dp))
                    .clickable { onNudgeStartFrame(delta) }
                    .testTag("nudge_in_${if (delta < 0) "back" else "fwd"}_${kotlin.math.abs(delta)}"),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = label,
                    color = TextPrimary,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }

          // Card 2: End Point (OUT)
          Column(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(6.dp))
              .background(ObsidianSurfaceContainer)
              .border(1.dp, ObsidianBorderActive, RoundedCornerShape(6.dp))
              .padding(6.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("النهاية OUT", color = CyberGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
              Text(
                text = "F#${outTc.totalFrames}",
                color = TextSecondary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
              )
            }

            Text(
              text = outTc.toCompactString(),
              color = TextPrimary,
              fontSize = 13.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(vertical = 2.dp)
            )

            // Mark OUT & Preview OUT Row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              // Mark OUT at Playhead
              Box(
                modifier = Modifier
                  .weight(1.3f)
                  .height(26.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(CyberGold.copy(alpha = 0.2f))
                  .border(1.dp, CyberGold.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                  .clickable { onSetEndToPlayhead() }
                  .testTag("mark_out_at_playhead_button"),
                contentAlignment = Alignment.Center
              ) {
                Text("تحديد OUT ]", color = CyberGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
              }

              // Preview OUT Frame
              Box(
                modifier = Modifier
                  .weight(0.9f)
                  .height(26.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(ObsidianBg)
                  .border(1.dp, ObsidianBorder, RoundedCornerShape(4.dp))
                  .clickable { onPreviewEndFrame() }
                  .testTag("preview_out_frame_button"),
                contentAlignment = Alignment.Center
              ) {
                Text("معاينة", color = TextSecondary, fontSize = 8.5.sp)
              }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Frame Micro-Jog Row for OUT (-10F, -1F, +1F, +10F)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
              listOf(-10 to "-10F", -1 to "◄ 1F", 1 to "1F ►", 10 to "+10F").forEach { (delta, label) ->
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .height(22.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(ObsidianBg)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(3.dp))
                    .clickable { onNudgeEndFrame(delta) }
                    .testTag("nudge_out_${if (delta < 0) "back" else "fwd"}_${kotlin.math.abs(delta)}"),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = label,
                    color = TextPrimary,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Sequence Trimming Actions & Duration Summary
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Duration & Frame Count Badge
          Column {
            Text(
              text = "المدة المحددة: ${activeTc.toCompactString()} (${activeTc.totalFrames} إطار)",
              color = CyberGold,
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold
            )
            val cutDurationSec = (safeDuration - activeDuration).toFloat() / 1000f
            if (cutDurationSec > 0.05f) {
              Text(
                text = "-${String.format(Locale.US, "%.2f", cutDurationSec)} ثانية سيتم حذفها",
                color = SunsetCoral,
                fontSize = 8.5.sp
              )
            }
          }

          // Action Buttons: Extract, Ripple Delete, Reset
          Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Reset Button
            Box(
              modifier = Modifier
                .height(28.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(ObsidianBg)
                .border(1.dp, ObsidianBorder, RoundedCornerShape(5.dp))
                .clickable { onResetTrim() }
                .padding(horizontal = 6.dp)
                .testTag("reset_trim_range_button"),
              contentAlignment = Alignment.Center
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.RestartAlt,
                  contentDescription = "استعادة المدى الكامل",
                  tint = TextSecondary,
                  modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text("إعادة ضبط", color = TextSecondary, fontSize = 8.5.sp)
              }
            }

            // Ripple Delete (Cut In/Out and close gap)
            Box(
              modifier = Modifier
                .height(28.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(SunsetCoral.copy(alpha = 0.15f))
                .border(1.dp, SunsetCoral.copy(alpha = 0.7f), RoundedCornerShape(5.dp))
                .clickable { onApplyRippleDelete() }
                .padding(horizontal = 7.dp)
                .testTag("apply_ripple_delete_button"),
              contentAlignment = Alignment.Center
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.ContentCut,
                  contentDescription = "حذف المدى مع سحب المسارات",
                  tint = SunsetCoral,
                  modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = "حذف وحزم (Ripple)",
                  color = SunsetCoral,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            // Apply Extract (Keep Selection across all tracks)
            Box(
              modifier = Modifier
                .height(28.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(CyberGold)
                .clickable { onApplyTrimExtract() }
                .padding(horizontal = 8.dp)
                .testTag("apply_trim_extract_button"),
              contentAlignment = Alignment.Center
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = "حفظ المدى المحدد وقص الباقي",
                  tint = Color.Black,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = "استخلاص المدى",
                  color = Color.Black,
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // 3. Zoom Slider Controls (1.0x - 10.0x for sub-frame & single-frame inspection)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(ObsidianSurfaceElevated)
        .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
        .padding(horizontal = 8.dp, vertical = 4.dp)
        .testTag("timeline_zoom_control_panel")
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = { onZoomChange((timelineZoom - 0.5f).coerceAtLeast(1.0f)) },
          modifier = Modifier
            .size(24.dp)
            .testTag("timeline_zoom_out_button")
        ) {
          Icon(
            imageVector = Icons.Default.ZoomOut,
            contentDescription = "تصغير المخطط الزمني",
            tint = if (timelineZoom > 1.0f) ElectricCyan else TextMuted,
            modifier = Modifier.size(15.dp)
          )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Slider(
          value = timelineZoom,
          onValueChange = { onZoomChange(it) },
          valueRange = 1.0f..10.0f,
          modifier = Modifier
            .weight(1f)
            .height(24.dp)
            .testTag("timeline_zoom_slider"),
          colors = SliderDefaults.colors(
            thumbColor = CyberGold,
            activeTrackColor = ElectricCyan,
            inactiveTrackColor = ObsidianBorder
          )
        )

        Spacer(modifier = Modifier.width(4.dp))

        IconButton(
          onClick = { onZoomChange((timelineZoom + 0.5f).coerceAtMost(10.0f)) },
          modifier = Modifier
            .size(24.dp)
            .testTag("timeline_zoom_in_button")
        ) {
          Icon(
            imageVector = Icons.Default.ZoomIn,
            contentDescription = "تكبير المخطط الزمني",
            tint = if (timelineZoom < 10.0f) ElectricCyan else TextMuted,
            modifier = Modifier.size(15.dp)
          )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(ObsidianBg)
            .border(
              1.dp,
              if (timelineZoom > 1.0f) ElectricCyan.copy(alpha = 0.6f) else ObsidianBorder,
              RoundedCornerShape(4.dp)
            )
            .clickable { onZoomChange(1.0f) }
            .padding(horizontal = 5.dp, vertical = 2.dp)
            .testTag("timeline_zoom_value_badge")
        ) {
          Text(
            text = String.format(Locale.US, "%.1fx FIT", timelineZoom),
            color = if (timelineZoom > 1.0f) CyberGold else TextSecondary,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // 4. Multi-Track Canvas Area with Interactive Draggable Trimming Handles
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxWidth()
        .height(140.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(ObsidianBg)
        .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
        .testTag("multi_track_scrubber_canvas")
    ) {
      val density = LocalDensity.current
      val containerWidthPx = constraints.maxWidth.toFloat()
      val zoomedWidthPx = containerWidthPx * timelineZoom
      val zoomedWidthDp = maxWidth * timelineZoom
      val minGapPx = with(density) { 32.dp.toPx() }

      // Auto-follow playhead during playback
      LaunchedEffect(playheadMs, timelineZoom, isPlaying) {
        if (timelineZoom > 1.0f) {
          val playheadXPx = ((playheadMs.toFloat() / safeDuration.toFloat()) * zoomedWidthPx)
          val viewportWidth = containerWidthPx
          val targetScroll = (playheadXPx - viewportWidth / 2f).coerceIn(
            0f,
            (zoomedWidthPx - viewportWidth).coerceAtLeast(0f)
          )
          if (isPlaying) {
            scrollState.scrollTo(targetScroll.roundToInt())
          }
        }
      }

      Box(
        modifier = Modifier
          .fillMaxSize()
          .horizontalScroll(scrollState)
      ) {
        Box(
          modifier = Modifier
            .width(zoomedWidthDp)
            .fillMaxHeight()
            .pointerInput(safeDuration, zoomedWidthPx) {
              detectTapGestures { offset ->
                val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                val targetMs = (fraction * safeDuration).toLong()
                val snapped = if (snapToFrames) FrameAccurateTimecodeEngine.quantizeToFrame(targetMs, fps, safeDuration) else targetMs
                onSeekTo(snapped)
              }
            }
        ) {
          val playheadX = ((playheadMs.toFloat() / safeDuration.toFloat()) * zoomedWidthPx)
            .coerceIn(0f, zoomedWidthPx)

          val trimStartXPx = ((safeStart.toFloat() / safeDuration.toFloat()) * zoomedWidthPx)
            .coerceIn(0f, zoomedWidthPx)
          val trimEndXPx = ((safeEnd.toFloat() / safeDuration.toFloat()) * zoomedWidthPx)
            .coerceIn(0f, zoomedWidthPx)

          // 4.1. Tracks Column (Ruler + 4 Media Tracks)
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
          ) {
            // Track 0: SMPTE Frame Ruler
            TimelineRulerLane(
              totalDurationMs = safeDuration,
              timelineZoom = timelineZoom,
              fps = fps,
              beatMarkers = beatMarkers,
              trimStartMs = safeStart,
              trimEndMs = safeEnd,
              isTrimmingActive = isTrimmingActive
            )

            // Track 1: 4K Video Track
            TrackLane(
              title = "4K VIDEO TRACK",
              color = VideoTrackColor,
              clips = timelineClips.filter { it.trackType == TrackType.VIDEO },
              totalDurationMs = safeDuration,
              laneWidthDp = zoomedWidthDp
            )

            // Track 2: REAL Audio Waveform Track
            AudioWaveformLane(
              title = if (audioClipCount > 0) "AUDIO • ${audioClipCount} مقطع" else "AUDIO (بلا مقاطع)",
              totalDurationMs = safeDuration,
              waveform = audioWaveform
            )

            // Track 3: Overlay / Text Track
            TrackLane(
              title = "TITLE & OVERLAY",
              color = TextTrackColor,
              clips = timelineClips.filter { it.trackType == TrackType.OVERLAY },
              totalDurationMs = safeDuration,
              laneWidthDp = zoomedWidthDp
            )

            // Track 4: Visual FX / Color Grade Track
            TrackLane(
              title = "COLOR GRADE & FX",
              color = EffectTrackColor,
              clips = timelineClips.filter { it.trackType == TrackType.FX },
              totalDurationMs = safeDuration,
              laneWidthDp = zoomedWidthDp
            )
          }

          // 4.2. Trim Scrim & Boundary Overlays across all tracks
          Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Excluded region before IN-point (Dimmed layer with zebra)
            if (trimStartXPx > 0f) {
              drawRect(
                color = Color.Black.copy(alpha = 0.65f),
                topLeft = Offset(0f, 0f),
                size = Size(trimStartXPx, h)
              )
            }

            // Excluded region after OUT-point (Dimmed layer)
            if (trimEndXPx < w) {
              drawRect(
                color = Color.Black.copy(alpha = 0.65f),
                topLeft = Offset(trimEndXPx, 0f),
                size = Size(w - trimEndXPx, h)
              )
            }

            // Active region highlight bars (Top & Bottom boundaries)
            if (isTrimmingActive || safeStart > 0L || safeEnd < safeDuration) {
              drawRect(
                color = CyberGold,
                topLeft = Offset(trimStartXPx, 0f),
                size = Size(trimEndXPx - trimStartXPx, 2.5.dp.toPx())
              )
              drawRect(
                color = CyberGold,
                topLeft = Offset(trimStartXPx, h - 2.5.dp.toPx()),
                size = Size(trimEndXPx - trimStartXPx, 2.5.dp.toPx())
              )
            }
          }

          // 4.3. Interactive DRAGGABLE START (IN) HANDLE across all tracks
          val handleWidthDp = 28.dp
          val handleWidthPx = with(density) { handleWidthDp.toPx() }

          Box(
            modifier = Modifier
              .offset { IntOffset((trimStartXPx - handleWidthPx / 2f).roundToInt(), 0) }
              .width(handleWidthDp)
              .fillMaxHeight()
              .pointerInput(safeDuration, zoomedWidthPx, safeEnd, fps, snapToFrames) {
                detectDragGestures(
                  onDragStart = { isDraggingStartHandle = true },
                  onDragEnd = { isDraggingStartHandle = false },
                  onDragCancel = { isDraggingStartHandle = false },
                  onDrag = { change, dragAmount ->
                    change.consume()
                    val currentX = (safeStart.toFloat() / safeDuration.toFloat()) * zoomedWidthPx
                    val newX = (currentX + dragAmount.x).coerceIn(0f, trimEndXPx - minGapPx)
                    val rawStart = ((newX / zoomedWidthPx) * safeDuration).toLong()
                    val snappedStart = if (snapToFrames) {
                      FrameAccurateTimecodeEngine.quantizeToFrame(rawStart, fps, safeEnd)
                    } else {
                      rawStart
                    }
                    onTrimChange(snappedStart, safeEnd)
                    onSeekTo(snappedStart) // Immediate preview of the exact frame
                  }
                )
              }
              .testTag("timeline_draggable_handle_in"),
            contentAlignment = Alignment.Center
          ) {
            // Visual Bracket & Guideline
            Canvas(modifier = Modifier.fillMaxSize()) {
              val midX = size.width / 2f
              val h = size.height

              // Vertical Laser Guide Line through all tracks
              drawLine(
                color = CyberGold,
                start = Offset(midX, 0f),
                end = Offset(midX, h),
                strokeWidth = 2.5.dp.toPx()
              )

              // Top horizontal bracket lip pointing right: [
              drawLine(
                color = CyberGold,
                start = Offset(midX, 1.dp.toPx()),
                end = Offset(midX + 8.dp.toPx(), 1.dp.toPx()),
                strokeWidth = 3.dp.toPx()
              )

              // Bottom horizontal bracket lip pointing right
              drawLine(
                color = CyberGold,
                start = Offset(midX, h - 1.dp.toPx()),
                end = Offset(midX + 8.dp.toPx(), h - 1.dp.toPx()),
                strokeWidth = 3.dp.toPx()
              )
            }

            // Top Flag Cap "[ IN"
            Box(
              modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 1.dp)
                .clip(RoundedCornerShape(topStart = 4.dp, bottomEnd = 4.dp))
                .background(CyberGold)
                .padding(horizontal = 3.dp, vertical = 1.dp)
            ) {
              Text(
                text = "[ IN",
                color = Color.Black,
                fontSize = 7.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
              )
            }

            // Center tactile grip handle
            Box(
              modifier = Modifier
                .size(width = 10.dp, height = 28.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(CyberGold)
                .border(1.dp, Color.Black.copy(alpha = 0.5f), RoundedCornerShape(3.dp)),
              contentAlignment = Alignment.Center
            ) {
              Column(
                verticalArrangement = Arrangement.spacedBy(2.5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                repeat(3) {
                  Box(
                    modifier = Modifier
                      .size(width = 5.dp, height = 1.5.dp)
                      .background(Color.Black.copy(alpha = 0.8f))
                  )
                }
              }
            }

            // Floating Tooltip on Drag
            if (isDraggingStartHandle) {
              Box(
                modifier = Modifier
                  .align(Alignment.TopCenter)
                  .offset(y = (-22).dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(Color.Black)
                  .border(1.dp, CyberGold, RoundedCornerShape(4.dp))
                  .padding(horizontal = 4.dp, vertical = 2.dp)
              ) {
                Text(
                  text = "IN: ${inTc.toCompactString()} [F#${inTc.totalFrames}]",
                  color = CyberGold,
                  fontSize = 8.sp,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }

          // 4.4. Interactive DRAGGABLE END (OUT) HANDLE across all tracks
          Box(
            modifier = Modifier
              .offset { IntOffset((trimEndXPx - handleWidthPx / 2f).roundToInt(), 0) }
              .width(handleWidthDp)
              .fillMaxHeight()
              .pointerInput(safeDuration, zoomedWidthPx, safeStart, fps, snapToFrames) {
                detectDragGestures(
                  onDragStart = { isDraggingEndHandle = true },
                  onDragEnd = { isDraggingEndHandle = false },
                  onDragCancel = { isDraggingEndHandle = false },
                  onDrag = { change, dragAmount ->
                    change.consume()
                    val currentX = (safeEnd.toFloat() / safeDuration.toFloat()) * zoomedWidthPx
                    val newX = (currentX + dragAmount.x).coerceIn(trimStartXPx + minGapPx, zoomedWidthPx)
                    val rawEnd = ((newX / zoomedWidthPx) * safeDuration).toLong()
                    val snappedEnd = if (snapToFrames) {
                      FrameAccurateTimecodeEngine.quantizeToFrame(rawEnd, fps, safeDuration)
                    } else {
                      rawEnd
                    }
                    onTrimChange(safeStart, snappedEnd)
                    onSeekTo(snappedEnd) // Immediate preview of the exact frame
                  }
                )
              }
              .testTag("timeline_draggable_handle_out"),
            contentAlignment = Alignment.Center
          ) {
            // Visual Bracket & Guideline
            Canvas(modifier = Modifier.fillMaxSize()) {
              val midX = size.width / 2f
              val h = size.height

              // Vertical Laser Guide Line through all tracks
              drawLine(
                color = CyberGold,
                start = Offset(midX, 0f),
                end = Offset(midX, h),
                strokeWidth = 2.5.dp.toPx()
              )

              // Top horizontal bracket lip pointing left: ]
              drawLine(
                color = CyberGold,
                start = Offset(midX - 8.dp.toPx(), 1.dp.toPx()),
                end = Offset(midX, 1.dp.toPx()),
                strokeWidth = 3.dp.toPx()
              )

              // Bottom horizontal bracket lip pointing left
              drawLine(
                color = CyberGold,
                start = Offset(midX - 8.dp.toPx(), h - 1.dp.toPx()),
                end = Offset(midX, h - 1.dp.toPx()),
                strokeWidth = 3.dp.toPx()
              )
            }

            // Top Flag Cap "OUT ]"
            Box(
              modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 1.dp)
                .clip(RoundedCornerShape(topEnd = 4.dp, bottomStart = 4.dp))
                .background(CyberGold)
                .padding(horizontal = 3.dp, vertical = 1.dp)
            ) {
              Text(
                text = "OUT ]",
                color = Color.Black,
                fontSize = 7.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
              )
            }

            // Center tactile grip handle
            Box(
              modifier = Modifier
                .size(width = 10.dp, height = 28.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(CyberGold)
                .border(1.dp, Color.Black.copy(alpha = 0.5f), RoundedCornerShape(3.dp)),
              contentAlignment = Alignment.Center
            ) {
              Column(
                verticalArrangement = Arrangement.spacedBy(2.5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                repeat(3) {
                  Box(
                    modifier = Modifier
                      .size(width = 5.dp, height = 1.5.dp)
                      .background(Color.Black.copy(alpha = 0.8f))
                  )
                }
              }
            }

            // Floating Tooltip on Drag
            if (isDraggingEndHandle) {
              Box(
                modifier = Modifier
                  .align(Alignment.TopCenter)
                  .offset(y = (-22).dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(Color.Black)
                  .border(1.dp, CyberGold, RoundedCornerShape(4.dp))
                  .padding(horizontal = 4.dp, vertical = 2.dp)
              ) {
                Text(
                  text = "OUT: ${outTc.toCompactString()} [F#${outTc.totalFrames}]",
                  color = CyberGold,
                  fontSize = 8.sp,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }

          // 4.5. Electric Cyan / Cyber Gold Playhead Needle
          Canvas(modifier = Modifier.fillMaxHeight().fillMaxWidth()) {
            drawLine(
              color = ElectricCyan,
              start = Offset(playheadX, 0f),
              end = Offset(playheadX, size.height),
              strokeWidth = 2.dp.toPx()
            )
          }

          // Playhead Top Pointer Diamond / Round Cap
          Box(
            modifier = Modifier
              .offset { IntOffset(playheadX.roundToInt() - 6.dp.roundToPx(), 0) }
              .size(12.dp)
              .clip(CircleShape)
              .background(ElectricCyan)
              .border(1.dp, Color.Black, CircleShape)
          )
        }
      }
    }
  }
}

/**
 * High-Precision Ruler Lane with Frame Ticks, SMPTE markings, and In/Out Flag indicators.
 */
@Composable
private fun TimelineRulerLane(
  totalDurationMs: Long,
  timelineZoom: Float,
  fps: Int = 30,
  beatMarkers: List<Long> = emptyList(),
  trimStartMs: Long = 0L,
  trimEndMs: Long = totalDurationMs,
  isTrimmingActive: Boolean = false
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(20.dp)
      .clip(RoundedCornerShape(3.dp))
      .background(ObsidianSurfaceElevated)
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      val totalSec = (totalDurationMs / 1000f)

      val majorStepSec = when {
        timelineZoom >= 7.0f -> 0.5f
        timelineZoom >= 3.5f -> 1.0f
        timelineZoom >= 2.0f -> 2.0f
        else -> 5.0f
      }

      val minorDivisions = when {
        timelineZoom >= 7.0f -> (fps / 2).coerceAtLeast(10)
        timelineZoom >= 3.5f -> 10
        else -> 5
      }

      // Bottom baseline
      drawLine(
        color = ObsidianBorder,
        start = Offset(0f, h - 1f),
        end = Offset(w, h - 1f),
        strokeWidth = 1f
      )

      // Tick markers
      var curSec = 0f
      while (curSec <= totalSec + 0.001f) {
        val x = (curSec / totalSec) * w

        // Major tick
        drawLine(
          color = CyberGold.copy(alpha = 0.85f),
          start = Offset(x, h - 8.dp.toPx()),
          end = Offset(x, h),
          strokeWidth = 1.5.dp.toPx()
        )

        // Minor ticks
        val stepDelta = majorStepSec / minorDivisions
        for (m in 1 until minorDivisions) {
          val subSec = curSec + stepDelta * m
          if (subSec <= totalSec) {
            val subX = (subSec / totalSec) * w
            val tickH = if (m % 5 == 0) 5.dp.toPx() else 3.dp.toPx()
            drawLine(
              color = TextMuted.copy(alpha = 0.45f),
              start = Offset(subX, h - tickH),
              end = Offset(subX, h),
              strokeWidth = 1.dp.toPx()
            )
          }
        }
        curSec += majorStepSec
      }

      // Smart Musical Beat Markers
      for (beatMs in beatMarkers) {
        if (beatMs <= totalDurationMs) {
          val bx = (beatMs.toFloat() / totalDurationMs.toFloat()) * w
          drawCircle(
            color = NeonViolet,
            radius = 2.dp.toPx(),
            center = Offset(bx, 3.dp.toPx())
          )
          drawLine(
            color = NeonViolet.copy(alpha = 0.75f),
            start = Offset(bx, 0f),
            end = Offset(bx, h),
            strokeWidth = 1.dp.toPx()
          )
        }
      }
    }
  }
}

/**
 * Track Lane representing a sequence track with individual clips.
 */
@Composable
private fun TrackLane(
  title: String,
  color: Color,
  clips: List<TimelineClip>,
  totalDurationMs: Long,
  laneWidthDp: Dp
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(23.dp)
      .clip(RoundedCornerShape(4.dp))
      .background(ObsidianSurfaceElevated)
      .padding(horizontal = 2.dp),
    contentAlignment = Alignment.CenterStart
  ) {
    if (clips.isNotEmpty()) {
      clips.forEach { clip ->
        val startFraction = (clip.startMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
        val durFraction = (clip.durationMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0.02f, 1f - startFraction)
        val clipStartOffset = laneWidthDp * startFraction
        val clipWidth = (laneWidthDp * durFraction).coerceAtLeast(16.dp)

        Box(
          modifier = Modifier
            .offset(x = clipStartOffset)
            .width(clipWidth)
            .fillMaxHeight(0.85f)
            .clip(RoundedCornerShape(3.dp))
            .background(color.copy(alpha = 0.85f))
            .border(1.dp, color, RoundedCornerShape(3.dp))
            .padding(horizontal = 4.dp),
          contentAlignment = Alignment.CenterStart
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
          ) {
            Text(
              text = clip.title,
              color = Color.White,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false)
            )
            if (clip.trackType == TrackType.VIDEO && clip.filterPreset != FilterPreset.ORIGINAL) {
              Spacer(modifier = Modifier.width(3.dp))
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(2.dp))
                  .background(Color.Black.copy(alpha = 0.65f))
                  .padding(horizontal = 3.dp, vertical = 1.dp)
              ) {
                Text(
                  text = "🎨 ${clip.filterPreset.labelAr}",
                  color = ElectricCyan,
                  fontSize = 7.5.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1
                )
              }
            }
          }
        }
      }
    } else {
      Text(
        text = title,
        color = TextMuted,
        fontSize = 8.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(start = 6.dp)
      )
    }
  }
}

/**
 * REAL Audio Waveform Track — draws the actual decoded PCM envelope of the
 * project audio. With no audio it honestly renders a flat baseline instead
 * of the previous decorative sine wave.
 */
@Composable
private fun AudioWaveformLane(
  title: String,
  totalDurationMs: Long,
  waveform: FloatArray? = null
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(23.dp)
      .clip(RoundedCornerShape(4.dp))
      .background(ObsidianSurfaceElevated),
    contentAlignment = Alignment.CenterStart
  ) {
    Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
      val w = size.width
      val h = size.height
      val midY = h / 2f
      val barCount = (w / 4.dp.toPx()).toInt().coerceAtLeast(1)
      for (i in 0 until barCount) {
        val x = i * 4.dp.toPx()
        val barH = if (waveform != null && waveform.isNotEmpty()) {
          val v = waveform[(i * waveform.size / barCount).coerceIn(0, waveform.size - 1)]
          (v * (h * 0.75f)).coerceAtLeast(2f)
        } else {
          2f
        }
        drawLine(
          color = AudioTrackColor.copy(alpha = if (waveform != null) 0.8f else 0.35f),
          start = Offset(x, midY - barH / 2f),
          end = Offset(x, midY + barH / 2f),
          strokeWidth = 2.dp.toPx()
        )
      }
    }
    Text(
      text = title,
      color = Color.White.copy(alpha = 0.85f),
      fontSize = 8.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      modifier = Modifier.padding(start = 6.dp)
    )
  }
}
