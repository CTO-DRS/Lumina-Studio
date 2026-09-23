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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdjustmentsState
import com.example.data.model.CropAspect
import com.example.data.model.FilterPreset
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun BeginnerToolsPanel(
  adjustments: AdjustmentsState,
  onAutoEnhanceChange: (Float) -> Unit,
  onPresetSelect: (FilterPreset) -> Unit,
  onCropSelect: (CropAspect) -> Unit,
  onReset: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(ObsidianSurface)
      .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
      .padding(12.dp)
      .testTag("beginner_tools_panel")
  ) {
    // 1. One-Tap Quick Auto Enhance Slider
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(ElectricCyan.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "تحسين فوري تلقائي",
            tint = ElectricCyan,
            modifier = Modifier.size(14.dp)
          )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "التحسين الفوري التلقائي (Algorithmic DSP)",
          color = TextPrimary,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "${adjustments.autoEnhance.toInt()}%",
          color = ElectricCyan,
          fontSize = 12.sp,
          fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
          imageVector = Icons.Default.RestartAlt,
          contentDescription = "إعادة ضبط",
          tint = TextMuted,
          modifier = Modifier
            .size(18.dp)
            .clickable { onReset() }
            .testTag("beginner_reset_button")
        )
      }
    }

    Slider(
      value = adjustments.autoEnhance,
      onValueChange = onAutoEnhanceChange,
      valueRange = 0f..100f,
      colors = SliderDefaults.colors(
        thumbColor = ElectricCyan,
        activeTrackColor = ElectricCyan,
        inactiveTrackColor = ObsidianSurfaceElevated
      ),
      modifier = Modifier.fillMaxWidth().testTag("auto_enhance_slider")
    )

    Spacer(modifier = Modifier.height(6.dp))

    // 2. Filter Presets Carousel (Teal & Orange, Cyberpunk, Film Noir, etc.)
    Text(
      text = "فلاتر ومؤثرات الألوان السينمائية",
      color = TextSecondary,
      fontSize = 11.sp,
      fontWeight = FontWeight.SemiBold
    )

    Spacer(modifier = Modifier.height(6.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      FilterPreset.values().forEach { preset ->
        val isSelected = adjustments.selectedPreset == preset
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) NeonViolet.copy(alpha = 0.25f) else ObsidianSurfaceElevated)
            .border(
              width = if (isSelected) 1.5.dp else 1.dp,
              color = if (isSelected) NeonViolet else ObsidianBorder,
              shape = RoundedCornerShape(8.dp)
            )
            .clickable { onPresetSelect(preset) }
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag("preset_${preset.name.lowercase()}")
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = preset.labelAr,
              color = if (isSelected) Color.White else TextSecondary,
              fontSize = 11.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Text(
              text = preset.labelEn,
              color = if (isSelected) NeonViolet else TextMuted,
              fontSize = 9.sp
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 3. Social Media Aspect Ratio Crop Buttons (Reels 9:16, YouTube 16:9, Instagram 1:1)
    Text(
      text = "أبعاد المنصات الاجتماعية (تيك توك / انستغرام / يوتيوب)",
      color = TextSecondary,
      fontSize = 11.sp,
      fontWeight = FontWeight.SemiBold
    )

    Spacer(modifier = Modifier.height(6.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      CropAspect.values().forEach { crop ->
        val isSelected = adjustments.selectedCrop == crop
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) ElectricCyan.copy(alpha = 0.2f) else ObsidianSurfaceElevated)
            .border(
              width = if (isSelected) 1.5.dp else 1.dp,
              color = if (isSelected) ElectricCyan else ObsidianBorder,
              shape = RoundedCornerShape(8.dp)
            )
            .clickable { onCropSelect(crop) }
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("crop_${crop.name.lowercase()}")
        ) {
          Text(
            text = crop.label,
            color = if (isSelected) ElectricCyan else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
          )
        }
      }
    }
  }
}
