package com.example.ui.components

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdjustmentsState
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.SunsetCoral
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import kotlin.math.sin

@Composable
fun HistogramVuMeter(
  adjustments: AdjustmentsState,
  peakLeft: Float,
  peakRight: Float,
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
    // 1. RGB Histogram Scopes
    Column(modifier = Modifier.weight(1f)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "RGB SCOPE & HISTOGRAM 2027",
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
      ) {
        val w = size.width
        val h = size.height

        val exposureShift = (adjustments.exposure / 100f) * 0.25f
        val contrastScale = ((adjustments.contrast + 100f) / 100f)

        // Draw RGB curve lines
        fun drawChannelCurve(color: Color, phaseOffset: Float, amp: Float) {
          val path = Path()
          val steps = 30
          for (i in 0..steps) {
            val normX = i.toFloat() / steps
            val x = normX * w
            val sineVal = sin((normX * 3.1415f) + phaseOffset)
            val shiftedX = (normX - 0.5f - exposureShift) * contrastScale + 0.5f
            val curveY = h - ((sineVal.coerceIn(0f, 1f) * amp) * h).coerceIn(2f, h - 2f)
            if (i == 0) path.moveTo(x, curveY) else path.lineTo(x, curveY)
          }
          drawPath(path, color.copy(alpha = 0.65f), style = Stroke(width = 1.5.dp.toPx()))
        }

        drawChannelCurve(Color(0xFFFF3366), 0.2f, 0.75f) // Red
        drawChannelCurve(Color(0xFF00E676), 0.5f, 0.85f) // Green
        drawChannelCurve(Color(0xFF00F0FF), 0.8f, 0.90f) // Blue
      }
    }

    Spacer(modifier = Modifier.width(16.dp))

    // 2. Stereo Audio VU Meter
    Column(horizontalAlignment = Alignment.End) {
      Text(
        text = "AUDIO VU (-dB)",
        color = TextSecondary,
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(4.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        VuMeterBar(channelLabel = "L", level = peakLeft)
        Spacer(modifier = Modifier.width(6.dp))
        VuMeterBar(channelLabel = "R", level = peakRight)
      }
    }
  }
}

@Composable
private fun VuMeterBar(channelLabel: String, level: Float) {
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
  }
}
