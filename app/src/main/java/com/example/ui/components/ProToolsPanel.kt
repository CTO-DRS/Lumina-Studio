package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdjustmentsState
import com.example.ui.theme.AudioTrackColor
import com.example.ui.theme.CyberGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.SunsetCoral
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ProToolsPanel(
  adjustments: AdjustmentsState,
  onAdjustmentChange: ((AdjustmentsState) -> AdjustmentsState) -> Unit,
  onReset: () -> Unit,
  modifier: Modifier = Modifier,
  playbackSpeed: Float = 1.0f,
  onSpeedChange: (Float) -> Unit = {},
  isLetterboxActive: Boolean = false,
  onToggleLetterbox: () -> Unit = {}
) {
  var selectedTab by remember { mutableIntStateOf(0) } // 0: Color, 1: FX, 2: Audio, 3: Speed

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(ObsidianSurface)
      .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
      .padding(10.dp)
      .testTag("pro_tools_panel")
  ) {
    // Top Tabs: Color Grading, Visual FX, Audio Mixer, Speed
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      ProTabItem(
        label = "تدريج الألوان",
        icon = Icons.Default.Palette,
        isSelected = selectedTab == 0,
        onClick = { selectedTab = 0 }
      )
      ProTabItem(
        label = "مؤثرات بصرية FX",
        icon = Icons.Default.AutoFixHigh,
        isSelected = selectedTab == 1,
        onClick = { selectedTab = 1 }
      )
      ProTabItem(
        label = "مكساج الصوت",
        icon = Icons.Default.GraphicEq,
        isSelected = selectedTab == 2,
        onClick = { selectedTab = 2 }
      )
      ProTabItem(
        label = "السرعة والشاشة",
        icon = Icons.Default.Speed,
        isSelected = selectedTab == 3,
        onClick = { selectedTab = 3 }
      )

      Spacer(modifier = Modifier.weight(1f))

      Icon(
        imageVector = Icons.Default.RestartAlt,
        contentDescription = "إعادة ضبط المعاملات",
        tint = TextMuted,
        modifier = Modifier
          .size(22.dp)
          .clickable { onReset() }
          .padding(2.dp)
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    when (selectedTab) {
      0 -> ColorGradingControls(adjustments, onAdjustmentChange)
      1 -> VisualFxControls(adjustments, onAdjustmentChange)
      2 -> AudioMixerControls()
      3 -> SpeedMotionControls(
        playbackSpeed = playbackSpeed,
        onSpeedChange = onSpeedChange,
        isLetterboxActive = isLetterboxActive,
        onToggleLetterbox = onToggleLetterbox
      )
    }
  }
}

@Composable
private fun ProTabItem(
  label: String,
  icon: ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .background(if (isSelected) NeonViolet.copy(alpha = 0.25f) else ObsidianSurfaceElevated)
      .border(
        width = if (isSelected) 1.5.dp else 1.dp,
        color = if (isSelected) NeonViolet else ObsidianBorder,
        shape = RoundedCornerShape(8.dp)
      )
      .clickable { onClick() }
      .padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (isSelected) NeonViolet else TextSecondary,
        modifier = Modifier.size(15.dp)
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = label,
        color = if (isSelected) Color.White else TextSecondary,
        fontSize = 11.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
      )
    }
  }
}

@Composable
private fun ColorGradingControls(
  adjustments: AdjustmentsState,
  onAdjustmentChange: ((AdjustmentsState) -> AdjustmentsState) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    ProSliderRow(
      labelAr = "التعريض (Exposure)",
      value = adjustments.exposure,
      range = -100f..100f,
      color = ElectricCyan,
      onValueChange = { v -> onAdjustmentChange { it.copy(exposure = v) } }
    )
    ProSliderRow(
      labelAr = "التباين (Contrast)",
      value = adjustments.contrast,
      range = -100f..100f,
      color = ElectricCyan,
      onValueChange = { v -> onAdjustmentChange { it.copy(contrast = v) } }
    )
    ProSliderRow(
      labelAr = "حرارة الألوان (Warmth)",
      value = adjustments.temperature,
      range = -100f..100f,
      color = CyberGold,
      onValueChange = { v -> onAdjustmentChange { it.copy(temperature = v) } }
    )
    ProSliderRow(
      labelAr = "التشبع اللوني (Saturation)",
      value = adjustments.saturation,
      range = -100f..100f,
      color = SunsetCoral,
      onValueChange = { v -> onAdjustmentChange { it.copy(saturation = v) } }
    )
    ProSliderRow(
      labelAr = "الصبغة (Tint)",
      value = adjustments.tint,
      range = -100f..100f,
      color = NeonViolet,
      onValueChange = { v -> onAdjustmentChange { it.copy(tint = v) } }
    )
  }
}

