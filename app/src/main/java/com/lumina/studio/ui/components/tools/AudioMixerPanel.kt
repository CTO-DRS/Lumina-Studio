package com.lumina.studio.ui.components.tools

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
import com.lumina.studio.data.model.TimelineClip
import com.lumina.studio.engine.AudioGraphEngine
import com.lumina.studio.ui.theme.AudioTrackColor
import com.lumina.studio.ui.theme.ElectricCyan
import com.lumina.studio.ui.theme.EmeraldGreen
import com.lumina.studio.ui.theme.ObsidianBorder
import com.lumina.studio.ui.theme.ObsidianSurface
import com.lumina.studio.ui.theme.ObsidianSurfaceElevated
import com.lumina.studio.ui.theme.SunsetCoral
import com.lumina.studio.ui.theme.TextMuted
import com.lumina.studio.ui.theme.TextPrimary
import com.lumina.studio.ui.theme.TextSecondary

/**
 * Dedicated Custom UI for the Audio Mixer Tool.
 * The master gain and mute switch drive the REAL PreviewAudioController, and
 * per-clip volume/mute edit the actual TimelineClip data used by playback.
 */
@Composable
fun AudioMixerPanel(
  volume: Float,
  isMuted: Boolean,
  peakLeft: Float,
  peakRight: Float,
  audioClips: List<TimelineClip>,
  onVolumeChange: (Float) -> Unit,
  onToggleMute: () -> Unit,
  onClipVolumeChange: (String, Float) -> Unit,
  onClipToggleMute: (String) -> Unit,
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
            text = "Master Bus حقيقي مرتبط بمشغل المعاينة",
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
      valueRange = 0f..1f,
      colors = SliderDefaults.colors(
        thumbColor = AudioTrackColor,
        activeTrackColor = AudioTrackColor,
        inactiveTrackColor = ObsidianSurfaceElevated
      ),
      modifier = Modifier.fillMaxWidth().testTag("audio_gain_slider")
    )

    Spacer(modifier = Modifier.height(6.dp))

    // Real per-clip gain / mute controls
    if (audioClips.isEmpty()) {
      Text(
        text = "لا توجد مقاطع صوتية في التايم لاين — أضف مؤثرات من مختبر الذكاء أو استورد ملف فيديو بصوت",
        color = TextMuted,
        fontSize = 10.sp
      )
    } else {
      Text(
        text = "مقاطع الصوتية (حجم/كتم حقيقي):",
        color = TextSecondary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold
      )
      audioClips.forEach { clip ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(22.dp)
              .clip(CircleShape)
              .background(if (clip.isMuted) SunsetCoral.copy(alpha = 0.25f) else ObsidianSurfaceElevated)
              .border(1.dp, if (clip.isMuted) SunsetCoral else ObsidianBorder, CircleShape)
              .clickable { onClipToggleMute(clip.id) }
              .testTag("clip_mute_${clip.id}"),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (clip.isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
              contentDescription = "كتم المقطع",
              tint = if (clip.isMuted) SunsetCoral else AudioTrackColor,
              modifier = Modifier.size(12.dp)
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = clip.title,
              color = TextPrimary,
              fontSize = 10.sp,
              maxLines = 1,
              overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Slider(
              value = clip.volume,
              onValueChange = { onClipVolumeChange(clip.id, it) },
              valueRange = 0f..1f,
              colors = SliderDefaults.colors(
                thumbColor = AudioTrackColor,
                activeTrackColor = AudioTrackColor.copy(alpha = 0.6f),
                inactiveTrackColor = ObsidianSurfaceElevated
              ),
              modifier = Modifier.height(18.dp).testTag("clip_volume_${clip.id}")
            )
          }
        }
        Spacer(modifier = Modifier.height(4.dp))
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Live Stereo Dynamic VU Meters (REAL amplitude from decoded PCM)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(ObsidianSurfaceElevated)
        .padding(8.dp)
    ) {
      VuBarItem(label = "L", peak = if (isMuted) 0f else peakLeft, isMuted = isMuted)
      Spacer(modifier = Modifier.height(6.dp))
      VuBarItem(label = "R", peak = if (isMuted) 0f else peakRight, isMuted = isMuted)
    }
  }
}

@Composable
private fun VuBarItem(label: String, peak: Float, isMuted: Boolean = false) {
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
    val db = AudioGraphEngine.rmsToDb(if (isMuted) 0f else peak)
    Text(
      text = if (db <= -59.5f) "-∞ dB" else String.format("%.0f dB", db),
      color = TextMuted,
      fontSize = 9.sp,
      fontFamily = FontFamily.Monospace,
      modifier = Modifier.width(44.dp)
    )
  }
}
