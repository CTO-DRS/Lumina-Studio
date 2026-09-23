package com.lumina.studio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumina.studio.engine.RealMediaManager
import com.lumina.studio.ui.theme.AudioTrackColor
import com.lumina.studio.ui.theme.CyberGold
import com.lumina.studio.ui.theme.ElectricCyan
import com.lumina.studio.ui.theme.EmeraldGreen
import com.lumina.studio.ui.theme.ObsidianBg
import com.lumina.studio.ui.theme.ObsidianBorder
import com.lumina.studio.ui.theme.ObsidianSurface
import com.lumina.studio.ui.theme.ObsidianSurfaceContainer
import com.lumina.studio.ui.theme.ObsidianSurfaceElevated
import com.lumina.studio.ui.theme.SunsetCoral
import com.lumina.studio.ui.theme.TextMuted
import com.lumina.studio.ui.theme.TextPrimary
import com.lumina.studio.ui.theme.TextSecondary
import com.lumina.studio.ui.theme.VideoTrackColor
import kotlin.math.roundToInt
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Interactive Dual-Handle Precision Video Trimming Slider for Lumina Studio.
 * Allows scrubbing, dragging start/end handles, frame-level micro-nudging,
 * and real-time looped preview within the trimmed zone.
 *
 * The filmstrip background shows REAL video thumbnails decoded from the
 * source file, and the center waveform is the REAL audio envelope when an
 * audio track exists. The old sine-wave simulation was removed.
 */