@Composable
private fun VisualFxControls(
  adjustments: AdjustmentsState,
  onAdjustmentChange: ((AdjustmentsState) -> AdjustmentsState) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    ProSliderRow(
      labelAr = "تعتيم الحواف (Vignette)",
      value = adjustments.vignette,
      range = 0f..100f,
      color = ElectricCyan,
      onValueChange = { v -> onAdjustmentChange { it.copy(vignette = v) } }
    )
    ProSliderRow(
      labelAr = "حبيبات الفيلم 35mm (Film Grain)",
      value = adjustments.filmGrain,
      range = 0f..100f,
      color = CyberGold,
      onValueChange = { v -> onAdjustmentChange { it.copy(filmGrain = v) } }
    )
    ProSliderRow(
      labelAr = "تشوه لوني (RGB Glitch)",
      value = adjustments.glitchRgb,
      range = 0f..100f,
      color = SunsetCoral,
      onValueChange = { v -> onAdjustmentChange { it.copy(glitchRgb = v) } }
    )
    ProSliderRow(
      labelAr = "توهج ضوئي (Bloom / Glow)",
      value = adjustments.bloomGlow,
      range = 0f..100f,
      color = NeonViolet,
      onValueChange = { v -> onAdjustmentChange { it.copy(bloomGlow = v) } }
    )
  }
}

@Composable
private fun AudioMixerControls() {
  var volume by remember { mutableStateOf(85f) }
  var isMuted by remember { mutableStateOf(false) }
  var selectedEq by remember { mutableIntStateOf(0) }

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
          contentDescription = "الصوت",
          tint = if (isMuted) SunsetCoral else AudioTrackColor,
          modifier = Modifier.size(18.dp).clickable { isMuted = !isMuted }
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "مستوى الصوت العام (Master Gain)", color = TextPrimary, fontSize = 12.sp)
      }
      Text(
        text = if (isMuted) "صامت" else "${volume.toInt()}%",
        color = AudioTrackColor,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold
      )
    }

    Slider(
      value = if (isMuted) 0f else volume,
      onValueChange = { volume = it; isMuted = false },
      valueRange = 0f..100f,
      colors = SliderDefaults.colors(
        thumbColor = AudioTrackColor,
        activeTrackColor = AudioTrackColor,
        inactiveTrackColor = ObsidianSurfaceElevated
      )
    )

    // Equalizer Profiles
    Text(text = "معادل الصوت السينمائي (EQ Preset)", color = TextSecondary, fontSize = 11.sp)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      val presets = listOf("سينمائي عميق Bass", "وضوح الصوت Vocal", "متوازن Flat")
      presets.forEachIndexed { index, name ->
        val isSel = selectedEq == index
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSel) AudioTrackColor.copy(alpha = 0.2f) else ObsidianSurfaceElevated)
            .border(1.dp, if (isSel) AudioTrackColor else ObsidianBorder, RoundedCornerShape(6.dp))
            .clickable { selectedEq = index }
            .padding(vertical = 6.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = name,
            color = if (isSel) AudioTrackColor else TextSecondary,
            fontSize = 10.sp,
            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
          )
        }
      }
    }
  }
}

@Composable
private fun SpeedMotionControls(
  playbackSpeed: Float,
  onSpeedChange: (Float) -> Unit,
  isLetterboxActive: Boolean,
  onToggleLetterbox: () -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text(text = "التحكم بسرعة الإطارات والحركة البطيئة (Time Remapping)", color = TextSecondary, fontSize = 11.sp)

    val speedOptions = listOf(
      0.25f to "0.25x",
      0.5f to "0.5x",
      1.0f to "1.0x",
      1.5f to "1.5x",
      2.0f to "2.0x",
      4.0f to "4.0x"
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      speedOptions.forEach { (speedVal, label) ->
        val isSel = kotlin.math.abs(playbackSpeed - speedVal) < 0.05f
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSel) ElectricCyan.copy(alpha = 0.2f) else ObsidianSurfaceElevated)
            .border(1.dp, if (isSel) ElectricCyan else ObsidianBorder, RoundedCornerShape(6.dp))
            .clickable { onSpeedChange(speedVal) }
            .padding(vertical = 8.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = label,
            color = if (isSel) ElectricCyan else TextSecondary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
          )
        }
      }
    }

    // Aspect Ratio & Letterbox Control Row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(ObsidianSurfaceElevated)
        .clickable { onToggleLetterbox() }
        .padding(horizontal = 12.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "شريط سينمائي عريض 2.39:1 CinemaScope",
          color = TextPrimary,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = if (isLetterboxActive) "مفعّل - قناع أبعاد الأفلام السينمائية العالمية" else "معطّل - أبعاد العرض القياسية الكاملة",
          color = if (isLetterboxActive) CyberGold else TextMuted,
          fontSize = 10.sp
        )
      }

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(if (isLetterboxActive) CyberGold else ObsidianBorder)
          .padding(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Text(
          text = if (isLetterboxActive) "مفعّل" else "تشغيل",
          color = if (isLetterboxActive) Color.Black else TextSecondary,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

@Composable
private fun ProSliderRow(
  labelAr: String,
  value: Float,
  range: ClosedFloatingPointRange<Float>,
  color: Color,
  onValueChange: (Float) -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = labelAr,
      color = TextPrimary,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium,
      modifier = Modifier.width(150.dp)
    )

    Slider(
      value = value,
      onValueChange = onValueChange,
      valueRange = range,
      colors = SliderDefaults.colors(
        thumbColor = color,
        activeTrackColor = color,
        inactiveTrackColor = ObsidianSurfaceElevated
      ),
      modifier = Modifier
        .weight(1f)
        .height(28.dp)
    )

    Spacer(modifier = Modifier.width(8.dp))

    Text(
      text = String.format("%+d", value.toInt()),
      color = color,
      fontSize = 11.sp,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.width(36.dp)
    )
  }
}
