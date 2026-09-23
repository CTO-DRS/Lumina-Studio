package com.lumina.studio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumina.studio.engine.audio.AudioGraphEngine
import com.lumina.studio.engine.analysis.MediaAnalysisEngine
import com.lumina.studio.ui.theme.EmeraldGreen
import com.lumina.studio.ui.theme.ObsidianBorder
import com.lumina.studio.ui.theme.ObsidianSurface
import com.lumina.studio.ui.theme.ObsidianSurfaceElevated
import com.lumina.studio.ui.theme.SunsetCoral
import com.lumina.studio.ui.theme.TextMuted
import com.lumina.studio.ui.theme.TextSecondary

/**
 * Scopes panel driven by REAL measured data:
 *
 *  - The RGB histogram is computed from the actual frame pixels
 *    (MediaAnalysisEngine.computeHistogram) — the previous three sine curves
 *    were pure decoration.
 *  - The VU bars are filled from the real decoded PCM envelope amplitude.
 */
@Composable
fun HistogramVuMeter(
  histogramData: MediaAnalysisEngine.HistogramData?,
  peakLeft: Float,
  peakRight: Float,
  peakLeftDb: String,
  peakRightDb: String,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .background(ObsidianSurface)
      .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
      .padding(horizontal = 10.dp, vertical = 6.dp)
      .testTag("histogram_vumeter_panel"),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // 1. REAL RGB histogram
    Column(modifier = Modifier.weight(1f)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "RGB HISTOGRAM (REAL PIXELS)",
          color = TextSecondary,
          fontSize = 9.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold
        )
        Row {
          Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFFF3366)))
          Spacer(modifier = Modifier.width(3.dp))
          Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF00E676)))
          Spacer(modifier = Modifier.width(3.dp))
          Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF00F0FF)))
        }
      }

      Spacer(modifier = Modifier.height(3.dp))

      Canvas(
        modifier = Modifier
          .fillMaxWidth()
          .height(32.dp)
          .clip(RoundedCornerShape(4.dp))
          .background(ObsidianSurfaceElevated)
          .testTag("real_histogram_canvas")
      ) {
        val w = size.width
        val h = size.height
        val data = histogramData

        if (data == null || data.pixelCount == 0) {
          // Honest empty state until analysis completes
          drawLine(
            color = TextMuted.copy(alpha = 0.5f),
            start = Offset(0f, h / 2f),
            end = Offset(w, h / 2f),
            strokeWidth = 1f
          )
        } else {
          val bins = data.binCount
          val binWidth = w / bins
          val maxCount = maxOf(
            data.red.max(),
            data.green.max(),
            data.blue.max()
          ).coerceAtLeast(1)

          fun drawChannel(values: IntArray, color: Color) {
            for (i in 0 until bins) {
              val norm = values[i].toFloat() / maxCount
              if (norm <= 0f) continue
              val barH = (norm * (h - 2f)).coerceAtLeast(1f)
              drawRoundRect(
                color = color.copy(alpha = 0.55f),
                topLeft = Offset(i * binWidth, h - barH),
                size = Size(binWidth.coerceAtLeast(1f), barH),
                cornerRadius = CornerRadius(1f, 1f)
              )
            }
          }

          drawChannel(data.red, Color(0xFFFF3366))
          drawChannel(data.green, Color(0xFF00E676))
          drawChannel(data.blue, Color(0xFF00F0FF))
        }
      }
    }

    Spacer(modifier = Modifier.width(16.dp))

    // 2. REAL audio VU meter (amplitude from decoded PCM)
    Column(horizontalAlignment = Alignment.End) {
      Text(
        text = "AUDIO VU (dBFS)",
        color = TextSecondary,
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(4.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        VuMeterBar(channelLabel = "L", level = peakLeft, dbLabel = peakLeftDb)
        Spacer(modifier = Modifier.width(6.dp))
        VuMeterBar(channelLabel = "R", level = peakRight, dbLabel = peakRightDb)
      }
    }
  }
}

@Composable
private fun VuMeterBar(channelLabel: String, level: Float, dbLabel: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text(
      text = channelLabel,
      color = TextMuted,
      fontSize = 9.sp,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.width(3.dp))
    Box(
      modifier = Modifier
        .width(50.dp)
        .height(8.dp)
        .clip(RoundedCornerShape(2.dp))
        .background(ObsidianSurfaceElevated)
        .testTag("vu_bar_$channelLabel")
    ) {
      val fillPercent = level.coerceIn(0f, 1f)
      Box(
        modifier = Modifier
          .fillMaxHeight()
          .fillMaxWidth(fillPercent)
          .background(
            Brush.horizontalGradient(
              colors = listOf(
                EmeraldGreen,
                Color(0xFFFFD166),
                SunsetCoral
              )
            )
          )
      )
    }
    Spacer(modifier = Modifier.width(3.dp))
    Text(
      text = dbLabel,
      color = TextMuted,
      fontSize = 7.sp,
      fontFamily = FontFamily.Monospace
    )
  }
}
