package com.example.ui.components.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AudioTrackColor
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.SunsetCoral
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Dedicated Custom UI for Audio & VU Meter Mixer Tool.
 */
@Composable
fun AudioMixerPanel(
  volume: Float,
  isMuted: Boolean,
  peakLeft: Float,
  peakRight: Float,
  onVolumeChange: (Float) -> Unit,
  onToggleMute: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(ObsidianSurface)
      .border(1.dp, AudioTrackColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
      .padding(12.dp)
      .testTag("audio_mixer_panel")
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(AudioTrackColor.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.GraphicEq,
            contentDescription = "مكساج الصوت",
            tint = AudioTrackColor,
            modifier = Modifier.size(15.dp)
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = "مكساج الصوت ومؤشرات VU Meter",
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "48 kHz 24-bit Stereo Master Bus",
            color = AudioTrackColor,
            fontSize = 9.sp
          )
        }
      }

      // Mute Toggle Button
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(if (isMuted) SunsetCoral.copy(alpha = 0.2f) else ObsidianSurfaceElevated)
          .border(1.dp, if (isMuted) SunsetCoral else ObsidianBorder, RoundedCornerShape(6.dp))
          .clickable { onToggleMute() }
          .padding(horizontal = 8.dp, vertical = 4.dp)
          .testTag("audio_mute_toggle_button"),
        contentAlignment = Alignment.Center
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = "كتم الصوت",
            tint = if (isMuted) SunsetCoral else TextSecondary,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (isMuted) "صامت" else "مفعل",
            color = if (isMuted) SunsetCoral else TextSecondary,
            fontSize = 10.sp
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Volume Slider
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("مستوى الصوت العام Master Gain", color = TextSecondary, fontSize = 11.sp)
      Text(
        text = if (isMuted) "Muted" else "${(volume * 100).toInt()}%",
        color = if (isMuted) SunsetCoral else AudioTrackColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
      )
    }

    Slider(
      value = if (isMuted) 0f else volume,
      onValueChange = onVolumeChange,
      valueRange = 0f..2f,
      colors = SliderDefaults.colors(
        thumbColor = AudioTrackColor,
        activeTrackColor = AudioTrackColor,
        inactiveTrackColor = ObsidianSurfaceElevated
      ),
      modifier = Modifier.fillMaxWidth().testTag("audio_gain_slider")
    )

    Spacer(modifier = Modifier.height(6.dp))

    // Live Stereo Dynamic VU Meters
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(ObsidianSurfaceElevated)
        .padding(8.dp)
    ) {
      VuBarItem(label = "L", peak = if (isMuted) 0f else peakLeft)
      Spacer(modifier = Modifier.height(6.dp))
      VuBarItem(label = "R", peak = if (isMuted) 0f else peakRight)
    }
  }
}

@Composable
private fun VuBarItem(label: String, peak: Float) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = label,
      color = TextMuted,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      modifier = Modifier.width(16.dp)
    )
    Box(
      modifier = Modifier
        .weight(1f)
        .height(8.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(Color.Black)
    ) {
      Canvas(modifier = Modifier.matchParentSize()) {
        val w = size.width * peak.coerceIn(0f, 1f)
        val barColor = when {
          peak > 0.85f -> SunsetCoral
          peak > 0.65f -> Color(0xFFFBBF24)
          else -> EmeraldGreen
        }
        drawRect(
          color = barColor,
          topLeft = Offset.Zero,
          size = androidx.compose.ui.geometry.Size(w, size.height)
        )
      }
    }
    Spacer(modifier = Modifier.width(8.dp))
    val db = if (peak <= 0.01f) -60 else ((peak - 1f) * 40).toInt()
    Text(
      text = "$db dB",
      color = TextMuted,
      fontSize = 9.sp,
      fontFamily = FontFamily.Monospace,
      modifier = Modifier.width(36.dp)
    )
  }
}