@Composable
fun VideoTrimSlider(
  trimStartMs: Long,
  trimEndMs: Long,
  totalDurationMs: Long,
  playheadMs: Long,
  isPlaying: Boolean,
  mediaUri: String? = null,
  audioWaveform: FloatArray? = null,
  onTrimChange: (startMs: Long, endMs: Long) -> Unit,
  onSeekTo: (Long) -> Unit,
  onTogglePlay: () -> Unit,
  onNudgeStart: (deltaMs: Long) -> Unit,
  onNudgeEnd: (deltaMs: Long) -> Unit,
  onSetStartToPlayhead: () -> Unit,
  onSetEndToPlayhead: () -> Unit,
  onApplyTrim: () -> Unit,
  onResetTrim: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val safeTotal = totalDurationMs.coerceAtLeast(1000L)
  val safeStart = trimStartMs.coerceIn(0L, safeTotal - 500L)
  val safeEnd = trimEndMs.coerceIn(safeStart + 500L, safeTotal)
  val activeDuration = (safeEnd - safeStart).coerceAtLeast(500L)
  val trimmedOutMs = safeTotal - activeDuration

  // REAL filmstrip thumbnails decoded from the actual source file
  val thumbnailCount = 8
  var thumbnails by remember(mediaUri) { mutableStateOf<List<Bitmap>>(emptyList()) }
  LaunchedEffect(mediaUri, safeTotal) {
    if (mediaUri.isNullOrBlank()) {
      thumbnails = emptyList()
      return@LaunchedEffect
    }
    val loaded = withContext(Dispatchers.IO) {
      val retriever = try {
        MediaMetadataRetriever().apply { setDataSource(context, android.net.Uri.parse(mediaUri)) }
      } catch (e: Exception) {
        null
      } ?: return@withContext emptyList<Bitmap>()
      val result = mutableListOf<Bitmap>()
      try {
        for (i in 0 until thumbnailCount) {
          val posUs = ((i + 0.5) / thumbnailCount * safeTotal * 1000.0).toLong()
          val frame = retriever.getFrameAtTime(posUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
          if (frame != null) {
            // Downscale for memory efficiency
            val targetW = 160
            val scale = targetW.toFloat() / frame.width.coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(
              frame,
              targetW,
              (frame.height * scale).toInt().coerceAtLeast(1),
              true
            )
            result.add(scaled)
          }
        }
      } catch (e: Exception) {
        // leave what we have
      } finally {
        try {
          retriever.release()
        } catch (_: Exception) {}
      }
      result
    }
    thumbnails = loaded
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(ObsidianSurface)
      .border(1.dp, CyberGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
      .padding(10.dp)
      .testTag("video_trim_slider_tool")
  ) {
    // 1. Header with Trim Timecodes & Net Duration
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.ContentCut,
          contentDescription = "أداة القص",
          tint = CyberGold,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "أداة قص الأطراف الدقيقة (Precision Trim)",
          color = TextPrimary,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
      }

      // Active duration badge
      val activeSec = activeDuration.toFloat() / 1000f
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(CyberGold.copy(alpha = 0.15f))
          .border(1.dp, CyberGold.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
          .padding(horizontal = 8.dp, vertical = 2.dp)
      ) {
        Text(
          text = String.format("المدة: %.2f ثانية", activeSec),
          color = CyberGold,
          fontSize = 11.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 2. Main Interactive Dual-Handle Trimming Track
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxWidth()
        .height(68.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(Color.Black)
        .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
        .testTag("trim_filmstrip_container")
    ) {
      val density = LocalDensity.current
      val trackWidthPx = constraints.maxWidth.toFloat()
      val handleWidthPx = with(density) { 28.dp.toPx() }

      // Calculate pixel positions for start, end, and playhead
      val startXPx = ((safeStart.toFloat() / safeTotal.toFloat()) * trackWidthPx)
        .coerceIn(0f, trackWidthPx - handleWidthPx * 1.5f)
      val endXPx = ((safeEnd.toFloat() / safeTotal.toFloat()) * trackWidthPx)
        .coerceIn(startXPx + handleWidthPx * 1.2f, trackWidthPx)
      val playheadXPx = ((playheadMs.toFloat() / safeTotal.toFloat()) * trackWidthPx)
        .coerceIn(0f, trackWidthPx)

      // REAL filmstrip thumbnails + REAL audio waveform
      Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val frameWidth = 44.dp.toPx()
        val numFrames = (w / frameWidth).toInt() + 1

        // REAL thumbnails painted across the strip (cover each frame cell)
        val bitmaps = thumbnails
        if (bitmaps.isNotEmpty()) {
          drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply { isFilterBitmap = true }
            for (i in 0 until numFrames) {
              val fx = i * frameWidth
              val bmp = bitmaps[i % bitmaps.size]
              val aspect = bmp.width.toFloat() / bmp.height.toFloat()
              val cellH = h
              val cellW = cellH * aspect
              canvas.nativeCanvas.drawBitmap(
                bmp,
                null,
                android.graphics.RectF(fx, 0f, fx + cellW.coerceAtLeast(frameWidth), cellH),
                paint
              )
            }
          }
        } else {
          drawRect(color = Color(0xFF141821), topLeft = Offset.Zero, size = size)
        }

        // Sprocket holes & separators (stylistic film frame border)
        for (i in 0 until numFrames) {
          val fx = i * frameWidth
          drawRect(
            color = Color(0xCC0A0A0A),
            topLeft = Offset(fx + 6f, 3f),
            size = Size(10f, 6f)
          )
          drawRect(
            color = Color(0xCC0A0A0A),
            topLeft = Offset(fx + 6f, h - 9f),
            size = Size(10f, 6f)
          )
          drawLine(
            color = Color(0x991F2937),
            start = Offset(fx, 12f),
            end = Offset(fx, h - 12f),
            strokeWidth = 1.dp.toPx()
          )
        }

        // Center REAL audio waveform (or honest flat baseline when silent)
        val waveBarCount = (w / 4.dp.toPx()).toInt().coerceAtLeast(1)
        val midY = h / 2f
        val wave = audioWaveform
        for (i in 0 until waveBarCount) {
          val bx = i * 4.dp.toPx()
          val inActive = bx in startXPx..endXPx
          val barH = if (wave != null && wave.isNotEmpty()) {
            val v = wave[(i * wave.size / waveBarCount).coerceIn(0, wave.size - 1)]
            (v * (h * 0.45f)).coerceAtLeast(2f)
          } else {
            2f // no audio data → honest flat line
          }
          val waveColor = if (inActive) AudioTrackColor.copy(alpha = 0.75f) else Color.DarkGray.copy(alpha = 0.35f)
          drawLine(
            color = waveColor,
            start = Offset(bx, midY - barH / 2f),
            end = Offset(bx, midY + barH / 2f),
            strokeWidth = 2.dp.toPx()
          )
        }

        // Dimmed / Scrim Excluded Zone (Before Start)
        if (startXPx > 0f) {
          drawRect(
            color = Color.Black.copy(alpha = 0.72f),
            topLeft = Offset(0f, 0f),
            size = Size(startXPx, h)
          )
        }

        // Dimmed / Scrim Excluded Zone (After End)
        if (endXPx < w) {
          drawRect(
            color = Color.Black.copy(alpha = 0.72f),
            topLeft = Offset(endXPx, 0f),
            size = Size(w - endXPx, h)
          )
        }

        // Active Segment Highlight Border Bars (Top & Bottom)
        drawRect(
          color = CyberGold,
          topLeft = Offset(startXPx, 0f),
          size = Size(endXPx - startXPx, 3.5.dp.toPx())
        )
        drawRect(
          color = CyberGold,
          topLeft = Offset(startXPx, h - 3.5.dp.toPx()),
          size = Size(endXPx - startXPx, 3.5.dp.toPx())
        )

        // Playhead Vertical Line
        drawLine(
          color = ElectricCyan,
          start = Offset(playheadXPx, 0f),
          end = Offset(playheadXPx, h),
          strokeWidth = 2.dp.toPx()
        )
      }

      // Tap-to-seek in track
      Box(
        modifier = Modifier
          .fillMaxSize()
          .pointerInput(safeTotal) {
            detectTapGestures { offset ->
              val fraction = (offset.x / size.width).coerceIn(0f, 1f)
              onSeekTo((fraction * safeTotal).toLong())
            }
          }
      )

      // LEFT HANDLE (Trim Start / In-Point)
      Box(
        modifier = Modifier
          .offset { IntOffset(startXPx.roundToInt(), 0) }
          .width(28.dp)
          .fillMaxHeight()
          .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
          .background(CyberGold)
          .pointerInput(safeTotal, safeEnd) {
            detectDragGestures { change, dragAmount ->
              change.consume()
              val currentX = (safeStart.toFloat() / safeTotal.toFloat()) * trackWidthPx
              val newX = (currentX + dragAmount.x).coerceIn(0f, endXPx - handleWidthPx)
              val newStartMs = ((newX / trackWidthPx) * safeTotal).toLong().coerceIn(0L, safeEnd - 500L)
              onTrimChange(newStartMs, safeEnd)
            }
          }
          .testTag("trim_handle_start"),
        contentAlignment = Alignment.Center
      ) {
        // Handle grip lines
        Column(
          verticalArrangement = Arrangement.spacedBy(3.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          repeat(3) {
            Box(
              modifier = Modifier
                .width(2.5.dp)
                .height(14.dp)
                .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(1.dp))
            )
          }
        }
      }

      // RIGHT HANDLE (Trim End / Out-Point)
      Box(
        modifier = Modifier
          .offset { IntOffset((endXPx - handleWidthPx).roundToInt(), 0) }
          .width(28.dp)
          .fillMaxHeight()
          .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
          .background(CyberGold)
          .pointerInput(safeTotal, safeStart) {
            detectDragGestures { change, dragAmount ->
              change.consume()
              val currentX = (safeEnd.toFloat() / safeTotal.toFloat()) * trackWidthPx
              val newX = (currentX + dragAmount.x).coerceIn(startXPx + handleWidthPx, trackWidthPx)
              val newEndMs = ((newX / trackWidthPx) * safeTotal).toLong().coerceIn(safeStart + 500L, safeTotal)
              onTrimChange(safeStart, newEndMs)
            }
          }
          .testTag("trim_handle_end"),
        contentAlignment = Alignment.Center
      ) {
        // Handle grip lines
        Column(
          verticalArrangement = Arrangement.spacedBy(3.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          repeat(3) {
            Box(
              modifier = Modifier
                .width(2.5.dp)
                .height(14.dp)
                .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(1.dp))
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 3. Timecodes Display Row (IN, OUT, TRIMMED OUT)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(ObsidianSurfaceElevated)
        .padding(horizontal = 10.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // IN Timecode
      val inSec = (safeStart / 1000L) % 60L
      val inMin = safeStart / 60000L
      val inMillis = (safeStart % 1000L) / 10L
      Column {
        Text("نقطة البداية IN", color = TextSecondary, fontSize = 9.sp)
        Text(
          text = String.format("%02d:%02d.%02d", inMin, inSec, inMillis),
          color = CyberGold,
          fontSize = 12.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold
        )
      }

      // OUT Timecode
      val outSec = (safeEnd / 1000L) % 60L
      val outMin = safeEnd / 60000L
      val outMillis = (safeEnd % 1000L) / 10L
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("نقطة النهاية OUT", color = TextSecondary, fontSize = 9.sp)
        Text(
          text = String.format("%02d:%02d.%02d", outMin, outSec, outMillis),
          color = CyberGold,
          fontSize = 12.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold
        )
      }

      // Cut Footage
      val cutSec = trimmedOutMs.toFloat() / 1000f
      Column(horizontalAlignment = Alignment.End) {
        Text("المقتطع المحذوف", color = TextSecondary, fontSize = 9.sp)
        Text(
          text = String.format("-%.2f ثانية", cutSec),
          color = SunsetCoral,
          fontSize = 12.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 4. Micro-Nudge & Quick-Set Precision Controls
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Nudge IN -0.1s
      Box(
        modifier = Modifier
          .weight(1f)
          .height(34.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(ObsidianSurfaceElevated)
          .border(1.dp, ObsidianBorder, RoundedCornerShape(6.dp))
          .clickable { onNudgeStart(-100L) }
          .testTag("nudge_in_backward"),
        contentAlignment = Alignment.Center
      ) {
        Text("IN -0.1s", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
      }

      // Nudge IN +0.1s
      Box(
        modifier = Modifier
          .weight(1f)
          .height(34.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(ObsidianSurfaceElevated)
          .border(1.dp, ObsidianBorder, RoundedCornerShape(6.dp))
          .clickable { onNudgeStart(100L) }
          .testTag("nudge_in_forward"),
        contentAlignment = Alignment.Center
      ) {
        Text("IN +0.1s", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
      }

      // Set IN at Playhead
      Box(
        modifier = Modifier
          .weight(1.3f)
          .height(34.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(ElectricCyan.copy(alpha = 0.15f))
          .border(1.dp, ElectricCyan, RoundedCornerShape(6.dp))
          .clickable { onSetStartToPlayhead() }
          .testTag("set_in_at_playhead"),
        contentAlignment = Alignment.Center
      ) {
        Text("IN عند المؤشر", color = ElectricCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
      }

      // Set OUT at Playhead
      Box(
        modifier = Modifier
          .weight(1.3f)
          .height(34.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(ElectricCyan.copy(alpha = 0.15f))
          .border(1.dp, ElectricCyan, RoundedCornerShape(6.dp))
          .clickable { onSetEndToPlayhead() }
          .testTag("set_out_at_playhead"),
        contentAlignment = Alignment.Center
      ) {
        Text("OUT عند المؤشر", color = ElectricCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
      }

      // Nudge OUT -0.1s
      Box(
        modifier = Modifier
          .weight(1f)
          .height(34.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(ObsidianSurfaceElevated)
          .border(1.dp, ObsidianBorder, RoundedCornerShape(6.dp))
          .clickable { onNudgeEnd(-100L) }
          .testTag("nudge_out_backward"),
        contentAlignment = Alignment.Center
      ) {
        Text("OUT -0.1s", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
      }

      // Nudge OUT +0.1s
      Box(
        modifier = Modifier
          .weight(1f)
          .height(34.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(ObsidianSurfaceElevated)
          .border(1.dp, ObsidianBorder, RoundedCornerShape(6.dp))
          .clickable { onNudgeEnd(100L) }
          .testTag("nudge_out_forward"),
        contentAlignment = Alignment.Center
      ) {
        Text("OUT +0.1s", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 5. Workflow Execution Buttons (Play Trimmed Range, Apply Trim, Reset)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Play / Pause Trimmed Selection Loop
      Button(
        onClick = onTogglePlay,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceElevated, contentColor = TextPrimary),
        modifier = Modifier
          .weight(1f)
          .height(40.dp)
          .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
          .testTag("play_trim_selection_button")
      ) {
        Icon(
          imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
          contentDescription = if (isPlaying) "إيقاف مؤقت للمعاينة" else "تشغيل المقطع المقصوص",
          tint = ElectricCyan,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(if (isPlaying) "إيقاف" else "معاينة القص", fontSize = 11.sp, fontWeight = FontWeight.Bold)
      }

      // Reset Trim
      Button(
        onClick = onResetTrim,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceElevated, contentColor = TextSecondary),
        modifier = Modifier
          .weight(0.9f)
          .height(40.dp)
          .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
          .testTag("reset_trim_button")
      ) {
        Icon(
          imageVector = Icons.Default.RestartAlt,
          contentDescription = "استعادة كامل المقطع",
          tint = TextSecondary,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text("استعادة", fontSize = 11.sp)
      }

      // Apply Trim to Clip
      Button(
        onClick = onApplyTrim,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CyberGold, contentColor = Color.Black),
        modifier = Modifier
          .weight(1.3f)
          .height(40.dp)
          .testTag("apply_trim_button")
      ) {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = "تأكيد وقص المقطع",
          tint = Color.Black,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("تأكيد القص", fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}
